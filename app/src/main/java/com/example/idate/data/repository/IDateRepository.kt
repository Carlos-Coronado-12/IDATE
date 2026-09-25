package com.example.idate.data.repository

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.idate.R
import com.example.idate.data.local.IDateDatabase
import com.example.idate.data.local.entity.*
import com.example.idate.data.remote.FriendsRemoteManager
import com.example.idate.model.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class IDateRepository(context: Context) {
    private val database = IDateDatabase.getDatabase(context)
    private val planDao = database.planDao()
    private val likedPlanDao = database.likedPlanDao()
    private val friendDao = database.friendDao()
    private val dateDeckDao = database.dateDeckDao()

    private val realtimeDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance().apply {
                try { setPersistenceEnabled(true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            FirebaseDatabase.getInstance("https://idate-8948b-default-rtdb.firebaseio.com")
        }
    }

    suspend fun initializeDefaultDataIfNeeded() {
        try {
            // Delete legacy starter deck if it exists
            dateDeckDao.deleteDeckById("starter_deck_main")

            if (planDao.getAllPlansList().isEmpty()) {
                val entities = SamplePlans.defaultPlans.map { it.toEntity() }
                planDao.insertPlans(entities)
            }
        } catch (e: Exception) {
            android.util.Log.e("IDATE_REPO", "Error inicializando datos por defecto: ${e.message}")
        }
    }

    suspend fun getOrCreateUserProfile(): UserProfile {
        val existing = friendDao.getUserProfileOnce()
        if (existing != null) {
            return UserProfile(
                userId = existing.userId,
                name = existing.name,
                friendCode = existing.friendCode,
                avatarEmoji = existing.avatarEmoji,
                bio = existing.bio
            )
        }

        val newUserId = UUID.randomUUID().toString().substring(0, 8)
        val newCode = FriendsRemoteManager.generateUniqueCode("ID")
        val defaultProfile = UserProfile(
            userId = newUserId,
            name = "Usuario IDATE",
            friendCode = newCode,
            avatarEmoji = "😎",
            bio = "¡Listo para salir y hacer los mejores planes!"
        )

        friendDao.insertOrUpdateProfile(
            UserProfileEntity(
                userId = defaultProfile.userId,
                name = defaultProfile.name,
                friendCode = defaultProfile.friendCode,
                avatarEmoji = defaultProfile.avatarEmoji,
                bio = defaultProfile.bio
            )
        )
        FriendsRemoteManager.syncUserProfileToCloud(defaultProfile)
        return defaultProfile
    }

    fun getUserProfileFlow(): Flow<UserProfile?> {
        return friendDao.getUserProfile().map { entity ->
            entity?.let {
                UserProfile(
                    userId = it.userId,
                    name = it.name,
                    friendCode = it.friendCode,
                    avatarEmoji = it.avatarEmoji,
                    bio = it.bio
                )
            }
        }
    }

    suspend fun updateUserProfile(name: String, avatarEmoji: String, bio: String) {
        val current = getOrCreateUserProfile()
        val updated = current.copy(name = name, avatarEmoji = avatarEmoji, bio = bio)
        friendDao.insertOrUpdateProfile(
            UserProfileEntity(
                userId = updated.userId,
                name = updated.name,
                friendCode = updated.friendCode,
                avatarEmoji = updated.avatarEmoji,
                bio = updated.bio
            )
        )
        FriendsRemoteManager.syncUserProfileToCloud(updated)
    }

    // Friends Management
    fun getAllFriends(): Flow<List<Friend>> {
        return friendDao.getAllFriends().map { list ->
            list.map {
                Friend(
                    id = it.id,
                    friendCode = it.friendCode,
                    name = it.name,
                    avatarEmoji = it.avatarEmoji,
                    status = try { FriendStatus.valueOf(it.status) } catch (_: Exception) { FriendStatus.ACCEPTED },
                    mutualMatchesCount = it.mutualMatchesCount,
                    isOnline = it.isOnline,
                    createdAt = it.createdAt
                )
            }
        }
    }

    suspend fun addFriend(friend: Friend) {
        friendDao.insertFriend(
            FriendEntity(
                id = friend.id,
                friendCode = friend.friendCode,
                name = friend.name,
                avatarEmoji = friend.avatarEmoji,
                status = friend.status.name,
                mutualMatchesCount = friend.mutualMatchesCount,
                isOnline = friend.isOnline,
                createdAt = friend.createdAt
            )
        )
    }

    suspend fun syncFriendsFromRemote(remoteFriends: List<Friend>) {
        val entities = remoteFriends.map { friend ->
            FriendEntity(
                id = friend.id,
                friendCode = friend.friendCode,
                name = friend.name,
                avatarEmoji = friend.avatarEmoji,
                status = friend.status.name,
                mutualMatchesCount = friend.mutualMatchesCount,
                isOnline = friend.isOnline,
                createdAt = friend.createdAt
            )
        }
        friendDao.insertFriends(entities)
    }

    suspend fun removeFriend(friendId: String) {
        friendDao.deleteFriend(friendId)
    }

    suspend fun incrementFriendMatches(friendId: String) {
        friendDao.incrementFriendMatches(friendId)
    }

    suspend fun incrementGroupMatches(groupId: String) {
        friendDao.incrementGroupMatches(groupId)
    }

    // Groups Management
    fun getAllGroups(): Flow<List<FriendGroup>> {
        return friendDao.getAllGroups().map { list ->
            list.map {
                FriendGroup(
                    id = it.id,
                    groupCode = it.groupCode,
                    name = it.name,
                    description = it.description,
                    iconEmoji = it.iconEmoji,
                    colorHex = it.colorHex,
                    memberCount = it.memberCount,
                    memberNames = it.memberNames,
                    matchedPlansCount = it.matchedPlansCount,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt
                )
            }
        }
    }

    suspend fun addGroup(group: FriendGroup) {
        friendDao.insertGroup(
            GroupEntity(
                id = group.id,
                groupCode = group.groupCode,
                name = group.name,
                description = group.description,
                iconEmoji = group.iconEmoji,
                colorHex = group.colorHex,
                memberCount = group.memberCount,
                memberNames = group.memberNames,
                matchedPlansCount = group.matchedPlansCount,
                createdBy = group.createdBy,
                createdAt = group.createdAt
            )
        )
    }

    suspend fun syncGroupsFromRemote(remoteGroups: List<FriendGroup>) {
        val entities = remoteGroups.map { group ->
            GroupEntity(
                id = group.id,
                groupCode = group.groupCode,
                name = group.name,
                description = group.description,
                iconEmoji = group.iconEmoji,
                colorHex = group.colorHex,
                memberCount = group.memberCount,
                memberNames = group.memberNames,
                matchedPlansCount = group.matchedPlansCount,
                createdBy = group.createdBy,
                createdAt = group.createdAt
            )
        }
        friendDao.insertGroups(entities)
    }

    suspend fun removeGroup(groupId: String) {
        friendDao.deleteGroup(groupId)
    }

    suspend fun resetAllUserMatches(myUserId: String, friendIds: List<String>, groupCodes: List<String>) {
        friendDao.resetAllFriendMatches()
        friendDao.resetAllGroupMatches()
        friendDao.clearAllTargetLikes()
        FriendsRemoteManager.resetAllCloudMatches(myUserId, friendIds, groupCodes)
    }

    // ==========================================
    // Date Decks Management
    // ==========================================
    fun getAllDecks(): Flow<List<DateDeck>> {
        return dateDeckDao.getAllDecks().map { entities ->
            entities.map {
                DateDeck(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    iconEmoji = it.iconEmoji,
                    colorHex = it.colorHex,
                    planIds = it.planIds,
                    createdBy = it.createdBy,
                    isImported = it.isImported,
                    targetGroupId = it.targetGroupId,
                    targetFriendId = it.targetFriendId,
                    createdAt = it.createdAt
                )
            }
        }
    }

    suspend fun createOrUpdateDeck(deck: DateDeck) {
        dateDeckDao.insertDeck(
            DateDeckEntity(
                id = deck.id,
                name = deck.name,
                description = deck.description,
                iconEmoji = deck.iconEmoji,
                colorHex = deck.colorHex,
                planIds = deck.planIds,
                createdBy = deck.createdBy,
                isImported = deck.isImported,
                targetGroupId = deck.targetGroupId,
                targetFriendId = deck.targetFriendId,
                createdAt = deck.createdAt
            )
        )
        // Sync to cloud if needed
        try {
            val deckMap = mapOf(
                "id" to deck.id,
                "name" to deck.name,
                "description" to deck.description,
                "iconEmoji" to deck.iconEmoji,
                "colorHex" to deck.colorHex,
                "planIds" to deck.planIds,
                "createdBy" to deck.createdBy,
                "createdAt" to deck.createdAt
            )
            realtimeDb.getReference("decks").child(deck.id).setValue(deckMap)
        } catch (_: Exception) {}
    }

    suspend fun deleteDeck(deckId: String) {
        dateDeckDao.deleteDeckById(deckId)
        try {
            realtimeDb.getReference("decks").child(deckId).removeValue()
        } catch (_: Exception) {}
    }

    suspend fun removeDefaultDecks() {
        dateDeckDao.deleteDeckById("deck_romantic")
        dateDeckDao.deleteDeckById("deck_adventures")
        dateDeckDao.deleteDeckById("deck_casual")
        dateDeckDao.deleteDeckById("deck_chill_food")
        dateDeckDao.deleteDeckById("deck_tasty_food")
        try {
            realtimeDb.getReference("decks").child("deck_romantic").removeValue()
            realtimeDb.getReference("decks").child("deck_adventures").removeValue()
            realtimeDb.getReference("decks").child("deck_casual").removeValue()
        } catch (_: Exception) {}
    }

    // Guardar únicamente planes (usado para barajas grupales sin contaminar inventario de barajas)
    suspend fun savePlansOnly(plans: List<Plan>) {
        try {
            if (plans.isNotEmpty()) {
                val entities = plans.map { it.toEntity() }
                planDao.insertPlans(entities)
            }
        } catch (e: Exception) {
            android.util.Log.e("IDATE_REPO", "Error guardando planes: ${e.message}")
        }
    }

    // Importar una Baraja recibida de un amigo junto con todos sus planes
    suspend fun importDeckWithPlans(deck: DateDeck, plans: List<Plan>) {
        try {
            if (plans.isNotEmpty()) {
                val entities = plans.map { it.toEntity() }
                planDao.insertPlans(entities)
            }
            createOrUpdateDeck(deck.copy(isImported = true))
        } catch (e: Exception) {
            android.util.Log.e("IDATE_REPO", "Error importando baraja con planes: ${e.message}")
        }
    }

    suspend fun getPlansForIds(planIds: List<Int>): List<Plan> {
        val all = planDao.getAllPlansList().associateBy { it.id }
        return planIds.mapNotNull { all[it]?.toDomain() }
    }

    suspend fun deleteAllPlans() {
        planDao.deleteAllPlans()
        likedPlanDao.clearAllLikedPlans()
    }

    suspend fun deletePlan(planId: Int) {
        planDao.deletePlanById(planId)
        likedPlanDao.deleteLikedPlanByPlanId(planId)
    }

    suspend fun restoreDefaultPlans() {
        val entities = SamplePlans.defaultPlans.map { it.toEntity() }
        planDao.insertPlans(entities)
        val starterDeck = DateDeckEntity(
            id = "starter_deck_main",
            name = "Mis Citas Favoritas",
            description = "Baraja inicial con los mejores planes de citas recomendados.",
            iconEmoji = "🎴",
            colorHex = 0xFF2E7D32,
            planIds = (1..12).toList(),
            createdBy = "IDATE",
            isImported = false,
            createdAt = System.currentTimeMillis()
        )
        dateDeckDao.insertDeck(starterDeck)
    }

    fun getAllPlans(): Flow<List<Plan>> {
        return planDao.getAllPlans().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getLikedPlans(): Flow<List<Plan>> {
        return likedPlanDao.getAllLikedPlans().map { likedEntities ->
            val allPlans = planDao.getAllPlansList().associateBy { it.id }
            likedEntities.mapNotNull { liked ->
                allPlans[liked.planId]?.toDomain()
            }
        }
    }

    suspend fun isPlanLiked(planId: Int): Boolean {
        return likedPlanDao.isPlanLiked(planId)
    }

    fun isPlanLikedFlow(planId: Int): Flow<Boolean> {
        return likedPlanDao.isPlanLikedFlow(planId)
    }

    suspend fun toggleLikePlan(plan: Plan, isLiked: Boolean) {
        if (isLiked) {
            likedPlanDao.insertLikedPlan(LikedPlanEntity(planId = plan.id))
        } else {
            likedPlanDao.deleteLikedPlanByPlanId(plan.id)
        }
    }

    suspend fun addCustomPlan(
        title: String,
        category: String,
        description: String,
        location: String,
        duration: String,
        budget: String,
        tags: List<String>,
        imageUrl: String = "",
        imageResName: String = "plan_legos",
        targetFriendId: String? = null,
        targetGroupId: String? = null,
        targetFriendName: String? = null,
        targetGroupName: String? = null,
        scope: PlanScope = PlanScope.GLOBAL,
        peopleCount: String = "2 personas",
        showBudget: Boolean = true,
        showDuration: Boolean = true,
        showLocation: Boolean = true,
        showPeopleCount: Boolean = true,
        existingPlanId: Int? = null
    ): Long {
        val finalId = existingPlanId ?: (((System.currentTimeMillis() % 1_000_000_000L).toInt()) + kotlin.random.Random.nextInt(100, 999))

        val entity = PlanEntity(
            id = finalId,
            title = title,
            category = category,
            detail = "$location / $budget",
            description = description,
            location = location,
            duration = duration,
            budget = budget,
            peopleCount = peopleCount,
            showBudget = showBudget,
            showDuration = showDuration,
            showLocation = showLocation,
            showPeopleCount = showPeopleCount,
            tags = tags,
            imageUrl = imageUrl,
            imageResName = imageResName,
            iconName = getIconNameForCategory(category),
            iconEmoji = getEmojiForCategory(category),
            isCustom = true,
            targetFriendId = targetFriendId,
            targetGroupId = targetGroupId,
            targetFriendName = targetFriendName,
            targetGroupName = targetGroupName,
            scope = scope.name
        )
        planDao.insertPlan(entity)

        try {
            val planMap = mutableMapOf<String, Any>(
                "id" to finalId,
                "title" to title,
                "category" to category,
                "detail" to "$location / $budget",
                "description" to description,
                "location" to location,
                "duration" to duration,
                "budget" to budget,
                "peopleCount" to peopleCount,
                "showBudget" to showBudget,
                "showDuration" to showDuration,
                "showLocation" to showLocation,
                "showPeopleCount" to showPeopleCount,
                "tags" to tags,
                "imageUrl" to imageUrl,
                "imageResName" to imageResName,
                "emoji" to getEmojiForCategory(category),
                "scope" to scope.name,
                "createdAt" to System.currentTimeMillis()
            )
            targetFriendId?.let { planMap["targetFriendId"] = it }
            targetGroupId?.let { planMap["targetGroupId"] = it }
            targetFriendName?.let { planMap["targetFriendName"] = it }
            targetGroupName?.let { planMap["targetGroupName"] = it }

            realtimeDb.getReference("plans").child(finalId.toString()).setValue(planMap)
        } catch (e: Exception) {
            android.util.Log.e("IDATE_FIREBASE", "Excepción al sincronizar plan: ${e.message}")
        }

        return finalId.toLong()
    }

    private fun getEmojiForCategory(category: String): String {
        return when (category.lowercase()) {
            "comida", "restaurante" -> "🍣"
            "película", "cine" -> "🎬"
            "fiesta", "drinks" -> "🍹"
            "juegos" -> "🗝️"
            "aire libre", "naturaleza" -> "🧺"
            "música", "concierto" -> "🎷"
            "arte", "manualidades" -> "🎨"
            else -> "🎉"
        }
    }

    private fun getIconNameForCategory(category: String): String {
        return when (category.lowercase()) {
            "comida" -> "Restaurant"
            "película" -> "Movie"
            "fiesta" -> "LocalBar"
            "juegos" -> "Key"
            "aire libre" -> "Park"
            "música" -> "MusicNote"
            "arte" -> "Palette"
            else -> "Celebration"
        }
    }

    private fun Plan.toEntity(): PlanEntity {
        return PlanEntity(
            id = this.id,
            category = this.category,
            title = this.title,
            detail = this.detail,
            imageResName = getDrawableName(this.imageResId),
            imageUrl = this.imageUrl,
            iconName = this.icon.name,
            iconEmoji = this.iconEmoji,
            description = this.description,
            location = this.location,
            duration = this.duration,
            budget = this.budget,
            peopleCount = this.peopleCount,
            showBudget = this.showBudget,
            showDuration = this.showDuration,
            showLocation = this.showLocation,
            showPeopleCount = this.showPeopleCount,
            tags = this.tags,
            isCustom = false,
            targetFriendId = this.targetFriendId,
            targetGroupId = this.targetGroupId,
            targetFriendName = this.targetFriendName,
            targetGroupName = this.targetGroupName,
            scope = this.scope.name
        )
    }

    private fun PlanEntity.toDomain(): Plan {
        val planScope = try {
            PlanScope.valueOf(this.scope)
        } catch (_: Exception) {
            PlanScope.GLOBAL
        }

        return Plan(
            id = this.id,
            category = this.category,
            title = this.title,
            detail = this.detail,
            imageResId = getDrawableResId(this.imageResName),
            imageUrl = this.imageUrl,
            icon = getIconFromName(this.iconName),
            iconEmoji = this.iconEmoji,
            description = this.description,
            location = this.location,
            duration = this.duration,
            budget = this.budget,
            peopleCount = this.peopleCount,
            showBudget = this.showBudget,
            showDuration = this.showDuration,
            showLocation = this.showLocation,
            showPeopleCount = this.showPeopleCount,
            tags = this.tags,
            categoryColor = Color.Red,
            targetFriendId = this.targetFriendId,
            targetGroupId = this.targetGroupId,
            targetFriendName = this.targetFriendName,
            targetGroupName = this.targetGroupName,
            scope = planScope
        )
    }

    private fun getDrawableName(resId: Int): String {
        return when (resId) {
            R.drawable.plan_legos -> "plan_legos"
            R.drawable.plan_sushi -> "plan_sushi"
            R.drawable.plan_cine -> "plan_cine"
            R.drawable.plan_drinks -> "plan_drinks"
            R.drawable.plan_escape -> "plan_escape"
            R.drawable.plan_boliche -> "plan_boliche"
            R.drawable.plan_picnic -> "plan_picnic"
            R.drawable.plan_tacos -> "plan_tacos"
            R.drawable.plan_pizza -> "plan_pizza"
            R.drawable.plan_senderismo -> "plan_senderismo"
            R.drawable.plan_jazz -> "plan_jazz"
            R.drawable.plan_ceramica -> "plan_ceramica"
            else -> "plan_legos"
        }
    }

    private fun getDrawableResId(name: String): Int {
        return when (name) {
            "plan_legos" -> R.drawable.plan_legos
            "plan_sushi" -> R.drawable.plan_sushi
            "plan_cine" -> R.drawable.plan_cine
            "plan_drinks" -> R.drawable.plan_drinks
            "plan_escape" -> R.drawable.plan_escape
            "plan_boliche" -> R.drawable.plan_boliche
            "plan_picnic" -> R.drawable.plan_picnic
            "plan_tacos" -> R.drawable.plan_tacos
            "plan_pizza" -> R.drawable.plan_pizza
            "plan_senderismo" -> R.drawable.plan_senderismo
            "plan_jazz" -> R.drawable.plan_jazz
            "plan_ceramica" -> R.drawable.plan_ceramica
            else -> R.drawable.plan_legos
        }
    }

    private fun getIconFromName(name: String): ImageVector {
        return when (name) {
            "Restaurant", "Filled.Restaurant" -> Icons.Default.Restaurant
            "Movie", "Filled.Movie" -> Icons.Default.Movie
            "LocalBar", "Filled.LocalBar" -> Icons.Default.LocalBar
            "Key", "Filled.Key" -> Icons.Default.Key
            "Sports", "Filled.Sports" -> Icons.Default.Sports
            "Park", "Filled.Park" -> Icons.Default.Park
            "Fastfood", "Filled.Fastfood" -> Icons.Default.Fastfood
            "LocalPizza", "Filled.LocalPizza" -> Icons.Default.LocalPizza
            "Terrain", "Filled.Terrain" -> Icons.Default.Terrain
            "MusicNote", "Filled.MusicNote" -> Icons.Default.MusicNote
            "Palette", "Filled.Palette" -> Icons.Default.Palette
            "Extension", "Filled.Extension" -> Icons.Default.Extension
            else -> Icons.Default.Celebration
        }
    }
}

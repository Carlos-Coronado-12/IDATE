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
        // Keeps user database prepared
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

    suspend fun removeFriend(friendId: String) {
        friendDao.deleteFriend(friendId)
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

    suspend fun removeGroup(groupId: String) {
        friendDao.deleteGroup(groupId)
    }

    // Plans Sincronization with Target Support
    fun startRealtimePlansSync(scope: CoroutineScope) {
        try {
            realtimeDb.getReference("plans").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            if (!snapshot.exists()) return@launch

                            val remoteEntities = mutableListOf<PlanEntity>()
                            for (child in snapshot.children) {
                                val id = child.child("id").getValue(Long::class.java)?.toInt()
                                    ?: child.key?.toIntOrNull() ?: continue
                                val title = child.child("title").getValue(String::class.java) ?: continue
                                val category = child.child("category").getValue(String::class.java) ?: "Planes"
                                val detail = child.child("detail").getValue(String::class.java) ?: ""
                                val description = child.child("description").getValue(String::class.java) ?: ""
                                val location = child.child("location").getValue(String::class.java) ?: ""
                                val duration = child.child("duration").getValue(String::class.java) ?: ""
                                val budget = child.child("budget").getValue(String::class.java) ?: ""
                                val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                                val imageResName = child.child("imageResName").getValue(String::class.java) ?: "plan_legos"
                                val emoji = child.child("emoji").getValue(String::class.java) ?: getEmojiForCategory(category)
                                val targetFriendId = child.child("targetFriendId").getValue(String::class.java)
                                val targetGroupId = child.child("targetGroupId").getValue(String::class.java)
                                val targetFriendName = child.child("targetFriendName").getValue(String::class.java)
                                val targetGroupName = child.child("targetGroupName").getValue(String::class.java)
                                val scopeStr = child.child("scope").getValue(String::class.java) ?: "GLOBAL"

                                val tagsList = mutableListOf<String>()
                                child.child("tags").children.forEach { tagSnap ->
                                    tagSnap.getValue(String::class.java)?.let { tagsList.add(it) }
                                }

                                remoteEntities.add(
                                    PlanEntity(
                                        id = id,
                                        category = category,
                                        title = title,
                                        detail = if (detail.isNotBlank()) detail else "$location / $budget",
                                        imageResName = imageResName,
                                        imageUrl = imageUrl,
                                        iconName = getIconNameForCategory(category),
                                        iconEmoji = emoji,
                                        description = description,
                                        location = location,
                                        duration = duration,
                                        budget = budget,
                                        tags = tagsList,
                                        isCustom = true,
                                        targetFriendId = targetFriendId,
                                        targetGroupId = targetGroupId,
                                        targetFriendName = targetFriendName,
                                        targetGroupName = targetGroupName,
                                        scope = scopeStr
                                    )
                                )
                            }

                            if (remoteEntities.isNotEmpty()) {
                                planDao.insertPlans(remoteEntities)

                                val remoteIds = remoteEntities.map { it.id }.toSet()
                                val localPlans = planDao.getAllPlansList()
                                for (local in localPlans) {
                                    if (local.isCustom && !remoteIds.contains(local.id)) {
                                        planDao.deletePlanById(local.id)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("IDATE_SYNC", "Error sincronización: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("IDATE_SYNC", "Error listener plans: ${error.message}")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("IDATE_SYNC", "Error iniciando sync: ${e.message}")
        }
    }

    suspend fun deleteAllPlans() {
        planDao.deleteAllPlans()
        likedPlanDao.clearAllLikedPlans()
        try {
            realtimeDb.getReference("plans").removeValue()
        } catch (_: Exception) {}
    }

    suspend fun deletePlan(planId: Int) {
        planDao.deletePlanById(planId)
        likedPlanDao.deleteLikedPlanByPlanId(planId)
        try {
            realtimeDb.getReference("plans").child(planId.toString()).removeValue()
        } catch (_: Exception) {}
    }

    suspend fun restoreDefaultPlans() {
        val entities = SamplePlans.defaultPlans.map { it.toEntity() }
        planDao.insertPlans(entities)
        entities.forEach { entity ->
            val planMap = mapOf(
                "id" to entity.id,
                "title" to entity.title,
                "category" to entity.category,
                "detail" to entity.detail,
                "description" to entity.description,
                "location" to entity.location,
                "duration" to entity.duration,
                "budget" to entity.budget,
                "tags" to entity.tags,
                "imageUrl" to entity.imageUrl,
                "imageResName" to entity.imageResName,
                "emoji" to entity.iconEmoji,
                "scope" to "GLOBAL",
                "createdAt" to entity.createdAt
            )
            realtimeDb.getReference("plans").child(entity.id.toString()).setValue(planMap)
        }
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
        scope: PlanScope = PlanScope.GLOBAL
    ): Long {
        val generatedId = ((System.currentTimeMillis() % 1_000_000_000L).toInt()) + kotlin.random.Random.nextInt(100, 999)

        val entity = PlanEntity(
            id = generatedId,
            title = title,
            category = category,
            detail = "$location / $budget",
            description = description,
            location = location,
            duration = duration,
            budget = budget,
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
                "id" to generatedId,
                "title" to title,
                "category" to category,
                "detail" to "$location / $budget",
                "description" to description,
                "location" to location,
                "duration" to duration,
                "budget" to budget,
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

            realtimeDb.getReference("plans").child(generatedId.toString()).setValue(planMap)
        } catch (e: Exception) {
            android.util.Log.e("IDATE_FIREBASE", "Excepción al sincronizar plan: ${e.message}")
        }

        return generatedId.toLong()
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

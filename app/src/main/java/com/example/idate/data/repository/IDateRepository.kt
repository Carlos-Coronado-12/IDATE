package com.example.idate.data.repository

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.idate.R
import com.example.idate.data.local.IDateDatabase
import com.example.idate.data.local.entity.LikedPlanEntity
import com.example.idate.data.local.entity.PlanEntity
import com.example.idate.model.Plan
import com.example.idate.model.SamplePlans
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class IDateRepository(context: Context) {
    private val database = IDateDatabase.getDatabase(context)
    private val planDao = database.planDao()
    private val likedPlanDao = database.likedPlanDao()
    private val matchSessionDao = database.matchSessionDao()

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
        // Keeps user-created database without auto-inserting sample plans
    }

    fun startRealtimePlansSync(scope: CoroutineScope) {
        try {
            realtimeDb.getReference("plans").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            if (!snapshot.exists()) {
                                // If database in cloud is empty and local custom plans exist, or vice-versa
                                return@launch
                            }

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
                                        isCustom = true
                                    )
                                )
                            }

                            if (remoteEntities.isNotEmpty()) {
                                planDao.insertPlans(remoteEntities)

                                // Remove local plans that were deleted from Firebase Realtime Database
                                val remoteIds = remoteEntities.map { it.id }.toSet()
                                val localPlans = planDao.getAllPlansList()
                                for (local in localPlans) {
                                    if (local.isCustom && !remoteIds.contains(local.id)) {
                                        planDao.deletePlanById(local.id)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("IDATE_SYNC", "Error al procesar sincronización: ${e.message}")
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
        // Upload defaults to Firebase so other devices get them too
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
        imageResName: String = "plan_legos"
    ): Long {
        // Generate unique ID across multiple devices
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
            isCustom = true
        )
        planDao.insertPlan(entity)

        // Broadcast to Firebase Realtime Database
        try {
            val planMap = mapOf(
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
                "createdAt" to System.currentTimeMillis()
            )

            realtimeDb.getReference("plans").child(generatedId.toString()).setValue(planMap)
                .addOnSuccessListener {
                    android.util.Log.d("IDATE_FIREBASE", "Plan $title sincronizado a Realtime Database con éxito!")
                }
                .addOnFailureListener { err ->
                    android.util.Log.e("IDATE_FIREBASE", "Error al subir a Realtime Database: ${err.message}")
                }

            // Sync to Firestore concurrently
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("plans").document(generatedId.toString())
                .set(planMap)
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
            isCustom = false
        )
    }

    private fun PlanEntity.toDomain(): Plan {
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
            categoryColor = Color.Red
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

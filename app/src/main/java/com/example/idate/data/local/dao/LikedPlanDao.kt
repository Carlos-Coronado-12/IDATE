package com.example.idate.data.local.dao

import androidx.room.*
import com.example.idate.data.local.entity.LikedPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedPlanDao {
    @Query("SELECT * FROM liked_plans ORDER BY likedAt DESC")
    fun getAllLikedPlans(): Flow<List<LikedPlanEntity>>

    @Query("SELECT * FROM liked_plans")
    suspend fun getLikedPlansList(): List<LikedPlanEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_plans WHERE planId = :planId)")
    fun isPlanLikedFlow(planId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_plans WHERE planId = :planId)")
    suspend fun isPlanLiked(planId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedPlan(likedPlan: LikedPlanEntity)

    @Query("DELETE FROM liked_plans WHERE planId = :planId")
    suspend fun deleteLikedPlanByPlanId(planId: Int)

    @Query("DELETE FROM liked_plans")
    suspend fun clearAllLikedPlans()
}

package com.example.idate.data.local.dao

import androidx.room.*
import com.example.idate.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY id ASC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans ORDER BY id ASC")
    suspend fun getAllPlansList(): List<PlanEntity>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlanById(id: Int): PlanEntity?

    @Query("SELECT * FROM plans WHERE category = :category ORDER BY id ASC")
    fun getPlansByCategory(category: String): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<PlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Delete
    suspend fun deletePlan(plan: PlanEntity)

    @Query("DELETE FROM plans")
    suspend fun deleteAllPlans()

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlanById(id: Int)

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun getPlansCount(): Int

}

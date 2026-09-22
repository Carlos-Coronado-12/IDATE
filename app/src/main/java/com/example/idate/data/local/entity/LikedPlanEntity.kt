package com.example.idate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_plans")
data class LikedPlanEntity(
    @PrimaryKey
    val planId: Int,
    val likedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isScheduled: Boolean = false,
    val scheduledDate: String = ""
)

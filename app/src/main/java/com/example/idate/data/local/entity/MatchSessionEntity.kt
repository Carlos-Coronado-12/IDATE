package com.example.idate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_sessions")
data class MatchSessionEntity(
    @PrimaryKey
    val roomCode: String,
    val partnerName: String = "Pareja",
    val matchedPlanIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

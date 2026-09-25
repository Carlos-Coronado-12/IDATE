package com.example.idate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "date_decks")
data class DateDeckEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val iconEmoji: String = "🎴",
    val colorHex: Long = 0xFF2E7D32,
    val planIds: List<Int> = emptyList(),
    val createdBy: String = "",
    val isImported: Boolean = false,
    val targetGroupId: String? = null,
    val targetFriendId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

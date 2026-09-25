package com.example.idate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val title: String,
    val detail: String,
    val imageResName: String = "",
    val imageUrl: String = "",
    val iconName: String = "Celebration",
    val iconEmoji: String = "🎉",
    val description: String = "",
    val location: String = "",
    val duration: String = "",
    val budget: String = "",
    val peopleCount: String = "2 personas",
    val showBudget: Boolean = true,
    val showDuration: Boolean = true,
    val showLocation: Boolean = true,
    val showPeopleCount: Boolean = true,
    val tags: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val targetFriendId: String? = null,
    val targetGroupId: String? = null,
    val targetFriendName: String? = null,
    val targetGroupName: String? = null,
    val scope: String = "GLOBAL",
    val createdAt: Long = System.currentTimeMillis()
)

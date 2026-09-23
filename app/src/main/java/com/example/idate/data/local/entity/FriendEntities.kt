package com.example.idate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val friendCode: String,
    val avatarEmoji: String = "😎",
    val bio: String = "¡Listo para los mejores planes!",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    val id: String,
    val friendCode: String,
    val name: String,
    val avatarEmoji: String = "👋",
    val status: String = "ACCEPTED", // PENDING, ACCEPTED, BLOCKED
    val mutualMatchesCount: Int = 0,
    val isOnline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val groupCode: String,
    val name: String,
    val description: String = "",
    val iconEmoji: String = "🎉",
    val colorHex: Long = 0xFF6200EE,
    val memberCount: Int = 1,
    val memberNames: List<String> = emptyList(),
    val matchedPlansCount: Int = 0,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_likes", primaryKeys = ["targetId", "planId"])
data class TargetLikeEntity(
    val targetId: String, // friendId or groupId
    val isGroup: Boolean,
    val planId: Int,
    val likedByUserId: String,
    val likedByUserName: String,
    val timestamp: Long = System.currentTimeMillis()
)

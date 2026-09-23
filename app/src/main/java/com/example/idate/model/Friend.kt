package com.example.idate.model

enum class PlanScope {
    GLOBAL,
    FRIEND_ONLY,
    GROUP_ONLY
}

enum class FriendStatus {
    PENDING,
    ACCEPTED,
    BLOCKED
}

data class UserProfile(
    val userId: String,
    val name: String,
    val friendCode: String,
    val avatarEmoji: String = "😎",
    val bio: String = "¡Listo para los mejores planes!"
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserAvatar: String = "👋",
    val fromUserCode: String = "",
    val toUserId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Friend(
    val id: String,
    val friendCode: String,
    val name: String,
    val avatarEmoji: String = "👋",
    val status: FriendStatus = FriendStatus.ACCEPTED,
    val mutualMatchesCount: Int = 0,
    val isOnline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class FriendGroup(
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

data class ActivePlanningContext(
    val type: ContextType = ContextType.GLOBAL,
    val friend: Friend? = null,
    val group: FriendGroup? = null
) {
    enum class ContextType {
        GLOBAL,
        FRIEND,
        GROUP
    }

    val displayName: String
        get() = when (type) {
            ContextType.GLOBAL -> "Todos los Planes"
            ContextType.FRIEND -> "Planes con ${friend?.name ?: "Amigo"}"
            ContextType.GROUP -> "Grupo: ${group?.name ?: "Grupo"}"
        }

    val emoji: String
        get() = when (type) {
            ContextType.GLOBAL -> "✨"
            ContextType.FRIEND -> friend?.avatarEmoji ?: "👤"
            ContextType.GROUP -> group?.iconEmoji ?: "👥"
        }
}

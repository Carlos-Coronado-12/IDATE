package com.example.idate.data.remote.model

data class LiveRoom(
    val roomCode: String,
    val hostName: String,
    val partnerName: String? = null,
    val hostLikes: Set<Int> = emptySet(),
    val partnerLikes: Set<Int> = emptySet(),
    val matchedPlanIds: List<Int> = emptyList(),
    val isConnected: Boolean = false,
    val lastEventMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

sealed class LiveSessionEvent {
    data class PartnerJoined(val partnerName: String) : LiveSessionEvent()
    data class PartnerSwiped(val partnerName: String, val planTitle: String) : LiveSessionEvent()
    data class RealtimeMatchFound(val planId: Int, val planTitle: String) : LiveSessionEvent()
    data class Error(val message: String) : LiveSessionEvent()
    object SessionEnded : LiveSessionEvent()
}

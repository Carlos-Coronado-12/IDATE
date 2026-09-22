package com.example.idate.data.remote

import com.example.idate.data.remote.model.LiveRoom
import com.example.idate.data.remote.model.LiveSessionEvent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object RealtimeSessionManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentRoom = MutableStateFlow<LiveRoom?>(null)
    val currentRoom: StateFlow<LiveRoom?> = _currentRoom.asStateFlow()

    private val _sessionEvents = MutableSharedFlow<LiveSessionEvent>(replay = 1)
    val sessionEvents: SharedFlow<LiveSessionEvent> = _sessionEvents.asSharedFlow()

    private var isHost = false
    private var currentUserName = "Tú"

    // Firebase Realtime Database Reference & Listener
    private val realtimeDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance().apply {
                try { setPersistenceEnabled(true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            FirebaseDatabase.getInstance("https://idate-8948b-default-rtdb.firebaseio.com")
        }
    }
    private var currentRoomRef: DatabaseReference? = null
    private var roomValueListener: ValueEventListener? = null

    private val announcedMatches = mutableSetOf<Int>()

    fun createRoom(hostName: String): LiveRoom {
        val code = generateRoomCode()
        val host = hostName.ifBlank { "Anfitrión" }
        val room = LiveRoom(
            roomCode = code,
            hostName = host,
            isConnected = true,
            lastEventMessage = "Sala creada. Comparte el código con tu pareja."
        )
        isHost = true
        currentUserName = host
        announcedMatches.clear()
        _currentRoom.value = room

        // Initialize room in Firebase Realtime Database
        val roomData = mapOf(
            "roomCode" to code,
            "hostName" to host,
            "partnerName" to "",
            "lastEventMessage" to "Sala creada. Esperando a tu pareja...",
            "createdAt" to System.currentTimeMillis()
        )
        realtimeDb.getReference("rooms").child(code).setValue(roomData)
        listenToRealtimeDbRoom(code)

        return room
    }

    fun joinRoom(roomCode: String, guestName: String): Result<LiveRoom> {
        val formattedCode = roomCode.trim().uppercase()
        val guest = guestName.ifBlank { "Pareja" }
        isHost = false
        currentUserName = guest
        announcedMatches.clear()

        val initialRoom = LiveRoom(
            roomCode = formattedCode,
            hostName = "Conectando...",
            partnerName = guest,
            isConnected = true,
            lastEventMessage = "Uniéndote a la sala $formattedCode..."
        )
        _currentRoom.value = initialRoom

        // Update partner name and status in Firebase Realtime Database
        val roomRef = realtimeDb.getReference("rooms").child(formattedCode)
        roomRef.child("partnerName").setValue(guest)
        roomRef.child("lastEventMessage").setValue("$guest se unió a la sala.")

        listenToRealtimeDbRoom(formattedCode)

        coroutineScope.launch {
            _sessionEvents.emit(LiveSessionEvent.PartnerJoined(guest))
        }

        return Result.success(initialRoom)
    }

    private fun listenToRealtimeDbRoom(code: String) {
        detachListener()
        try {
            val ref = realtimeDb.getReference("rooms").child(code)
            currentRoomRef = ref

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val hostName = snapshot.child("hostName").getValue(String::class.java) ?: "Anfitrión"
                    val partnerName = snapshot.child("partnerName").getValue(String::class.java)

                    val hostLikesSet = mutableSetOf<Int>()
                    snapshot.child("hostLikes").children.forEach { child ->
                        val id = child.getValue(Long::class.java)?.toInt() 
                            ?: child.key?.toIntOrNull()
                        if (id != null) hostLikesSet.add(id)
                    }

                    val partnerLikesSet = mutableSetOf<Int>()
                    snapshot.child("partnerLikes").children.forEach { child ->
                        val id = child.getValue(Long::class.java)?.toInt() 
                            ?: child.key?.toIntOrNull()
                        if (id != null) partnerLikesSet.add(id)
                    }

                    val matchedIdsList = mutableListOf<Int>()
                    snapshot.child("matchedPlanIds").children.forEach { child ->
                        child.getValue(Long::class.java)?.toInt()?.let { matchedIdsList.add(it) }
                    }

                    // Strict real match: plan liked by BOTH host and partner
                    val realMutualMatches = hostLikesSet.intersect(partnerLikesSet).toList()
                    val allMatches = (matchedIdsList + realMutualMatches).distinct()

                    // Check if there are newly formed mutual matches not yet stored in Firebase
                    if (realMutualMatches.isNotEmpty()) {
                        val unsavedMatches = realMutualMatches - matchedIdsList.toSet()
                        if (unsavedMatches.isNotEmpty()) {
                            ref.child("matchedPlanIds").setValue(allMatches)
                        }
                    }

                    val lastMsg = snapshot.child("lastEventMessage").getValue(String::class.java) ?: "Sincronizado"

                    val updatedRoom = LiveRoom(
                        roomCode = code,
                        hostName = hostName,
                        partnerName = if (partnerName.isNullOrBlank()) null else partnerName,
                        hostLikes = hostLikesSet,
                        partnerLikes = partnerLikesSet,
                        matchedPlanIds = allMatches,
                        isConnected = true,
                        lastEventMessage = lastMsg
                    )

                    _currentRoom.value = updatedRoom

                    // Announce any new mutual matches
                    for (matchId in allMatches) {
                        if (!announcedMatches.contains(matchId)) {
                            announcedMatches.add(matchId)
                            coroutineScope.launch {
                                _sessionEvents.emit(LiveSessionEvent.RealtimeMatchFound(matchId, "¡Cita Elegida!"))
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    coroutineScope.launch {
                        _sessionEvents.emit(LiveSessionEvent.Error(error.message))
                    }
                }
            }

            roomValueListener = listener
            ref.addValueEventListener(listener)
        } catch (_: Exception) {}
    }

    fun submitVote(planId: Int, isLike: Boolean, planTitle: String) {
        val room = _currentRoom.value ?: return
        val currentCode = room.roomCode

        if (!isLike) return

        val roomRef = realtimeDb.getReference("rooms").child(currentCode)
        if (isHost) {
            roomRef.child("hostLikes").child(planId.toString()).setValue(planId)
        } else {
            roomRef.child("partnerLikes").child(planId.toString()).setValue(planId)
        }
        roomRef.child("lastEventMessage").setValue("$currentUserName votó por $planTitle")
    }

    private fun detachListener() {
        roomValueListener?.let { listener ->
            currentRoomRef?.removeEventListener(listener)
        }
        roomValueListener = null
        currentRoomRef = null
    }

    fun leaveRoom() {
        val room = _currentRoom.value
        if (room != null) {
            detachListener()
            _currentRoom.value = null
            announcedMatches.clear()
            coroutineScope.launch {
                _sessionEvents.emit(LiveSessionEvent.SessionEnded)
            }
        }
    }

    private fun generateRoomCode(): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val numbers = "23456789"
        val prefix = (1..3).map { letters.random() }.joinToString("")
        val suffix = (1..3).map { numbers.random() }.joinToString("")
        return "$prefix-$suffix"
    }
}

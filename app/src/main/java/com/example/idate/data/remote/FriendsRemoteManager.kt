package com.example.idate.data.remote

import android.util.Log
import com.example.idate.model.*
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class FriendEvent {
    data class FriendRequestAccepted(val friendName: String) : FriendEvent()
    data class FriendMatchFound(val planId: Int, val planTitle: String, val friendName: String) : FriendEvent()
    data class GroupMatchFound(
        val planId: Int,
        val planTitle: String,
        val groupName: String,
        val voteCount: Int,
        val likedUserNames: List<String> = emptyList()
    ) : FriendEvent()
    data class MemberJoinedGroup(val memberName: String, val groupName: String) : FriendEvent()
    data class Info(val message: String) : FriendEvent()
    data class Error(val message: String) : FriendEvent()
}

object FriendsRemoteManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _friendEvents = MutableSharedFlow<FriendEvent>(replay = 1)
    val friendEvents: SharedFlow<FriendEvent> = _friendEvents.asSharedFlow()

    private val _activeGroupMatches = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val activeGroupMatches: StateFlow<Map<String, List<Int>>> = _activeGroupMatches.asStateFlow()

    private val realtimeDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance().apply {
                try { setPersistenceEnabled(true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            FirebaseDatabase.getInstance("https://idate-8948b-default-rtdb.firebaseio.com")
        }
    }

    private val activeGroupListeners = mutableMapOf<String, ValueEventListener>()
    private val activeFriendListeners = mutableMapOf<String, ValueEventListener>()
    private val notifiedMatches = mutableSetOf<String>()

    // Register or Sync Current User Profile in Firebase
    fun syncUserProfileToCloud(profile: UserProfile) {
        try {
            val userRef = realtimeDb.getReference("users").child(profile.friendCode)
            val data = mapOf(
                "userId" to profile.userId,
                "name" to profile.name,
                "friendCode" to profile.friendCode,
                "avatarEmoji" to profile.avatarEmoji,
                "bio" to profile.bio,
                "updatedAt" to System.currentTimeMillis()
            )
            userRef.setValue(data)
        } catch (e: Exception) {
            Log.e("IDATE_FRIENDS", "Error sincronizando perfil: ${e.message}")
        }
    }

    // Find a friend by Friend Code
    fun findFriendByCode(code: String, onResult: (Result<Friend>) -> Unit) {
        val formattedCode = code.trim().uppercase()
        realtimeDb.getReference("users").child(formattedCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(Result.failure(Exception("No se encontró ningún usuario con el código $formattedCode")))
                        return
                    }
                    val userId = snapshot.child("userId").getValue(String::class.java) ?: formattedCode
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Amigo"
                    val avatar = snapshot.child("avatarEmoji").getValue(String::class.java) ?: "👋"

                    val friend = Friend(
                        id = userId,
                        friendCode = formattedCode,
                        name = name,
                        avatarEmoji = avatar,
                        status = FriendStatus.ACCEPTED
                    )
                    onResult(Result.success(friend))
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(Result.failure(Exception(error.message)))
                }
            })
    }

    // Listen to Friend Pair Interaction for Realtime Matches
    fun listenToFriendPair(myUserId: String, friendId: String, friendName: String) {
        val pairKey = getPairKey(myUserId, friendId)
        if (activeFriendListeners.containsKey(pairKey)) return

        val ref = realtimeDb.getReference("friendPairs").child(pairKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val votesSnap = snapshot.child("votes")
                val myVotes = mutableSetOf<Int>()
                val friendVotes = mutableSetOf<Int>()

                votesSnap.children.forEach { planSnap ->
                    val planId = planSnap.key?.toIntOrNull() ?: return@forEach
                    if (planSnap.child(myUserId).exists()) myVotes.add(planId)
                    if (planSnap.child(friendId).exists()) friendVotes.add(planId)
                }

                val mutualMatches = myVotes.intersect(friendVotes)
                for (matchId in mutualMatches) {
                    val key = "$pairKey-$matchId"
                    if (!notifiedMatches.contains(key)) {
                        notifiedMatches.add(key)
                        coroutineScope.launch {
                            _friendEvents.emit(
                                FriendEvent.FriendMatchFound(
                                    planId = matchId,
                                    planTitle = "¡Plan con $friendName!",
                                    friendName = friendName
                                )
                            )
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IDATE_FRIENDS", "Error en listener de amigo: ${error.message}")
            }
        }

        activeFriendListeners[pairKey] = listener
        ref.addValueEventListener(listener)
    }

    // Submit Vote for Friend Interaction
    fun submitFriendVote(myUserId: String, friendId: String, planId: Int, isLike: Boolean) {
        if (!isLike) return
        val pairKey = getPairKey(myUserId, friendId)
        val ref = realtimeDb.getReference("friendPairs").child(pairKey).child("votes").child(planId.toString())
        ref.child(myUserId).setValue(System.currentTimeMillis())
    }

    // Create a new Group in Firebase
    fun createGroupInCloud(group: FriendGroup, creatorUserId: String, creatorName: String, onResult: (Result<FriendGroup>) -> Unit) {
        try {
            val groupRef = realtimeDb.getReference("groups").child(group.groupCode)
            val groupData = mapOf(
                "id" to group.id,
                "groupCode" to group.groupCode,
                "name" to group.name,
                "description" to group.description,
                "iconEmoji" to group.iconEmoji,
                "colorHex" to group.colorHex,
                "createdBy" to creatorUserId,
                "createdAt" to group.createdAt,
                "members" to mapOf(creatorUserId to creatorName)
            )
            groupRef.setValue(groupData).addOnSuccessListener {
                listenToGroup(group.groupCode, group.name)
                onResult(Result.success(group.copy(memberCount = 1, memberNames = listOf(creatorName))))
            }.addOnFailureListener {
                onResult(Result.failure(it))
            }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    // Join an existing group by Code
    fun joinGroupByCode(code: String, userId: String, userName: String, onResult: (Result<FriendGroup>) -> Unit) {
        val formattedCode = code.trim().uppercase()
        val groupRef = realtimeDb.getReference("groups").child(formattedCode)

        groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onResult(Result.failure(Exception("No se encontró ningún grupo con el código $formattedCode")))
                    return
                }

                val id = snapshot.child("id").getValue(String::class.java) ?: formattedCode
                val name = snapshot.child("name").getValue(String::class.java) ?: "Grupo"
                val desc = snapshot.child("description").getValue(String::class.java) ?: ""
                val icon = snapshot.child("iconEmoji").getValue(String::class.java) ?: "🎉"
                val color = snapshot.child("colorHex").getValue(Long::class.java) ?: 0xFF6200EE

                // Add current user as member
                groupRef.child("members").child(userId).setValue(userName)

                val membersList = mutableListOf<String>()
                snapshot.child("members").children.forEach { mem ->
                    mem.getValue(String::class.java)?.let { membersList.add(it) }
                }
                if (!membersList.contains(userName)) membersList.add(userName)

                val friendGroup = FriendGroup(
                    id = id,
                    groupCode = formattedCode,
                    name = name,
                    description = desc,
                    iconEmoji = icon,
                    colorHex = color,
                    memberCount = membersList.size,
                    memberNames = membersList
                )

                listenToGroup(formattedCode, name)
                onResult(Result.success(friendGroup))
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(Result.failure(Exception(error.message)))
            }
        })
    }

    // Listen to Group Votes and Matches
    fun listenToGroup(groupCode: String, groupName: String) {
        if (activeGroupListeners.containsKey(groupCode)) return

        val ref = realtimeDb.getReference("groups").child(groupCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val membersMap = mutableMapOf<String, String>()
                snapshot.child("members").children.forEach { mem ->
                    val memId = mem.key ?: return@forEach
                    val memName = mem.getValue(String::class.java) ?: "Miembro"
                    membersMap[memId] = memName
                }

                val votesSnap = snapshot.child("votes")
                val groupMatchesList = mutableListOf<Int>()

                votesSnap.children.forEach { planSnap ->
                    val planId = planSnap.key?.toIntOrNull() ?: return@forEach
                    val voteCount = planSnap.childrenCount.toInt()
                    
                    // Group Match Condition: At least 2 members liked the plan
                    val isGroupMatch = voteCount >= 2

                    if (isGroupMatch) {
                        groupMatchesList.add(planId)
                        val key = "$groupCode-$planId"
                        if (!notifiedMatches.contains(key)) {
                            notifiedMatches.add(key)

                            // Extract the names of who gave like
                            val likersList = mutableListOf<String>()
                            planSnap.children.forEach { voteChild ->
                                val vUserId = voteChild.key ?: return@forEach
                                val vName = voteChild.getValue(String::class.java) 
                                    ?: membersMap[vUserId] 
                                    ?: "Amigo"
                                if (!likersList.contains(vName)) {
                                    likersList.add(vName)
                                }
                            }

                            coroutineScope.launch {
                                _friendEvents.emit(
                                    FriendEvent.GroupMatchFound(
                                        planId = planId,
                                        planTitle = "¡Match de Grupo!",
                                        groupName = groupName,
                                        voteCount = voteCount,
                                        likedUserNames = likersList
                                    )
                                )
                            }
                        }
                    }
                }

                _activeGroupMatches.value = _activeGroupMatches.value + (groupCode to groupMatchesList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IDATE_FRIENDS", "Error listener grupo: ${error.message}")
            }
        }

        activeGroupListeners[groupCode] = listener
        ref.addValueEventListener(listener)
    }

    // Submit Vote in a Group
    fun submitGroupVote(groupCode: String, userId: String, userName: String, planId: Int, isLike: Boolean) {
        if (!isLike) return
        val ref = realtimeDb.getReference("groups").child(groupCode).child("votes").child(planId.toString())
        ref.child(userId).setValue(userName)
    }

    private fun getPairKey(id1: String, id2: String): String {
        return if (id1 < id2) "${id1}_$id2" else "${id2}_$id1"
    }

    fun generateUniqueCode(prefix: String = "ID"): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val codePart = (1..5).map { chars.random() }.joinToString("")
        return "$prefix-$codePart"
    }
}

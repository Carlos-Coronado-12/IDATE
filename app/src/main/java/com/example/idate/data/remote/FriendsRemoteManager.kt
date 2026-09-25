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
    data class DeckInvitationReceived(
        val inviteId: String,
        val deckId: String,
        val deckName: String,
        val description: String = "",
        val colorHex: Long = 0xFFFF4B72L,
        val fromFriendName: String,
        val fromFriendId: String,
        val iconEmoji: String,
        val plans: List<Plan>
    ) : FriendEvent()
    data class Info(val message: String) : FriendEvent()
    data class Error(val message: String) : FriendEvent()
}

object FriendsRemoteManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _friendEvents = MutableSharedFlow<FriendEvent>(replay = 1)
    val friendEvents: SharedFlow<FriendEvent> = _friendEvents.asSharedFlow()

    private val _incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingRequests: StateFlow<List<FriendRequest>> = _incomingRequests.asStateFlow()

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
    private var incomingRequestsListener: ValueEventListener? = null
    private var userFriendsListener: ValueEventListener? = null
    private var allGroupsListener: ValueEventListener? = null
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

    // Send a Friend Request to a target user by Friend Code
    fun sendFriendRequest(fromUser: UserProfile, targetCode: String, onResult: (Result<String>) -> Unit) {
        val formattedCode = targetCode.trim().uppercase()
        realtimeDb.getReference("users").child(formattedCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(Result.failure(Exception("No se encontró ningún usuario con el código $formattedCode")))
                        return
                    }
                    val targetUserId = snapshot.child("userId").getValue(String::class.java) ?: formattedCode
                    val targetName = snapshot.child("name").getValue(String::class.java) ?: "Usuario"

                    if (targetUserId == fromUser.userId) {
                        onResult(Result.failure(Exception("No puedes enviarte una solicitud a ti mismo")))
                        return
                    }

                    // Check if already friends
                    realtimeDb.getReference("userFriends").child(fromUser.userId).child(targetUserId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(friendSnap: DataSnapshot) {
                                if (friendSnap.exists()) {
                                    onResult(Result.failure(Exception("¡Ya son amigos!")))
                                    return
                                }

                                val requestRef = realtimeDb.getReference("friendRequests")
                                    .child(targetUserId)
                                    .child(fromUser.userId)

                                val requestData = mapOf(
                                    "id" to fromUser.userId,
                                    "fromUserId" to fromUser.userId,
                                    "fromUserName" to fromUser.name,
                                    "fromUserAvatar" to fromUser.avatarEmoji,
                                    "fromUserCode" to fromUser.friendCode,
                                    "toUserId" to targetUserId,
                                    "timestamp" to System.currentTimeMillis()
                                )

                                requestRef.setValue(requestData).addOnSuccessListener {
                                    onResult(Result.success("¡Solicitud enviada a $targetName!"))
                                }.addOnFailureListener {
                                    onResult(Result.failure(it))
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onResult(Result.failure(Exception(error.message)))
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(Result.failure(Exception(error.message)))
                }
            })
    }

    // Listen to Incoming Friend Requests for current user
    fun listenToIncomingFriendRequests(myUserId: String) {
        incomingRequestsListener?.let {
            realtimeDb.getReference("friendRequests").child(myUserId).removeEventListener(it)
        }

        val ref = realtimeDb.getReference("friendRequests").child(myUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FriendRequest>()
                snapshot.children.forEach { snap ->
                    val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: ""
                    val fromUserId = snap.child("fromUserId").getValue(String::class.java) ?: ""
                    val fromUserName = snap.child("fromUserName").getValue(String::class.java) ?: "Amigo"
                    val fromUserAvatar = snap.child("fromUserAvatar").getValue(String::class.java) ?: "👋"
                    val fromUserCode = snap.child("fromUserCode").getValue(String::class.java) ?: ""
                    val toUserId = snap.child("toUserId").getValue(String::class.java) ?: myUserId
                    val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                    if (fromUserId.isNotBlank()) {
                        list.add(
                            FriendRequest(
                                id = id,
                                fromUserId = fromUserId,
                                fromUserName = fromUserName,
                                fromUserAvatar = fromUserAvatar,
                                fromUserCode = fromUserCode,
                                toUserId = toUserId,
                                timestamp = timestamp
                            )
                        )
                    }
                }
                _incomingRequests.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IDATE_FRIENDS", "Error en listener de solicitudes: ${error.message}")
            }
        }
        incomingRequestsListener = listener
        ref.addValueEventListener(listener)
    }

    // Accept Friend Request (Creates mutual friendship)
    fun acceptFriendRequest(request: FriendRequest, myProfile: UserProfile, onResult: (Result<Friend>) -> Unit) {
        val rootRef = realtimeDb.reference

        val friendForMe = Friend(
            id = request.fromUserId,
            friendCode = request.fromUserCode,
            name = request.fromUserName,
            avatarEmoji = request.fromUserAvatar,
            status = FriendStatus.ACCEPTED
        )

        val updates = hashMapOf<String, Any>(
            // Add to my friends
            "userFriends/${myProfile.userId}/${request.fromUserId}" to mapOf(
                "id" to request.fromUserId,
                "friendCode" to request.fromUserCode,
                "name" to request.fromUserName,
                "avatarEmoji" to request.fromUserAvatar,
                "createdAt" to System.currentTimeMillis()
            ),
            // Add to the requester's friends
            "userFriends/${request.fromUserId}/${myProfile.userId}" to mapOf(
                "id" to myProfile.userId,
                "friendCode" to myProfile.friendCode,
                "name" to myProfile.name,
                "avatarEmoji" to myProfile.avatarEmoji,
                "createdAt" to System.currentTimeMillis()
            )
        )

        rootRef.updateChildren(updates).addOnSuccessListener {
            // Delete pending request
            realtimeDb.getReference("friendRequests").child(myProfile.userId).child(request.fromUserId).removeValue()
            listenToFriendPair(myProfile.userId, request.fromUserId, request.fromUserName)
            onResult(Result.success(friendForMe))
        }.addOnFailureListener {
            onResult(Result.failure(it))
        }
    }

    // Reject / Delete Friend Request
    fun rejectFriendRequest(request: FriendRequest, myUserId: String) {
        realtimeDb.getReference("friendRequests").child(myUserId).child(request.fromUserId).removeValue()
    }

    // Listen to mutual friends from Firebase in real-time
    fun listenToUserFriends(myUserId: String, onFriendsUpdated: (List<Friend>) -> Unit) {
        userFriendsListener?.let {
            realtimeDb.getReference("userFriends").child(myUserId).removeEventListener(it)
        }

        val ref = realtimeDb.getReference("userFriends").child(myUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Friend>()
                snapshot.children.forEach { snap ->
                    val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: return@forEach
                    val code = snap.child("friendCode").getValue(String::class.java) ?: ""
                    val name = snap.child("name").getValue(String::class.java) ?: "Amigo"
                    val avatar = snap.child("avatarEmoji").getValue(String::class.java) ?: "👋"
                    val createdAt = snap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                    val friend = Friend(
                        id = id,
                        friendCode = code,
                        name = name,
                        avatarEmoji = avatar,
                        status = FriendStatus.ACCEPTED,
                        createdAt = createdAt
                    )
                    list.add(friend)
                    listenToFriendPair(myUserId, id, name)
                }
                onFriendsUpdated(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IDATE_FRIENDS", "Error sincronizando amigos: ${error.message}")
            }
        }
        userFriendsListener = listener
        ref.addValueEventListener(listener)
    }

    // Delete Friend Relationship
    fun deleteFriendPair(myUserId: String, friendId: String) {
        val rootRef = realtimeDb.reference
        val updates = hashMapOf<String, Any?>(
            "userFriends/$myUserId/$friendId" to null,
            "userFriends/$friendId/$myUserId" to null
        )
        rootRef.updateChildren(updates)
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

    // Add a Friend to a Group in Cloud (by Creator)
    fun addFriendToGroupInCloud(groupCode: String, friendId: String, friendName: String, onResult: (Result<Unit>) -> Unit) {
        realtimeDb.getReference("groups").child(groupCode).child("members").child(friendId).setValue(friendName)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
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
                val createdBy = snapshot.child("createdBy").getValue(String::class.java) ?: ""

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
                    memberNames = membersList,
                    createdBy = createdBy
                )

                listenToGroup(formattedCode, name)
                onResult(Result.success(friendGroup))
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(Result.failure(Exception(error.message)))
            }
        })
    }

    // Leave or Delete Group in Cloud
    fun leaveOrDeleteGroupInCloud(groupCode: String, myUserId: String, isCreatorOrSoleMember: Boolean) {
        val groupRef = realtimeDb.getReference("groups").child(groupCode)
        if (isCreatorOrSoleMember) {
            groupRef.removeValue()
        } else {
            groupRef.child("members").child(myUserId).removeValue()
        }
        activeGroupListeners.remove(groupCode)?.let { listener ->
            groupRef.removeEventListener(listener)
        }
    }

    // Listen to all groups that the user is a member of in real-time
    fun listenToAllUserGroups(myUserId: String, onGroupsUpdated: (List<FriendGroup>) -> Unit) {
        allGroupsListener?.let {
            realtimeDb.getReference("groups").removeEventListener(it)
        }

        val ref = realtimeDb.getReference("groups")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FriendGroup>()
                snapshot.children.forEach { groupSnap ->
                    val membersSnap = groupSnap.child("members")
                    if (membersSnap.child(myUserId).exists()) {
                        val id = groupSnap.child("id").getValue(String::class.java) ?: groupSnap.key ?: ""
                        val code = groupSnap.child("groupCode").getValue(String::class.java) ?: groupSnap.key ?: ""
                        val name = groupSnap.child("name").getValue(String::class.java) ?: "Grupo"
                        val desc = groupSnap.child("description").getValue(String::class.java) ?: ""
                        val icon = groupSnap.child("iconEmoji").getValue(String::class.java) ?: "🎉"
                        val color = groupSnap.child("colorHex").getValue(Long::class.java) ?: 0xFF6200EE
                        val createdBy = groupSnap.child("createdBy").getValue(String::class.java) ?: ""

                        val memberNames = mutableListOf<String>()
                        membersSnap.children.forEach { mem ->
                            mem.getValue(String::class.java)?.let { memberNames.add(it) }
                        }

                        val g = FriendGroup(
                            id = id,
                            groupCode = code,
                            name = name,
                            description = desc,
                            iconEmoji = icon,
                            colorHex = color,
                            memberCount = memberNames.size,
                            memberNames = memberNames,
                            createdBy = createdBy
                        )
                        list.add(g)
                        listenToGroup(code, name)
                    }
                }
                onGroupsUpdated(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IDATE_FRIENDS", "Error sincronizando grupos: ${error.message}")
            }
        }
        allGroupsListener = listener
        ref.addValueEventListener(listener)
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

    // Reset Friend Pair Votes
    fun resetFriendPairVotes(myUserId: String, friendId: String) {
        val pairKey = getPairKey(myUserId, friendId)
        realtimeDb.getReference("friendPairs").child(pairKey).child("votes").removeValue()
        notifiedMatches.removeAll { it.startsWith(pairKey) }
    }

    // Reset Group Votes
    fun resetGroupVotes(groupCode: String) {
        realtimeDb.getReference("groups").child(groupCode).child("votes").removeValue()
        notifiedMatches.removeAll { it.startsWith(groupCode) }
        _activeGroupMatches.value = _activeGroupMatches.value - groupCode
    }

    // Reset All Matches in Cloud and Local State
    fun resetAllCloudMatches(myUserId: String, friendIds: List<String>, groupCodes: List<String>) {
        notifiedMatches.clear()
        _activeGroupMatches.value = emptyMap()
        for (friendId in friendIds) {
            val pairKey = getPairKey(myUserId, friendId)
            realtimeDb.getReference("friendPairs").child(pairKey).child("votes").removeValue()
        }
        for (code in groupCodes) {
            realtimeDb.getReference("groups").child(code).child("votes").removeValue()
        }
    }

    // Send Deck Invitation to a Friend with full Plan objects
    fun sendDeckInvitation(
        toFriendId: String,
        fromUser: UserProfile,
        deck: DateDeck,
        plans: List<Plan>,
        onResult: (Result<String>) -> Unit
    ) {
        try {
            val inviteId = "inv_${System.currentTimeMillis()}"
            val plansListMaps = plans.map { plan ->
                mapOf(
                    "id" to plan.id,
                    "title" to plan.title,
                    "category" to plan.category,
                    "detail" to plan.detail,
                    "description" to plan.description,
                    "location" to plan.location,
                    "duration" to plan.duration,
                    "budget" to plan.budget,
                    "peopleCount" to plan.peopleCount,
                    "showBudget" to plan.showBudget,
                    "showDuration" to plan.showDuration,
                    "showLocation" to plan.showLocation,
                    "showPeopleCount" to plan.showPeopleCount,
                    "tags" to plan.tags,
                    "imageUrl" to plan.imageUrl,
                    "iconEmoji" to plan.iconEmoji,
                    "scope" to plan.scope.name
                )
            }

            val inviteData = mapOf(
                "inviteId" to inviteId,
                "deckId" to deck.id,
                "deckName" to deck.name,
                "description" to deck.description,
                "iconEmoji" to deck.iconEmoji,
                "colorHex" to deck.colorHex,
                "planIds" to deck.planIds,
                "plans" to plansListMaps,
                "fromUserId" to fromUser.userId,
                "fromUserName" to fromUser.name,
                "createdAt" to System.currentTimeMillis()
            )
            realtimeDb.getReference("deckInvitations")
                .child(toFriendId)
                .child(inviteId)
                .setValue(inviteData)
                .addOnSuccessListener {
                    onResult(Result.success("¡Invitación a jugar '${deck.name}' enviada con éxito!"))
                }
                .addOnFailureListener { e ->
                    onResult(Result.failure(e))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    // Listen to Deck Invitations received by user
    fun listenToDeckInvitations(myUserId: String) {
        realtimeDb.getReference("deckInvitations").child(myUserId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val inviteId = snapshot.child("inviteId").getValue(String::class.java) ?: snapshot.key ?: return
                    val deckId = snapshot.child("deckId").getValue(String::class.java) ?: return
                    val deckName = snapshot.child("deckName").getValue(String::class.java) ?: "Baraja"
                    val desc = snapshot.child("description").getValue(String::class.java) ?: ""
                    val colorHex = snapshot.child("colorHex").getValue(Long::class.java) ?: 0xFFFF4B72L
                    val fromUserName = snapshot.child("fromUserName").getValue(String::class.java) ?: "Un amigo"
                    val fromUserId = snapshot.child("fromUserId").getValue(String::class.java) ?: ""
                    val iconEmoji = snapshot.child("iconEmoji").getValue(String::class.java) ?: "🃏"

                    val plansList = mutableListOf<Plan>()
                    snapshot.child("plans").children.forEach { planSnap ->
                        parsePlanFromSnapshot(planSnap)?.let { plansList.add(it) }
                    }

                    coroutineScope.launch {
                        _friendEvents.emit(
                            FriendEvent.DeckInvitationReceived(
                                inviteId = inviteId,
                                deckId = deckId,
                                deckName = deckName,
                                description = desc,
                                colorHex = colorHex,
                                fromFriendName = fromUserName,
                                fromFriendId = fromUserId,
                                iconEmoji = iconEmoji,
                                plans = plansList
                            )
                        )
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Delete or Dismiss a deck invitation
    fun dismissDeckInvitation(myUserId: String, inviteId: String) {
        realtimeDb.getReference("deckInvitations").child(myUserId).child(inviteId).removeValue()
    }

    // Import a Deck into a Group in Cloud (Group Pooled Deck) with full Plan objects
    fun importDeckToGroupInCloud(
        groupCode: String,
        userName: String,
        deck: DateDeck,
        plans: List<Plan>,
        onResult: (Result<String>) -> Unit
    ) {
        try {
            val plansListMaps = plans.map { plan ->
                mapOf(
                    "id" to plan.id,
                    "title" to plan.title,
                    "category" to plan.category,
                    "detail" to plan.detail,
                    "description" to plan.description,
                    "location" to plan.location,
                    "duration" to plan.duration,
                    "budget" to plan.budget,
                    "peopleCount" to plan.peopleCount,
                    "showBudget" to plan.showBudget,
                    "showDuration" to plan.showDuration,
                    "showLocation" to plan.showLocation,
                    "showPeopleCount" to plan.showPeopleCount,
                    "tags" to plan.tags,
                    "imageUrl" to plan.imageUrl,
                    "iconEmoji" to plan.iconEmoji,
                    "scope" to plan.scope.name
                )
            }

            val deckData = mapOf(
                "deckId" to deck.id,
                "name" to deck.name,
                "description" to deck.description,
                "iconEmoji" to deck.iconEmoji,
                "colorHex" to deck.colorHex,
                "planIds" to deck.planIds,
                "plans" to plansListMaps,
                "importedBy" to userName,
                "importedAt" to System.currentTimeMillis()
            )
            realtimeDb.getReference("groups")
                .child(groupCode)
                .child("importedDecks")
                .child(deck.id)
                .setValue(deckData)
                .addOnSuccessListener {
                    onResult(Result.success("¡Baraja '${deck.name}' importada a la Baraja Grupal!"))
                }
                .addOnFailureListener { e ->
                    onResult(Result.failure(e))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    // Remove Deck from Group in Cloud
    fun removeDeckFromGroupInCloud(groupCode: String, deckId: String, onResult: (Result<String>) -> Unit) {
        realtimeDb.getReference("groups")
            .child(groupCode)
            .child("importedDecks")
            .child(deckId)
            .removeValue()
            .addOnSuccessListener {
                onResult(Result.success("Baraja eliminada del grupo"))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    // Listen to all Imported Decks for a Group in Cloud
    fun listenToGroupImportedDecks(
        groupCode: String,
        onDecksUpdated: (List<Pair<DateDeck, List<Plan>>>) -> Unit
    ) {
        realtimeDb.getReference("groups").child(groupCode).child("importedDecks")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Pair<DateDeck, List<Plan>>>()
                    snapshot.children.forEach { child ->
                        val id = child.child("deckId").getValue(String::class.java) ?: child.key ?: return@forEach
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val description = child.child("description").getValue(String::class.java) ?: ""
                        val iconEmoji = child.child("iconEmoji").getValue(String::class.java) ?: "🃏"
                        val colorHex = child.child("colorHex").getValue(Long::class.java) ?: 0xFFFF4B72L
                        val planIds = child.child("planIds").children.mapNotNull { it.getValue(Int::class.java) }
                        val importedBy = child.child("importedBy").getValue(String::class.java) ?: ""

                        val plans = mutableListOf<Plan>()
                        child.child("plans").children.forEach { planSnap ->
                            parsePlanFromSnapshot(planSnap)?.let { plans.add(it) }
                        }

                        val deck = DateDeck(
                            id = id,
                            name = name,
                            description = description,
                            iconEmoji = iconEmoji,
                            colorHex = colorHex,
                            planIds = if (plans.isNotEmpty()) plans.map { it.id } else planIds,
                            createdBy = importedBy,
                            isImported = true
                        )
                        list.add(Pair(deck, plans))
                    }
                    onDecksUpdated(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IDATE_FRIENDS", "Error escuchando barajas del grupo: ${error.message}")
                }
            })
    }

    private fun parsePlanFromSnapshot(snap: DataSnapshot): Plan? {
        val id = snap.child("id").getValue(Long::class.java)?.toInt()
            ?: snap.key?.toIntOrNull() ?: return null
        val title = snap.child("title").getValue(String::class.java) ?: return null
        val category = snap.child("category").getValue(String::class.java) ?: "Planes"
        val detail = snap.child("detail").getValue(String::class.java) ?: ""
        val description = snap.child("description").getValue(String::class.java) ?: ""
        val location = snap.child("location").getValue(String::class.java) ?: ""
        val duration = snap.child("duration").getValue(String::class.java) ?: ""
        val budget = snap.child("budget").getValue(String::class.java) ?: ""
        val peopleCount = snap.child("peopleCount").getValue(String::class.java) ?: "2 personas"
        val showBudget = snap.child("showBudget").getValue(Boolean::class.java) ?: true
        val showDuration = snap.child("showDuration").getValue(Boolean::class.java) ?: true
        val showLocation = snap.child("showLocation").getValue(Boolean::class.java) ?: true
        val showPeopleCount = snap.child("showPeopleCount").getValue(Boolean::class.java) ?: true
        val imageUrl = snap.child("imageUrl").getValue(String::class.java) ?: ""
        val emoji = snap.child("iconEmoji").getValue(String::class.java) ?: "🎉"
        val tagsList = mutableListOf<String>()
        snap.child("tags").children.forEach { tagSnap ->
            tagSnap.getValue(String::class.java)?.let { tagsList.add(it) }
        }

        return Plan(
            id = id,
            title = title,
            category = category,
            detail = detail.ifBlank { "$location / $budget" },
            description = description,
            location = location,
            duration = duration,
            budget = budget,
            peopleCount = peopleCount,
            showBudget = showBudget,
            showDuration = showDuration,
            showLocation = showLocation,
            showPeopleCount = showPeopleCount,
            tags = tagsList,
            imageUrl = imageUrl,
            iconEmoji = emoji
        )
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

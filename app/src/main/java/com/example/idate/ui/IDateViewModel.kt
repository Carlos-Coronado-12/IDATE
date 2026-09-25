package com.example.idate.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.idate.data.remote.FriendEvent
import com.example.idate.data.remote.FriendsRemoteManager
import com.example.idate.data.repository.IDateRepository
import com.example.idate.model.*
import com.example.idate.notifications.NotificationHelper
import com.example.idate.ui.components.VerticalSwipeDirection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    SWIPE_SESSION
}

data class IDateUiState(
    val currentScreen: AppScreen = AppScreen.MAIN_MENU,
    val plans: List<Plan> = emptyList(),
    val filteredPlans: List<Plan> = emptyList(),
    val selectedCategory: String = "Todos",
    val currentIndex: Int = 0,
    val savedPlans: List<Plan> = emptyList(),
    val customDecks: List<DateDeck> = emptyList(),
    val groupDecksMap: Map<String, List<DateDeck>> = emptyMap(),
    val showCreateDeckModal: Boolean = false,
    val editingDeck: DateDeck? = null,
    val showShareDeckModal: Boolean = false,
    val deckToShare: DateDeck? = null,
    val matchedPlan: Plan? = null,
    val realtimeMatchPlan: Plan? = null,
    val realtimeMatchLikers: List<String> = emptyList(),
    val realtimeMatchGroupName: String? = null,
    val detailedPlan: Plan? = null,
    val showSavedPlansSheet: Boolean = false,
    val showFriendsHubModal: Boolean = false,
    val showCreatePlanModal: Boolean = false,
    val editingPlan: Plan? = null,
    val userProfile: UserProfile? = null,
    val friendsList: List<Friend> = emptyList(),
    val incomingFriendRequests: List<FriendRequest> = emptyList(),
    val incomingDeckInvitations: List<FriendEvent.DeckInvitationReceived> = emptyList(),
    val groupsList: List<FriendGroup> = emptyList(),
    val activeContext: ActivePlanningContext = ActivePlanningContext(),
    val friendsHubInitialTab: Int = 0,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null
)

class IDateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IDateRepository(application)
    private val notificationHelper = NotificationHelper(application)
    private val vibrator = application.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator

    private val _uiState = MutableStateFlow(IDateUiState())
    val uiState: StateFlow<IDateUiState> = _uiState.asStateFlow()

    private val swipeHistory = mutableListOf<Int>()

    init {
        // Initialize / Load User Profile & Remote Listeners
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
            val profile = repository.getOrCreateUserProfile()
            _uiState.update { it.copy(userProfile = profile) }
            setupRemoteListeners(profile.userId)
            repository.removeDefaultDecks()
        }

        // Observe Custom Decks from Room
        viewModelScope.launch {
            repository.getAllDecks().collect { decks ->
                _uiState.update { it.copy(customDecks = decks) }
            }
        }

        // Observe User Profile updates
        viewModelScope.launch {
            repository.getUserProfileFlow().collect { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(userProfile = profile) }
                    setupRemoteListeners(profile.userId)
                }
            }
        }

        // Observe incoming friend requests from FriendsRemoteManager
        viewModelScope.launch {
            FriendsRemoteManager.incomingRequests.collect { requests ->
                _uiState.update { it.copy(incomingFriendRequests = requests) }
            }
        }

        // Observe Friends list
        viewModelScope.launch {
            repository.getAllFriends().collect { friends ->
                _uiState.update { it.copy(friendsList = friends) }
                // Start listening to friend interactions for real-time matches
                val myProfile = _uiState.value.userProfile
                if (myProfile != null) {
                    friends.forEach { friend ->
                        FriendsRemoteManager.listenToFriendPair(myProfile.userId, friend.id, friend.name)
                    }
                }
            }
        }

        // Observe Groups list and their imported decks
        viewModelScope.launch {
            repository.getAllGroups().collect { groups ->
                _uiState.update { it.copy(groupsList = groups) }
                groups.forEach { group ->
                    FriendsRemoteManager.listenToGroup(group.groupCode, group.name)
                    FriendsRemoteManager.listenToGroupImportedDecks(group.groupCode) { deckWithPlansList ->
                        viewModelScope.launch {
                            deckWithPlansList.forEach { (_, plans) ->
                                if (plans.isNotEmpty()) {
                                    repository.savePlansOnly(plans)
                                }
                            }
                            _uiState.update { current ->
                                val updatedMap = current.groupDecksMap + (group.groupCode to deckWithPlansList.map { it.first })
                                val filtered = applyFilter(current.plans, current.selectedCategory, current.activeContext, updatedMap)
                                current.copy(groupDecksMap = updatedMap, filteredPlans = filtered)
                            }
                        }
                    }
                }
            }
        }

        // Observe Plans from Room
        viewModelScope.launch {
            repository.getAllPlans().collect { planList ->
                _uiState.update { current ->
                    val filtered = applyFilter(planList, current.selectedCategory, current.activeContext, current.groupDecksMap)
                    current.copy(plans = planList, filteredPlans = filtered)
                }
            }
        }

        // Observe liked plans from Room
        viewModelScope.launch {
            repository.getLikedPlans().collect { likedList ->
                _uiState.update { it.copy(savedPlans = likedList) }
            }
        }

        // Observe Realtime Friend / Group Events
        viewModelScope.launch {
            FriendsRemoteManager.friendEvents.collect { event ->
                when (event) {
                    is FriendEvent.FriendMatchFound -> {
                        triggerHaptic(300)
                        val existingPlan = _uiState.value.plans.find { it.id == event.planId }
                        val plan = existingPlan ?: Plan(
                            id = event.planId,
                            title = event.planTitle,
                            category = "Cita",
                            detail = "Coincidencia con ${event.friendName}"
                        )
                        notificationHelper.showMatchNotification(
                            plan.title,
                            event.friendName
                        )
                        val friend = _uiState.value.friendsList.find { it.name == event.friendName }
                        if (friend != null) {
                            viewModelScope.launch {
                                repository.incrementFriendMatches(friend.id)
                            }
                        }
                        _uiState.update {
                            it.copy(
                                realtimeMatchPlan = plan,
                                realtimeMatchLikers = listOf(it.userProfile?.name ?: "Tú", event.friendName),
                                realtimeMatchGroupName = null,
                                toastMessage = "¡Coincidencia con ${event.friendName}!"
                            )
                        }
                    }
                    is FriendEvent.GroupMatchFound -> {
                        triggerHaptic(300)
                        val existingPlan = _uiState.value.plans.find { it.id == event.planId }
                        val plan = existingPlan ?: Plan(
                            id = event.planId,
                            title = event.planTitle,
                            category = "Grupo",
                            detail = "Coincidencia grupal en ${event.groupName}"
                        )
                        notificationHelper.showMatchNotification(
                            plan.title,
                            event.groupName
                        )
                        val group = _uiState.value.groupsList.find { it.name == event.groupName }
                        if (group != null) {
                            viewModelScope.launch {
                                repository.incrementGroupMatches(group.id)
                            }
                        }
                        val likersText = if (event.likedUserNames.isNotEmpty()) {
                            event.likedUserNames.joinToString(" y ")
                        } else {
                            "${event.voteCount} personas"
                        }
                        _uiState.update {
                            it.copy(
                                realtimeMatchPlan = plan,
                                realtimeMatchLikers = event.likedUserNames,
                                realtimeMatchGroupName = event.groupName,
                                toastMessage = "¡A $likersText les gustó '${plan.title}' en ${event.groupName}!"
                            )
                        }
                    }
                    is FriendEvent.DeckInvitationReceived -> {
                        triggerHaptic(250)
                        notificationHelper.showMatchNotification(
                            "Invitación a jugar: ${event.deckName}",
                            event.fromFriendName
                        )
                        _uiState.update { current ->
                            val updatedList = if (current.incomingDeckInvitations.none { it.inviteId == event.inviteId }) {
                                current.incomingDeckInvitations + event
                            } else {
                                current.incomingDeckInvitations
                            }
                            current.copy(
                                incomingDeckInvitations = updatedList,
                                toastMessage = "${event.fromFriendName} te invitó a jugar la baraja '${event.deckName}' ${event.iconEmoji}"
                            )
                        }
                    }
                    is FriendEvent.FriendRequestAccepted -> {
                        _uiState.update { it.copy(toastMessage = "${event.friendName} ahora es tu amigo") }
                    }
                    is FriendEvent.MemberJoinedGroup -> {
                        _uiState.update { it.copy(toastMessage = "${event.memberName} se unió a ${event.groupName}") }
                    }
                    is FriendEvent.Info -> {
                        _uiState.update { it.copy(toastMessage = event.message) }
                    }
                    is FriendEvent.Error -> {
                        _uiState.update { it.copy(errorMessage = event.message) }
                    }
                }
            }
        }
    }

    fun handleSwipe(direction: VerticalSwipeDirection) {
        val currentPlans = _uiState.value.filteredPlans
        val currentPlan = currentPlans.getOrNull(_uiState.value.currentIndex) ?: return
        val isLike = direction == VerticalSwipeDirection.UP

        swipeHistory.add(_uiState.value.currentIndex)

        if (isLike) {
            triggerHaptic(100)
            viewModelScope.launch {
                repository.toggleLikePlan(currentPlan, true)
            }
        }

        val myProfile = _uiState.value.userProfile
        val activeCtx = _uiState.value.activeContext

        if (myProfile != null) {
            // 1. Friend voting (active friend, shared deck friend, or all connected friends)
            val friendId = activeCtx.friend?.id ?: activeCtx.deck?.targetFriendId
            if (friendId != null) {
                FriendsRemoteManager.submitFriendVote(
                    myUserId = myProfile.userId,
                    friendId = friendId,
                    planId = currentPlan.id,
                    isLike = isLike
                )
            } else {
                _uiState.value.friendsList.forEach { friend ->
                    FriendsRemoteManager.submitFriendVote(
                        myUserId = myProfile.userId,
                        friendId = friend.id,
                        planId = currentPlan.id,
                        isLike = isLike
                    )
                }
            }

            // 2. Group voting (active group or group associated with deck)
            val groupCode = activeCtx.group?.groupCode
                ?: _uiState.value.groupsList.find { it.id == activeCtx.deck?.targetGroupId }?.groupCode
            if (groupCode != null) {
                FriendsRemoteManager.submitGroupVote(
                    groupCode = groupCode,
                    userId = myProfile.userId,
                    userName = myProfile.name,
                    planId = currentPlan.id,
                    isLike = isLike
                )
            }
        }

        _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
    }

    fun handleRewind() {
        if (swipeHistory.isNotEmpty()) {
            val lastIndex = swipeHistory.removeAt(swipeHistory.lastIndex)
            _uiState.update { it.copy(currentIndex = lastIndex) }
            triggerHaptic(50)
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { current ->
            val filtered = applyFilter(current.plans, category, current.activeContext, current.groupDecksMap)
            current.copy(
                selectedCategory = category,
                filteredPlans = filtered,
                currentIndex = 0
            )
        }
        swipeHistory.clear()
    }

    fun setActiveContext(context: ActivePlanningContext) {
        _uiState.update { current ->
            val filtered = applyFilter(current.plans, current.selectedCategory, context, current.groupDecksMap)
            current.copy(
                activeContext = context,
                filteredPlans = filtered,
                currentIndex = 0,
                toastMessage = "Modo activado: ${context.displayName}"
            )
        }
        swipeHistory.clear()
    }

    fun resetActiveContext() {
        val defaultDeck = _uiState.value.customDecks.firstOrNull()
        if (defaultDeck != null) {
            setActiveContext(ActivePlanningContext(type = ActivePlanningContext.ContextType.DECK, deck = defaultDeck))
        } else {
            setActiveContext(ActivePlanningContext(type = ActivePlanningContext.ContextType.DECK))
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun startSessionWithFriend(friend: Friend) {
        val defaultDeck = _uiState.value.customDecks.firstOrNull()
        setActiveContext(ActivePlanningContext(type = ActivePlanningContext.ContextType.FRIEND, friend = friend, deck = defaultDeck))
        _uiState.update { it.copy(currentScreen = AppScreen.SWIPE_SESSION, currentIndex = 0) }
        swipeHistory.clear()
    }

    fun startSessionWithGroup(group: FriendGroup) {
        setActiveContext(ActivePlanningContext(type = ActivePlanningContext.ContextType.GROUP, group = group))
        _uiState.update { it.copy(currentScreen = AppScreen.SWIPE_SESSION, currentIndex = 0) }
        swipeHistory.clear()
    }

    fun startSessionWithDeck(deck: DateDeck) {
        setActiveContext(ActivePlanningContext(type = ActivePlanningContext.ContextType.DECK, deck = deck))
        _uiState.update { it.copy(currentScreen = AppScreen.SWIPE_SESSION, currentIndex = 0) }
        swipeHistory.clear()
    }

    // Deck Management
    fun openCreateDeckModal(deck: DateDeck? = null) {
        _uiState.update { it.copy(showCreateDeckModal = true, editingDeck = deck) }
    }

    fun closeCreateDeckModal() {
        _uiState.update { it.copy(showCreateDeckModal = false, editingDeck = null) }
    }

    fun openShareDeckModal(deck: DateDeck) {
        _uiState.update { it.copy(showShareDeckModal = true, deckToShare = deck) }
    }

    fun closeShareDeckModal() {
        _uiState.update { it.copy(showShareDeckModal = false, deckToShare = null) }
    }

    fun saveDeck(
        name: String,
        description: String,
        iconEmoji: String,
        colorHex: Long,
        planIds: List<Int>,
        existingDeckId: String? = null
    ) {
        viewModelScope.launch {
            val myProfile = _uiState.value.userProfile
            val deck = DateDeck(
                id = existingDeckId ?: "deck_${System.currentTimeMillis()}",
                name = name.trim(),
                description = description.trim(),
                iconEmoji = iconEmoji,
                colorHex = colorHex,
                planIds = planIds,
                createdBy = myProfile?.userId ?: "local_user"
            )
            repository.createOrUpdateDeck(deck)
            _uiState.update {
                it.copy(
                    showCreateDeckModal = false,
                    editingDeck = null,
                    toastMessage = "¡Baraja '${deck.name}' guardada con éxito!"
                )
            }
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            if (_uiState.value.activeContext.type == ActivePlanningContext.ContextType.DECK &&
                _uiState.value.activeContext.deck?.id == deckId
            ) {
                resetActiveContext()
            }
            _uiState.update { it.copy(toastMessage = "Baraja eliminada.") }
        }
    }

    fun shareDeckWithGroup(deck: DateDeck, group: FriendGroup) {
        val myProfile = _uiState.value.userProfile
        _uiState.update { it.copy(isConnecting = true) }
        viewModelScope.launch {
            val plans = repository.getPlansForIds(deck.planIds)
            FriendsRemoteManager.importDeckToGroupInCloud(
                groupCode = group.groupCode,
                userName = myProfile?.name ?: "Usuario",
                deck = deck,
                plans = plans
            ) { result ->
                _uiState.update { it.copy(isConnecting = false) }
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            showShareDeckModal = false,
                            deckToShare = null,
                            toastMessage = "¡Baraja '${deck.name}' importada a la Baraja Grupal de ${group.name}!"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Error al importar baraja al grupo") }
                }
            }
        }
    }

    fun removeDeckFromGroup(groupCode: String, deckId: String) {
        _uiState.update { it.copy(isConnecting = true) }
        FriendsRemoteManager.removeDeckFromGroupInCloud(groupCode, deckId) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess { msg ->
                _uiState.update { current ->
                    val currentGroupDecks = current.groupDecksMap[groupCode] ?: emptyList()
                    val updated = currentGroupDecks.filter { it.id != deckId }
                    val newMap = current.groupDecksMap + (groupCode to updated)
                    val updatedFiltered = applyFilter(current.plans, current.selectedCategory, current.activeContext, newMap)
                    current.copy(
                        groupDecksMap = newMap,
                        filteredPlans = updatedFiltered,
                        toastMessage = msg
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al eliminar baraja del grupo") }
            }
        }
    }

    fun sendDeckInvitationToFriend(friend: Friend, deck: DateDeck) {
        val myProfile = _uiState.value.userProfile ?: return
        _uiState.update { it.copy(isConnecting = true) }
        viewModelScope.launch {
            val plans = repository.getPlansForIds(deck.planIds)
            FriendsRemoteManager.sendDeckInvitation(
                toFriendId = friend.id,
                fromUser = myProfile,
                deck = deck,
                plans = plans
            ) { result ->
                _uiState.update { it.copy(isConnecting = false) }
                result.onSuccess { msg ->
                    _uiState.update {
                        it.copy(
                            showShareDeckModal = false,
                            deckToShare = null,
                            toastMessage = msg
                        )
                    }
                    // Iniciar sesión con amigo con esta baraja
                    setActiveContext(
                        ActivePlanningContext(
                            type = ActivePlanningContext.ContextType.FRIEND,
                            friend = friend,
                            deck = deck
                        )
                    )
                    navigateTo(AppScreen.SWIPE_SESSION)
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Error al enviar invitación") }
                }
            }
        }
    }

    fun acceptDeckInvitation(invitation: FriendEvent.DeckInvitationReceived, playNow: Boolean = false) {
        viewModelScope.launch {
            val deck = DateDeck(
                id = invitation.deckId,
                name = invitation.deckName,
                description = invitation.description,
                iconEmoji = invitation.iconEmoji,
                colorHex = invitation.colorHex,
                planIds = invitation.plans.map { it.id },
                createdBy = invitation.fromFriendName,
                isImported = true,
                targetFriendId = invitation.fromFriendId
            )
            repository.importDeckWithPlans(deck, invitation.plans)
            val myUserId = _uiState.value.userProfile?.userId
            if (myUserId != null) {
                FriendsRemoteManager.dismissDeckInvitation(myUserId, invitation.inviteId)
            }
            _uiState.update { current ->
                val remaining = current.incomingDeckInvitations.filter { it.inviteId != invitation.inviteId }
                current.copy(
                    incomingDeckInvitations = remaining,
                    toastMessage = "¡Baraja '${invitation.deckName}' agregada a tus barajas!"
                )
            }

            if (playNow) {
                val friend = _uiState.value.friendsList.find { it.id == invitation.fromFriendId }
                if (friend != null) {
                    setActiveContext(
                        ActivePlanningContext(
                            type = ActivePlanningContext.ContextType.FRIEND,
                            friend = friend,
                            deck = deck
                        )
                    )
                } else {
                    setActiveContext(
                        ActivePlanningContext(
                            type = ActivePlanningContext.ContextType.DECK,
                            deck = deck
                        )
                    )
                }
                navigateTo(AppScreen.SWIPE_SESSION)
            }
        }
    }

    fun dismissDeckInvitation(invitation: FriendEvent.DeckInvitationReceived) {
        val myUserId = _uiState.value.userProfile?.userId
        if (myUserId != null) {
            FriendsRemoteManager.dismissDeckInvitation(myUserId, invitation.inviteId)
        }
        _uiState.update { current ->
            val remaining = current.incomingDeckInvitations.filter { it.inviteId != invitation.inviteId }
            current.copy(incomingDeckInvitations = remaining)
        }
    }

    fun createPlanAndAddToDeck(
        title: String,
        category: String,
        description: String,
        location: String,
        duration: String,
        budget: String,
        tags: List<String>,
        imageUrl: String = "",
        imageResName: String = "plan_legos",
        peopleCount: String = "2 personas",
        showBudget: Boolean = true,
        showDuration: Boolean = true,
        showLocation: Boolean = true,
        showPeopleCount: Boolean = true,
        existingPlanId: Int? = null,
        onPlanCreated: (Plan) -> Unit
    ) {
        viewModelScope.launch {
            val generatedId = repository.addCustomPlan(
                title = title,
                category = category,
                description = description,
                location = location,
                duration = duration,
                budget = budget,
                tags = tags,
                imageUrl = imageUrl,
                imageResName = imageResName,
                peopleCount = peopleCount,
                showBudget = showBudget,
                showDuration = showDuration,
                showLocation = showLocation,
                showPeopleCount = showPeopleCount,
                existingPlanId = existingPlanId
            ).toInt()

            val created = Plan(
                id = generatedId,
                title = title,
                category = category,
                detail = "$location / $budget",
                description = description,
                location = location,
                duration = duration,
                budget = budget,
                peopleCount = peopleCount,
                showBudget = showBudget,
                showDuration = showDuration,
                showLocation = showLocation,
                showPeopleCount = showPeopleCount,
                tags = tags,
                imageUrl = imageUrl
            )
            _uiState.update { it.copy(toastMessage = if (existingPlanId != null) "Plan '$title' actualizado" else "Plan '$title' agregado a la baraja") }
            onPlanCreated(created)
        }
    }

    private fun applyFilter(
        plans: List<Plan>,
        category: String,
        context: ActivePlanningContext,
        groupDecksMap: Map<String, List<DateDeck>> = _uiState.value.groupDecksMap
    ): List<Plan> {
        val categoryFiltered = if (category == "Todos" || category.isBlank()) {
            plans
        } else {
            plans.filter { it.category.equals(category, ignoreCase = true) }
        }

        return when (context.type) {
            ActivePlanningContext.ContextType.GLOBAL -> {
                val fallbackDeck = _uiState.value.customDecks.firstOrNull()
                if (fallbackDeck != null) {
                    categoryFiltered.filter { it.id in fallbackDeck.planIds }
                } else {
                    emptyList()
                }
            }
            ActivePlanningContext.ContextType.FRIEND -> {
                val deckPlanIds = context.deck?.planIds?.toSet()
                if (deckPlanIds != null && deckPlanIds.isNotEmpty()) {
                    categoryFiltered.filter { it.id in deckPlanIds }
                } else {
                    emptyList()
                }
            }
            ActivePlanningContext.ContextType.GROUP -> {
                val groupCode = context.group?.groupCode
                val groupDecks = if (groupCode != null) groupDecksMap[groupCode] ?: emptyList() else emptyList()
                val groupDeckPlanIds = groupDecks.flatMap { it.planIds }.toSet()

                if (groupDeckPlanIds.isNotEmpty()) {
                    categoryFiltered.filter { it.id in groupDeckPlanIds }
                } else {
                    emptyList()
                }
            }
            ActivePlanningContext.ContextType.DECK -> {
                val allowedPlanIds = context.deck?.planIds?.toSet() ?: emptySet()
                categoryFiltered.filter { it.id in allowedPlanIds }
            }
        }
    }

    fun resetDeck() {
        _uiState.update { it.copy(currentIndex = 0) }
        swipeHistory.clear()
    }

    fun toggleSavePlan(plan: Plan) {
        val isSaved = _uiState.value.savedPlans.any { it.id == plan.id }
        viewModelScope.launch {
            repository.toggleLikePlan(plan, !isSaved)
        }
    }

    private fun setupRemoteListeners(userId: String) {
        FriendsRemoteManager.listenToIncomingFriendRequests(userId)
        FriendsRemoteManager.listenToDeckInvitations(userId)
        FriendsRemoteManager.listenToUserFriends(userId) { remoteFriends ->
            viewModelScope.launch {
                repository.syncFriendsFromRemote(remoteFriends)
            }
        }
        FriendsRemoteManager.listenToAllUserGroups(userId) { remoteGroups ->
            viewModelScope.launch {
                repository.syncGroupsFromRemote(remoteGroups)
            }
        }
    }

    // Friends Actions (Friend Requests & Management)
    fun sendFriendRequestByCode(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un código de amigo válido") }
            return
        }

        val myProfile = _uiState.value.userProfile
        if (myProfile == null) {
            _uiState.update { it.copy(errorMessage = "Cargando perfil...") }
            return
        }

        if (code.trim().equals(myProfile.friendCode, ignoreCase = true)) {
            _uiState.update { it.copy(errorMessage = "No puedes enviarte una solicitud a ti mismo") }
            return
        }

        _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
        FriendsRemoteManager.sendFriendRequest(myProfile, code) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess { successMsg ->
                _uiState.update {
                    it.copy(
                        toastMessage = successMsg,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al enviar solicitud") }
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val myProfile = _uiState.value.userProfile ?: return
        _uiState.update { it.copy(isConnecting = true) }
        FriendsRemoteManager.acceptFriendRequest(request, myProfile) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess { friend ->
                viewModelScope.launch {
                    repository.addFriend(friend)
                    _uiState.update {
                        it.copy(
                            toastMessage = "¡Ahora eres amigo de ${friend.name}!",
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al aceptar solicitud") }
            }
        }
    }

    fun rejectFriendRequest(request: FriendRequest) {
        val myProfile = _uiState.value.userProfile ?: return
        FriendsRemoteManager.rejectFriendRequest(request, myProfile.userId)
        _uiState.update { it.copy(toastMessage = "Solicitud rechazada") }
    }

    fun removeFriend(friendId: String) {
        val myProfile = _uiState.value.userProfile
        viewModelScope.launch {
            if (myProfile != null) {
                FriendsRemoteManager.deleteFriendPair(myProfile.userId, friendId)
            }
            repository.removeFriend(friendId)
            if (_uiState.value.activeContext.type == ActivePlanningContext.ContextType.FRIEND &&
                _uiState.value.activeContext.friend?.id == friendId) {
                resetActiveContext()
            }
            _uiState.update { it.copy(toastMessage = "Amigo eliminado.") }
        }
    }

    // Groups Actions
    fun createGroup(name: String, description: String, iconEmoji: String) {
        val myProfile = _uiState.value.userProfile ?: return
        val newCode = FriendsRemoteManager.generateUniqueCode("GRP")
        val newGroup = FriendGroup(
            id = "grp_${System.currentTimeMillis()}",
            groupCode = newCode,
            name = name.trim(),
            description = description.trim(),
            iconEmoji = iconEmoji,
            createdBy = myProfile.userId
        )

        _uiState.update { it.copy(isConnecting = true) }
        FriendsRemoteManager.createGroupInCloud(newGroup, myProfile.userId, myProfile.name) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess { group ->
                viewModelScope.launch {
                    repository.addGroup(group)
                    _uiState.update {
                        it.copy(
                            toastMessage = "¡Grupo '${group.name}' creado con código ${group.groupCode}!",
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al crear grupo") }
            }
        }
    }

    fun addFriendToGroup(groupCode: String, friend: Friend) {
        _uiState.update { it.copy(isConnecting = true) }
        FriendsRemoteManager.addFriendToGroupInCloud(groupCode, friend.id, friend.name) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        toastMessage = "¡${friend.name} añadido al grupo!",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al añadir amigo al grupo") }
            }
        }
    }

    fun joinGroupByCode(code: String) {
        val myProfile = _uiState.value.userProfile ?: return
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un código de grupo válido") }
            return
        }

        _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
        FriendsRemoteManager.joinGroupByCode(code, myProfile.userId, myProfile.name) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess { group ->
                viewModelScope.launch {
                    repository.addGroup(group)
                    _uiState.update {
                        it.copy(
                            toastMessage = "¡Te has unido al grupo '${group.name}'!",
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al unirse al grupo") }
            }
        }
    }

    fun removeGroup(groupId: String) {
        val myProfile = _uiState.value.userProfile
        val group = _uiState.value.groupsList.find { it.id == groupId }
        viewModelScope.launch {
            if (myProfile != null && group != null) {
                val isCreatorOrSoleMember = group.createdBy == myProfile.userId || group.memberCount <= 1
                FriendsRemoteManager.leaveOrDeleteGroupInCloud(
                    groupCode = group.groupCode,
                    myUserId = myProfile.userId,
                    isCreatorOrSoleMember = isCreatorOrSoleMember
                )
            }
            repository.removeGroup(groupId)
            if (_uiState.value.activeContext.type == ActivePlanningContext.ContextType.GROUP &&
                _uiState.value.activeContext.group?.id == groupId) {
                resetActiveContext()
            }
            _uiState.update { it.copy(toastMessage = "Has salido del grupo.") }
        }
    }

    fun updateUserProfile(name: String, avatarEmoji: String, bio: String) {
        viewModelScope.launch {
            repository.updateUserProfile(name, avatarEmoji, bio)
            _uiState.update { it.copy(toastMessage = "Perfil actualizado correctamente") }
        }
    }

    fun resetAllMatches() {
        val myProfile = _uiState.value.userProfile
        val friendIds = _uiState.value.friendsList.map { it.id }
        val groupCodes = _uiState.value.groupsList.map { it.groupCode }
        viewModelScope.launch {
            if (myProfile != null) {
                repository.resetAllUserMatches(myProfile.userId, friendIds, groupCodes)
            }
            _uiState.update { it.copy(toastMessage = "¡Todos los matches y votos han sido reiniciados!") }
        }
    }

    fun addCustomPlan(
        title: String,
        category: String,
        description: String,
        location: String,
        duration: String,
        budget: String,
        tags: List<String>,
        imageUrl: String = "",
        imageResName: String = "plan_legos",
        targetFriendId: String? = null,
        targetGroupId: String? = null,
        targetFriendName: String? = null,
        targetGroupName: String? = null,
        scope: PlanScope = PlanScope.GLOBAL,
        peopleCount: String = "2 personas",
        showBudget: Boolean = true,
        showDuration: Boolean = true,
        showLocation: Boolean = true,
        showPeopleCount: Boolean = true,
        existingPlanId: Int? = null
    ) {
        viewModelScope.launch {
            repository.addCustomPlan(
                title = title,
                category = category,
                description = description,
                location = location,
                duration = duration,
                budget = budget,
                tags = tags,
                imageUrl = imageUrl,
                imageResName = imageResName,
                targetFriendId = targetFriendId,
                targetGroupId = targetGroupId,
                targetFriendName = targetFriendName,
                targetGroupName = targetGroupName,
                scope = scope,
                peopleCount = peopleCount,
                showBudget = showBudget,
                showDuration = showDuration,
                showLocation = showLocation,
                showPeopleCount = showPeopleCount,
                existingPlanId = existingPlanId
            )
            _uiState.update {
                it.copy(
                    showCreatePlanModal = false,
                    editingPlan = null,
                    toastMessage = if (existingPlanId != null) "¡Plan '$title' actualizado exitosamente!" else "¡Plan '$title' creado exitosamente!"
                )
            }
        }
    }

    fun setDetailedPlan(plan: Plan?) {
        _uiState.update { it.copy(detailedPlan = plan) }
    }

    fun setShowSavedPlansSheet(show: Boolean) {
        _uiState.update { it.copy(showSavedPlansSheet = show) }
    }

    fun setShowFriendsHubModal(show: Boolean, initialTab: Int = 0) {
        _uiState.update { it.copy(showFriendsHubModal = show, friendsHubInitialTab = initialTab) }
    }

    fun setShowCreatePlanModal(show: Boolean) {
        _uiState.update { it.copy(showCreatePlanModal = show, editingPlan = if (!show) null else it.editingPlan) }
    }

    fun openCreateOrEditPlanModal(planToEdit: Plan? = null) {
        _uiState.update { it.copy(showCreatePlanModal = true, editingPlan = planToEdit) }
    }

    fun deleteAllPlans() {
        viewModelScope.launch {
            repository.deleteAllPlans()
            _uiState.update {
                it.copy(
                    currentIndex = 0,
                    toastMessage = "Se han eliminado todos los planes de la base de datos."
                )
            }
            swipeHistory.clear()
        }
    }

    fun deletePlan(planId: Int) {
        viewModelScope.launch {
            repository.deletePlan(planId)
            _uiState.update { it.copy(toastMessage = "Plan eliminado.") }
        }
    }

    fun restoreDefaultPlans() {
        viewModelScope.launch {
            repository.restoreDefaultPlans()
            _uiState.update {
                it.copy(
                    currentIndex = 0,
                    toastMessage = "Se han restaurado los planes predeterminados."
                )
            }
            swipeHistory.clear()
        }
    }

    fun dismissMatchedBanner() {
        _uiState.update { it.copy(matchedPlan = null) }
    }

    fun dismissRealtimeMatchDialog() {
        _uiState.update { it.copy(realtimeMatchPlan = null) }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}

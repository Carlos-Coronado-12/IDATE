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

data class IDateUiState(
    val plans: List<Plan> = emptyList(),
    val filteredPlans: List<Plan> = emptyList(),
    val selectedCategory: String = "Todos",
    val currentIndex: Int = 0,
    val savedPlans: List<Plan> = emptyList(),
    val matchedPlan: Plan? = null,
    val realtimeMatchPlan: Plan? = null,
    val realtimeMatchLikers: List<String> = emptyList(),
    val realtimeMatchGroupName: String? = null,
    val detailedPlan: Plan? = null,
    val showSavedPlansSheet: Boolean = false,
    val showFriendsHubModal: Boolean = false,
    val showCreatePlanModal: Boolean = false,
    val showPlanManagerSheet: Boolean = false,
    val userProfile: UserProfile? = null,
    val friendsList: List<Friend> = emptyList(),
    val groupsList: List<FriendGroup> = emptyList(),
    val activeContext: ActivePlanningContext = ActivePlanningContext(),
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
        // Start Realtime synchronization for Plans
        repository.startRealtimePlansSync(viewModelScope)

        // Initialize / Load User Profile
        viewModelScope.launch {
            val profile = repository.getOrCreateUserProfile()
            _uiState.update { it.copy(userProfile = profile) }
        }

        // Observe User Profile updates
        viewModelScope.launch {
            repository.getUserProfileFlow().collect { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(userProfile = profile) }
                }
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

        // Observe Groups list
        viewModelScope.launch {
            repository.getAllGroups().collect { groups ->
                _uiState.update { it.copy(groupsList = groups) }
                groups.forEach { group ->
                    FriendsRemoteManager.listenToGroup(group.groupCode, group.name)
                }
            }
        }

        // Observe Plans from Room
        viewModelScope.launch {
            repository.getAllPlans().collect { planList ->
                _uiState.update { current ->
                    val filtered = applyFilter(planList, current.selectedCategory, current.activeContext)
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
                        val plan = _uiState.value.plans.find { it.id == event.planId }
                        notificationHelper.showMatchNotification(
                            plan?.title ?: "Plan en común",
                            event.friendName
                        )
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
                        val plan = _uiState.value.plans.find { it.id == event.planId }
                        notificationHelper.showMatchNotification(
                            plan?.title ?: "Plan grupal",
                            event.groupName
                        )
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
                                toastMessage = "¡A $likersText les gustó '${plan?.title ?: "el plan"}' en ${event.groupName}!"
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

        // Collaborate with Friend if active
        if (myProfile != null && activeCtx.type == ActivePlanningContext.ContextType.FRIEND && activeCtx.friend != null) {
            FriendsRemoteManager.submitFriendVote(
                myUserId = myProfile.userId,
                friendId = activeCtx.friend.id,
                planId = currentPlan.id,
                isLike = isLike
            )
        }

        // Collaborate with Group if active
        if (myProfile != null && activeCtx.type == ActivePlanningContext.ContextType.GROUP && activeCtx.group != null) {
            FriendsRemoteManager.submitGroupVote(
                groupCode = activeCtx.group.groupCode,
                userId = myProfile.userId,
                userName = myProfile.name,
                planId = currentPlan.id,
                isLike = isLike
            )
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
            val filtered = applyFilter(current.plans, category, current.activeContext)
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
            val filtered = applyFilter(current.plans, current.selectedCategory, context)
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
        setActiveContext(ActivePlanningContext())
    }

    private fun applyFilter(
        plans: List<Plan>,
        category: String,
        context: ActivePlanningContext
    ): List<Plan> {
        val categoryFiltered = if (category == "Todos" || category.isBlank()) {
            plans
        } else {
            plans.filter { it.category.equals(category, ignoreCase = true) }
        }

        return when (context.type) {
            ActivePlanningContext.ContextType.GLOBAL -> {
                categoryFiltered.filter { it.scope == PlanScope.GLOBAL }
            }
            ActivePlanningContext.ContextType.FRIEND -> {
                val friendId = context.friend?.id
                categoryFiltered.filter {
                    it.scope == PlanScope.GLOBAL || (it.scope == PlanScope.FRIEND_ONLY && it.targetFriendId == friendId)
                }
            }
            ActivePlanningContext.ContextType.GROUP -> {
                val groupId = context.group?.id
                categoryFiltered.filter {
                    it.scope == PlanScope.GLOBAL || (it.scope == PlanScope.GROUP_ONLY && it.targetGroupId == groupId)
                }
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

    // Friends Actions
    fun addFriendByCode(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un código de amigo válido") }
            return
        }

        val myCode = _uiState.value.userProfile?.friendCode
        if (code.trim().equals(myCode, ignoreCase = true)) {
            _uiState.update { it.copy(errorMessage = "No puedes agregarte a ti mismo como amigo") }
            return
        }

        _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
        FriendsRemoteManager.findFriendByCode(code) { result ->
            _uiState.update { it.copy(isConnecting = false) }
            result.onSuccess { friend ->
                viewModelScope.launch {
                    repository.addFriend(friend)
                    _uiState.value.userProfile?.let { me ->
                        FriendsRemoteManager.listenToFriendPair(me.userId, friend.id, friend.name)
                    }
                    _uiState.update {
                        it.copy(
                            toastMessage = "¡${friend.name} agregado como amigo!",
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Error al buscar amigo") }
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            repository.removeFriend(friendId)
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
        viewModelScope.launch {
            repository.removeGroup(groupId)
            _uiState.update { it.copy(toastMessage = "Has salido del grupo.") }
        }
    }

    fun updateUserProfile(name: String, avatarEmoji: String, bio: String) {
        viewModelScope.launch {
            repository.updateUserProfile(name, avatarEmoji, bio)
            _uiState.update { it.copy(toastMessage = "Perfil actualizado correctamente") }
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
        scope: PlanScope = PlanScope.GLOBAL
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
                scope = scope
            )
            _uiState.update {
                it.copy(
                    showCreatePlanModal = false,
                    toastMessage = "¡Plan '$title' creado exitosamente!"
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

    fun setShowFriendsHubModal(show: Boolean) {
        _uiState.update { it.copy(showFriendsHubModal = show) }
    }

    fun setShowCreatePlanModal(show: Boolean) {
        _uiState.update { it.copy(showCreatePlanModal = show) }
    }

    fun setShowPlanManagerSheet(show: Boolean) {
        _uiState.update { it.copy(showPlanManagerSheet = show) }
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

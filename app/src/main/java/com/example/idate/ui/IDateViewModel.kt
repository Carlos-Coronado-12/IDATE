package com.example.idate.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.idate.data.remote.RealtimeSessionManager
import com.example.idate.data.remote.model.LiveRoom
import com.example.idate.data.remote.model.LiveSessionEvent
import com.example.idate.data.repository.IDateRepository
import com.example.idate.model.Plan
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
    val detailedPlan: Plan? = null,
    val showSavedPlansSheet: Boolean = false,
    val showLiveRoomModal: Boolean = false,
    val showCreatePlanModal: Boolean = false,
    val showPlanManagerSheet: Boolean = false,
    val liveRoom: LiveRoom? = null,
    val isConnectingRoom: Boolean = false,
    val roomErrorMessage: String? = null,
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
        // Start Realtime Multi-Device synchronization for Plans
        repository.startRealtimePlansSync(viewModelScope)

        // Observe plans from Room
        viewModelScope.launch {
            repository.getAllPlans().collect { planList ->
                _uiState.update { current ->
                    val filtered = filterPlans(planList, current.selectedCategory)
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

        // Observe Realtime Room
        viewModelScope.launch {
            RealtimeSessionManager.currentRoom.collect { room ->
                _uiState.update { it.copy(liveRoom = room) }
            }
        }

        // Observe Realtime Events (Matches, Join)
        viewModelScope.launch {
            RealtimeSessionManager.sessionEvents.collect { event ->
                when (event) {
                    is LiveSessionEvent.PartnerJoined -> {
                        triggerHaptic(50)
                        notificationHelper.showPartnerJoinedNotification(
                            event.partnerName,
                            _uiState.value.liveRoom?.roomCode ?: ""
                        )
                        _uiState.update { it.copy(toastMessage = "${event.partnerName} se unió a la sala") }
                    }
                    is LiveSessionEvent.RealtimeMatchFound -> {
                        triggerHaptic(300)
                        val plan = _uiState.value.plans.find { it.id == event.planId }
                        if (plan != null) {
                            notificationHelper.showMatchNotification(
                                plan.title,
                                _uiState.value.liveRoom?.partnerName ?: "Tu pareja"
                            )
                            _uiState.update {
                                it.copy(realtimeMatchPlan = plan)
                            }
                        }
                    }
                    is LiveSessionEvent.PartnerSwiped -> {
                        // Subtle update
                    }
                    is LiveSessionEvent.Error -> {
                        _uiState.update { it.copy(roomErrorMessage = event.message) }
                    }
                    is LiveSessionEvent.SessionEnded -> {
                        _uiState.update { it.copy(toastMessage = "La sesión en vivo ha finalizado") }
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

        // Broadcast to Realtime Room if connected
        if (_uiState.value.liveRoom != null) {
            RealtimeSessionManager.submitVote(currentPlan.id, isLike, currentPlan.title)
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
            val filtered = filterPlans(current.plans, category)
            current.copy(
                selectedCategory = category,
                filteredPlans = filtered,
                currentIndex = 0
            )
        }
        swipeHistory.clear()
    }

    private fun filterPlans(plans: List<Plan>, category: String): List<Plan> {
        return if (category == "Todos" || category.isBlank()) {
            plans
        } else {
            plans.filter { it.category.equals(category, ignoreCase = true) }
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

    fun createLiveRoom(hostName: String) {
        val room = RealtimeSessionManager.createRoom(hostName)
        _uiState.update {
            it.copy(
                liveRoom = room,
                roomErrorMessage = null,
                toastMessage = "Sala creada: ${room.roomCode}"
            )
        }
    }

    fun joinLiveRoom(roomCode: String, guestName: String) {
        if (roomCode.isBlank()) {
            _uiState.update { it.copy(roomErrorMessage = "Por favor ingresa un código de sala válido") }
            return
        }

        val result = RealtimeSessionManager.joinRoom(roomCode, guestName)
        result.onSuccess { room ->
            _uiState.update {
                it.copy(
                    liveRoom = room,
                    roomErrorMessage = null,
                    toastMessage = "¡Te has unido a la sala ${room.roomCode}!"
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(roomErrorMessage = err.message ?: "Error al unirse a la sala") }
        }
    }

    fun leaveLiveRoom() {
        RealtimeSessionManager.leaveRoom()
        _uiState.update { it.copy(liveRoom = null) }
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
        imageResName: String = "plan_legos"
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
                imageResName = imageResName
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

    fun setShowLiveRoomModal(show: Boolean) {
        _uiState.update { it.copy(showLiveRoomModal = show) }
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

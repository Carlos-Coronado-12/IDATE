package com.example.idate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.idate.ui.IDateUiState
import com.example.idate.ui.IDateViewModel
import com.example.idate.ui.components.*
import com.example.idate.ui.screens.CreatePlanModal
import com.example.idate.ui.screens.FriendsHubModal
import com.example.idate.ui.screens.PlanManagerSheet
import com.example.idate.workers.SyncPlansWorker

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic background sync with WorkManager
        SyncPlansWorker.schedulePeriodicSync(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            // Request Notification Permission for Android 13+ (Tiramisu)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission handled
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val viewModel: IDateViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    IDateApp(
                        uiState = uiState,
                        isWideScreen = isWideScreen,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun IDateApp(
    uiState: IDateUiState,
    isWideScreen: Boolean,
    viewModel: IDateViewModel
) {
    val plans = uiState.filteredPlans
    val currentIndex = uiState.currentIndex
    val savedPlans = uiState.savedPlans
    val matchedPlan = uiState.matchedPlan
    val realtimeMatchPlan = uiState.realtimeMatchPlan
    val detailedPlan = uiState.detailedPlan

    val snackbarHostState = remember { SnackbarHostState() }

    // Toast and event observation
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastMessage()
        }
    }

    val currentPlan = plans.getOrNull(currentIndex)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                TopBar(
                    savedPlansCount = savedPlans.size,
                    friendsCount = uiState.friendsList.size,
                    groupsCount = uiState.groupsList.size,
                    activeContext = uiState.activeContext,
                    onOpenSavedPlans = { viewModel.setShowSavedPlansSheet(true) },
                    onOpenFriendsHub = { viewModel.setShowFriendsHubModal(true) },
                    onOpenCreatePlan = { viewModel.setShowCreatePlanModal(true) },
                    onOpenPlanManager = { viewModel.setShowPlanManagerSheet(true) },
                    onResetActiveContext = { viewModel.resetActiveContext() }
                )

                if (currentPlan != null && !isWideScreen) {
                    PlanHeader(plan = currentPlan)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AdaptivePlanLayout(
                isWideScreen = isWideScreen,
                plans = plans,
                currentIndex = currentIndex,
                savedPlans = savedPlans,
                onSwiped = { direction -> viewModel.handleSwipe(direction) },
                onInfoClick = { plan -> viewModel.setDetailedPlan(plan) },
                onResetDeck = { viewModel.resetDeck() },
                onCreateNewPlan = { viewModel.setShowCreatePlanModal(true) },
                onRewind = { viewModel.handleRewind() },
                canRewind = currentIndex > 0
            )

            // Local Match Top Banner
            AnimatedVisibility(
                visible = matchedPlan != null && realtimeMatchPlan == null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                matchedPlan?.let { plan ->
                    MatchTopBanner(
                        plan = plan,
                        onDismiss = { viewModel.dismissMatchedBanner() },
                        onViewDetails = {
                            viewModel.dismissMatchedBanner()
                            viewModel.setDetailedPlan(it)
                        }
                    )
                }
            }
        }
    }

    // Realtime Collaborative Match Dialog (Friend or Group)
    realtimeMatchPlan?.let { plan ->
        RealtimeMatchDialog(
            plan = plan,
            partnerName = uiState.activeContext.displayName,
            groupName = uiState.realtimeMatchGroupName,
            likedUserNames = uiState.realtimeMatchLikers,
            onDismiss = { viewModel.dismissRealtimeMatchDialog() },
            onViewDetails = {
                viewModel.dismissRealtimeMatchDialog()
                viewModel.setDetailedPlan(it)
            }
        )
    }

    // Detail Modal
    detailedPlan?.let { plan ->
        val isSaved = savedPlans.any { it.id == plan.id }
        PlanDetailModal(
            plan = plan,
            onDismiss = { viewModel.setDetailedPlan(null) },
            isSaved = isSaved,
            onSaveToggle = { viewModel.toggleSavePlan(plan) }
        )
    }

    // Saved Plans Bottom Sheet
    if (uiState.showSavedPlansSheet) {
        LikedPlansSheet(
            savedPlans = savedPlans,
            onDismiss = { viewModel.setShowSavedPlansSheet(false) },
            onPlanClick = { plan ->
                viewModel.setShowSavedPlansSheet(false)
                viewModel.setDetailedPlan(plan)
            },
            onRemovePlan = { plan ->
                viewModel.toggleSavePlan(plan)
            }
        )
    }

    // Plan Manager Menu Sheet (Manage, Delete, View all Plans)
    if (uiState.showPlanManagerSheet) {
        PlanManagerSheet(
            plans = uiState.plans,
            onDismiss = { viewModel.setShowPlanManagerSheet(false) },
            onCreateNewPlan = { viewModel.setShowCreatePlanModal(true) },
            onDeletePlan = { planId -> viewModel.deletePlan(planId) },
            onDeleteAllPlans = { viewModel.deleteAllPlans() },
            onRestoreDefaults = { viewModel.restoreDefaultPlans() }
        )
    }

    // Friends and Groups Hub Modal
    if (uiState.showFriendsHubModal) {
        FriendsHubModal(
            userProfile = uiState.userProfile,
            friendsList = uiState.friendsList,
            incomingRequests = uiState.incomingFriendRequests,
            groupsList = uiState.groupsList,
            activeContext = uiState.activeContext,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setShowFriendsHubModal(false) },
            onSendFriendRequest = { code -> viewModel.sendFriendRequestByCode(code) },
            onAcceptFriendRequest = { request -> viewModel.acceptFriendRequest(request) },
            onRejectFriendRequest = { request -> viewModel.rejectFriendRequest(request) },
            onRemoveFriend = { friendId -> viewModel.removeFriend(friendId) },
            onCreateGroup = { name, desc, emoji -> viewModel.createGroup(name, desc, emoji) },
            onJoinGroupByCode = { code -> viewModel.joinGroupByCode(code) },
            onAddFriendToGroup = { groupCode, friend -> viewModel.addFriendToGroup(groupCode, friend) },
            onRemoveGroup = { groupId -> viewModel.removeGroup(groupId) },
            onUpdateProfile = { name, emoji, bio -> viewModel.updateUserProfile(name, emoji, bio) },
            onSelectContext = { context -> viewModel.setActiveContext(context) }
        )
    }

    // Create Custom Plan Modal with Friend/Group targeting
    if (uiState.showCreatePlanModal) {
        CreatePlanModal(
            friendsList = uiState.friendsList,
            groupsList = uiState.groupsList,
            onDismiss = { viewModel.setShowCreatePlanModal(false) },
            onCreatePlan = { title, category, description, location, duration, budget, tags, imageUrl, imageResName, targetFriendId, targetGroupId, targetFriendName, targetGroupName, scope ->
                viewModel.addCustomPlan(
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
            }
        )
    }
}

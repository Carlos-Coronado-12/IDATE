package com.example.idate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.idate.model.Plan
import com.example.idate.model.SamplePlans
import com.example.idate.ui.components.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    IDateApp()
                }
            }
        }
    }
}

@Composable
fun IDateApp() {
    val plans = remember { SamplePlans.defaultPlans }

    var currentIndex by remember { mutableIntStateOf(0) }
    val swipeHistory = remember { mutableStateListOf<Int>() }
    val savedPlans = remember { mutableStateListOf<Plan>() }

    var matchedPlan by remember { mutableStateOf<Plan?>(null) }
    var detailedPlan by remember { mutableStateOf<Plan?>(null) }
    var showSavedPlansSheet by remember { mutableStateOf(false) }

    fun handleSwipe(direction: VerticalSwipeDirection) {
        val currentPlan = plans.getOrNull(currentIndex) ?: return
        swipeHistory.add(currentIndex)

        if (direction == VerticalSwipeDirection.UP) {
            if (!savedPlans.any { it.id == currentPlan.id }) {
                savedPlans.add(currentPlan)
            }
            matchedPlan = currentPlan
        }

        currentIndex++
    }

    fun handleRewind() {
        if (swipeHistory.isNotEmpty()) {
            val lastIndex = swipeHistory.removeAt(swipeHistory.lastIndex)
            currentIndex = lastIndex
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            Column {
                TopBar(
                    savedPlansCount = savedPlans.size,
                    onOpenSavedPlans = { showSavedPlansSheet = true }
                )

                val currentPlan = plans.getOrNull(currentIndex)
                if (currentPlan != null) {
                    PlanHeader(plan = currentPlan)
                }
            }
        },
        bottomBar = {
            val isDeckEmpty = currentIndex >= plans.size
            if (!isDeckEmpty) {
                ActionButtonsBar(
                    onRewind = { handleRewind() },
                    onDislike = { handleSwipe(VerticalSwipeDirection.DOWN) },
                    onLike = { handleSwipe(VerticalSwipeDirection.UP) },
                    canRewind = swipeHistory.isNotEmpty()
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentIndex < plans.size) {
                val nextPlan = plans.getOrNull(currentIndex + 1)
                val currentPlan = plans[currentIndex]

                if (nextPlan != null) {
                    key(nextPlan.id) {
                        TinderCard(
                            plan = nextPlan,
                            onSwiped = {},
                            onInfoClick = { detailedPlan = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                key(currentPlan.id) {
                    TinderCard(
                        plan = currentPlan,
                        onSwiped = { direction ->
                            handleSwipe(direction)
                        },
                        onInfoClick = { detailedPlan = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                EmptyPlansView(
                    onResetDeck = {
                        currentIndex = 0
                        swipeHistory.clear()
                    }
                )
            }

            AnimatedVisibility(
                visible = matchedPlan != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                matchedPlan?.let { plan ->
                    MatchTopBanner(
                        plan = plan,
                        onDismiss = { matchedPlan = null },
                        onViewDetails = {
                            matchedPlan = null
                            detailedPlan = it
                        }
                    )
                }
            }
        }
    }

    detailedPlan?.let { plan ->
        val isSaved = savedPlans.any { it.id == plan.id }
        PlanDetailModal(
            plan = plan,
            onDismiss = { detailedPlan = null },
            isSaved = isSaved,
            onSaveToggle = {
                if (isSaved) {
                    savedPlans.removeAll { it.id == plan.id }
                } else {
                    savedPlans.add(plan)
                }
            }
        )
    }

    if (showSavedPlansSheet) {
        LikedPlansSheet(
            savedPlans = savedPlans,
            onDismiss = { showSavedPlansSheet = false },
            onPlanClick = { plan ->
                showSavedPlansSheet = false
                detailedPlan = plan
            },
            onRemovePlan = { plan ->
                savedPlans.remove(plan)
            }
        )
    }
}

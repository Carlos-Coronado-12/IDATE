package com.example.idate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idate.model.Plan

@Composable
fun AdaptivePlanLayout(
    isWideScreen: Boolean,
    plans: List<Plan>,
    currentIndex: Int,
    savedPlans: List<Plan>,
    onSwiped: (VerticalSwipeDirection) -> Unit,
    onInfoClick: (Plan) -> Unit,
    onResetDeck: () -> Unit,
    onCreateNewPlan: () -> Unit,
    onRewind: () -> Unit,
    canRewind: Boolean,
    modifier: Modifier = Modifier
) {
    if (isWideScreen) {
        // TABLET / WIDE SCREEN SPLIT-VIEW (Master - Detail)
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Panel: Saved & Matched Plans Side Panel
            Card(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "💖 Planes Guardados (${savedPlans.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (savedPlans.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no hay planes guardados.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(savedPlans, key = { it.id }) { plan ->
                                SavedPlanItem(
                                    plan = plan,
                                    onClick = { onInfoClick(plan) },
                                    onDelete = {}
                                )
                            }
                        }
                    }
                }
            }

            // Right Panel: Interactive Deck & Action Buttons
            Card(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    DeckStack(
                        plans = plans,
                        currentIndex = currentIndex,
                        onSwiped = onSwiped,
                        onInfoClick = onInfoClick,
                        onResetDeck = onResetDeck,
                        onCreateNewPlan = onCreateNewPlan
                    )
                }
            }
        }
    } else {
        // COMPACT MOBILE PHONE LAYOUT
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            DeckStack(
                plans = plans,
                currentIndex = currentIndex,
                onSwiped = onSwiped,
                onInfoClick = onInfoClick,
                onResetDeck = onResetDeck,
                onCreateNewPlan = onCreateNewPlan
            )
        }
    }
}

@Composable
fun DeckStack(
    plans: List<Plan>,
    currentIndex: Int,
    onSwiped: (VerticalSwipeDirection) -> Unit,
    onInfoClick: (Plan) -> Unit,
    onResetDeck: () -> Unit,
    onCreateNewPlan: () -> Unit
) {
    if (currentIndex < plans.size) {
        val nextPlan = plans.getOrNull(currentIndex + 1)
        val currentPlan = plans[currentIndex]

        if (nextPlan != null) {
            key(nextPlan.id) {
                TinderCard(
                    plan = nextPlan,
                    onSwiped = {},
                    onInfoClick = onInfoClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        key(currentPlan.id) {
            TinderCard(
                plan = currentPlan,
                onSwiped = onSwiped,
                onInfoClick = onInfoClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        EmptyPlansView(
            hasAnyPlans = plans.isNotEmpty(),
            onResetDeck = onResetDeck,
            onCreateNewPlan = onCreateNewPlan
        )
    }
}


package com.example.idate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.idate.R
import com.example.idate.model.Plan
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class VerticalSwipeDirection {
    UP,     // Aceptar / Me interesa
    DOWN    // Rechazar / Paso
}

@Composable
fun TinderCard(
    plan: Plan,
    onSwiped: (VerticalSwipeDirection) -> Unit,
    onInfoClick: (Plan) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val swipeThreshold = screenHeightPx * 0.20f

    val rotation = (offsetX.value / 60f).coerceIn(-10f, 10f)

    val acceptAlpha = (-offsetY.value / 150f).coerceIn(0f, 1f)  // Dragging UP = Aceptar
    val rejectAlpha = (offsetY.value / 150f).coerceIn(0f, 1f)   // Dragging DOWN = Rechazar

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(rotation)
            .shadow(12.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(2.dp, Color.Red.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
            .background(Color.Black)
            .pointerInput(plan.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                            offsetX.snapTo(offsetX.value + dragAmount.x * 0.3f)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value < -swipeThreshold) {
                                offsetY.animateTo(-screenHeightPx * 1.2f, tween(250))
                                onSwiped(VerticalSwipeDirection.UP)
                            } else if (offsetY.value > swipeThreshold) {
                                offsetY.animateTo(screenHeightPx * 1.2f, tween(250))
                                onSwiped(VerticalSwipeDirection.DOWN)
                            } else {
                                offsetY.animateTo(0f, tween(200))
                                offsetX.animateTo(0f, tween(200))
                            }
                        }
                    }
                )
            }
    ) {
        // Plan Background Image (Instant local bundled asset or custom web URL)
        AsyncImage(
            model = if (plan.imageUrl.isNotBlank()) plan.imageUrl else if (plan.imageResId != 0) plan.imageResId else R.drawable.plan_legos,
            contentDescription = plan.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Shadow Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Top Activity Badge with Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = plan.icon,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = plan.category,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                color = Color.Red.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${plan.iconEmoji} ${plan.detail}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // SWIPE UP STAMP: ¡ME INTERESA!
        if (acceptAlpha > 0.05f) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFF00E676)),
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "¡ME INTERESA!",
                        color = Color(0xFF00E676),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // SWIPE DOWN STAMP: PASO
        if (rejectAlpha > 0.05f) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFFFF1744)),
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFFFF1744),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "PASO",
                        color = Color(0xFFFF1744),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Card Info Bottom Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = plan.iconEmoji, fontSize = 22.sp)
                Text(
                    text = plan.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = plan.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plan.location.isNotEmpty()) InfoBadge(icon = Icons.Default.LocationOn, text = plan.location)
                if (plan.duration.isNotEmpty()) InfoBadge(icon = Icons.Default.AccessTime, text = plan.duration)
                if (plan.budget.isNotEmpty()) InfoBadge(icon = Icons.Default.AttachMoney, text = plan.budget)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    plan.tags.take(3).forEach { tag ->
                        Text(
                            text = "#$tag",
                            color = Color(0xFFFF80AB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = { onInfoClick(plan) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Detalles",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFFD54F),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.example.idate.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.idate.R
import com.example.idate.model.Plan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailModal(
    plan: Plan,
    onDismiss: () -> Unit,
    onSaveToggle: (Plan) -> Unit,
    isSaved: Boolean,
    onEditPlan: (Plan) -> Unit = {}
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                PlanImage(
                    plan = plan,
                    contentDescription = plan.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top Floating Action Buttons (Close & Share)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onEditPlan(plan)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Plan",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("¡Oye! Encontré este plan genial en IDATE:\n")
                                    appendLine("${plan.iconEmoji} *${plan.title}*")
                                    if (plan.showPeopleCount && plan.peopleCount.isNotBlank()) appendLine("👥 ${plan.peopleCount}")
                                    if (plan.showLocation && plan.location.isNotBlank()) appendLine("📍 ${plan.location}")
                                    if (plan.showDuration && plan.duration.isNotBlank()) appendLine("⏱️ ${plan.duration}")
                                    if (plan.showBudget && plan.budget.isNotBlank()) appendLine("💰 ${plan.budget}")
                                    appendLine("\n¿Te animas a ir?")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Invitar a un amigo/pareja")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir Plan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Plan Body Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Category & Icon Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = plan.icon,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = plan.category,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFF0F0F0),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${plan.iconEmoji} ${plan.detail}",
                            color = Color(0xFF424242),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = plan.iconEmoji, fontSize = 24.sp)
                    Text(
                        text = plan.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Cards Row (Dynamic based on toggles)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (plan.showPeopleCount && plan.peopleCount.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Default.Group,
                            title = "Personas",
                            subtitle = plan.peopleCount,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                    if (plan.showLocation && plan.location.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Default.LocationOn,
                            title = "Ubicación",
                            subtitle = plan.location,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                    if (plan.showDuration && plan.duration.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Default.AccessTime,
                            title = "Duración",
                            subtitle = plan.duration,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                    if (plan.showBudget && plan.budget.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Default.AttachMoney,
                            title = "Presupuesto",
                            subtitle = plan.budget,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description Header & Text
                Text(
                    text = "¿En qué consiste este plan?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.description,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF424242)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tags List
                Text(
                    text = "Etiquetas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    plan.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF3E5F5)
                        ) {
                            Text(
                                text = "#$tag",
                                color = Color(0xFF8E24AA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Save or Unsave Button
                Button(
                    onClick = { onSaveToggle(plan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color(0xFFEEEEEE) else Color(0xFFE91E63)
                    )
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isSaved) Color(0xFFE91E63) else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSaved) "Plan Guardado ❤️" else "Guardar este Plan",
                        color = if (isSaved) Color(0xFFE91E63) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F9FA),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

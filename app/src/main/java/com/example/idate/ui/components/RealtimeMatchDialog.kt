package com.example.idate.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.idate.R
import com.example.idate.model.Plan

@Composable
fun RealtimeMatchDialog(
    plan: Plan,
    partnerName: String,
    groupName: String? = null,
    likedUserNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onViewDetails: (Plan) -> Unit
) {
    val isGroupMatch = !groupName.isNullOrBlank() || likedUserNames.size >= 2

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "matchScale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .semantics {
                    contentDescription = if (isGroupMatch) {
                        "¡Match de Grupo en $groupName para el plan ${plan.title}!"
                    } else {
                        "¡Coincidencia en tiempo real con $partnerName para el plan ${plan.title}!"
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Icon Badge
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGroupMatch) {
                                Brush.linearGradient(
                                    listOf(Color(0xFF6200EE), Color(0xFF7C4DFF), Color(0xFFB388FF))
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF1744), Color(0xFFE91E63), Color(0xFFFF4081))
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGroupMatch) Icons.Default.Groups else Icons.Default.Favorite,
                        contentDescription = "Icono Match",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isGroupMatch) "¡MATCH DE GRUPO! 🎉" else "¡IT'S A MATCH! ❤️",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = if (isGroupMatch) Color(0xFF6200EE) else Color(0xFFE91E63)
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (isGroupMatch) {
                    val namesText = if (likedUserNames.isNotEmpty()) {
                        likedUserNames.joinToString(", ")
                    } else {
                        "2 o más personas"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "¡A $namesText les gustó este plan!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        if (!groupName.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFEDE7F6),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Grupo: $groupName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4A148C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "¡A ti y a $partnerName les encantó la misma idea de cita al mismo tiempo!",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Plan preview
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGroupMatch) Color(0xFFF3E5F5) else Color(0xFFFFF0F5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlanImage(
                            plan = plan,
                            contentDescription = plan.title,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.category.uppercase(),
                                color = if (isGroupMatch) Color(0xFF6200EE) else Color(0xFFE91E63),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = plan.title,
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = plan.detail,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onViewDetails(plan)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGroupMatch) Color(0xFF6200EE) else Color(0xFFE91E63)
                    )
                ) {
                    Text("Ver Detalles del Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seguir Deslizando", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

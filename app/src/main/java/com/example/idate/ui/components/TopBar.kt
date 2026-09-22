package com.example.idate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idate.R
import com.example.idate.data.remote.model.LiveRoom

@Composable
fun TopBar(
    savedPlansCount: Int,
    liveRoom: LiveRoom?,
    onOpenSavedPlans: () -> Unit,
    onOpenLiveRoom: () -> Unit,
    onOpenCreatePlan: () -> Unit,
    onOpenPlanManager: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // 1. LEFT: Sala en Vivo / Radar Button (Always visible)
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                IconButton(
                    onClick = onOpenLiveRoom,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (liveRoom != null) Color(0xFFE8F5E9) else Color(0xFFE0F2F1))
                        .border(
                            1.5.dp,
                            if (liveRoom != null) Color(0xFF00E676) else Color(0xFF00897B).copy(alpha = 0.5f),
                            CircleShape
                        )
                        .semantics {
                            contentDescription = if (liveRoom != null)
                                "Sala en vivo activa: código ${liveRoom.roomCode}. Toca para abrir detalles."
                            else
                                "Abrir Sala en Vivo y conectar dos dispositivos."
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Sala en Vivo",
                        tint = if (liveRoom != null) Color(0xFF00C853) else Color(0xFF00897B),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Live Active Badge Indicator
                if (liveRoom != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                            .border(1.5.dp, Color.White, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            // 2. CENTER: Main Application Heart Icon
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { onOpenLiveRoom() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_idate_logo),
                    contentDescription = "Icono Principal IDATE",
                    modifier = Modifier.height(44.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 3. RIGHT: Cascading Action Dropdown Menu
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8F9FA))
                        .border(1.5.dp, Color(0xFFE0E0E0), CircleShape)
                        .semantics {
                            contentDescription = "Menú de opciones en cascada"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menú",
                        tint = Color(0xFF212121),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Cascading Vertical Dropdown Menu
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    // Item 1: Mis Planes Guardados
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF0F5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Planes Guardados",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                        if (savedPlansCount > 0) {
                                            Surface(
                                                color = Color(0xFFE91E63),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "$savedPlansCount",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Ver citas que te interesaron",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenSavedPlans()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFFF0F0F0))

                    // Item 2: Lápiz (Gestionar Planes)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE1F5FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF0288D1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Gestionar Planes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Ver catálogo y opciones",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenPlanManager()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFFF0F0F0))

                    // Item 3: Signo '+' (Crear Nuevo Plan)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Nuevo Plan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Crear cita personalizada",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenCreatePlan()
                        }
                    )
                }
            }
        }

        // Live Room Banner when connected
        AnimatedVisibility(visible = liveRoom != null) {
            liveRoom?.let { room ->
                Surface(
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLiveRoom() }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853))
                            )
                            Text(
                                text = "Sala: ${room.roomCode} • ${room.partnerName ?: "Esperando pareja..."}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "${room.matchedPlanIds.size} matches",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

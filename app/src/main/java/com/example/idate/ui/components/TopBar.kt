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
import com.example.idate.model.ActivePlanningContext

@Composable
fun TopBar(
    savedPlansCount: Int,
    friendsCount: Int,
    groupsCount: Int,
    activeContext: ActivePlanningContext,
    onOpenSavedPlans: () -> Unit,
    onOpenFriendsHub: () -> Unit,
    onOpenCreatePlan: () -> Unit,
    onOpenPlanManager: () -> Unit,
    onResetActiveContext: () -> Unit
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
            // 1. LEFT: Friends and Groups Hub Button
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                IconButton(
                    onClick = onOpenFriendsHub,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeContext.type != ActivePlanningContext.ContextType.GLOBAL)
                                Color(0xFFE8F5E9)
                            else
                                Color(0xFFF1F8E9)
                        )
                        .border(
                            1.5.dp,
                            if (activeContext.type != ActivePlanningContext.ContextType.GLOBAL)
                                Color(0xFF2E7D32)
                            else
                                Color(0xFF81C784).copy(alpha = 0.6f),
                            CircleShape
                        )
                        .semantics {
                            contentDescription = "Abrir Hub de Amigos y Grupos"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Amigos y Grupos",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Badge Indicator if user has friends or active context
                if (friendsCount > 0 || groupsCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C853))
                            .border(1.5.dp, Color.White, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            // 2. CENTER: Main Application Heart Icon
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { onOpenFriendsHub() }
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

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    // Item 0: Amigos y Grupos
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
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Amigos y Grupos",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                        if (friendsCount > 0) {
                                            Surface(
                                                color = Color(0xFF2E7D32),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "$friendsCount",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Conexiones y planes compartidos",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenFriendsHub()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFFF0F0F0))

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

        // Active Targeted Planning Banner (When swiping specifically for a Friend or Group)
        AnimatedVisibility(visible = activeContext.type != ActivePlanningContext.ContextType.GLOBAL) {
            Surface(
                color = if (activeContext.type == ActivePlanningContext.ContextType.GROUP) Color(0xFFEDE7F6) else Color(0xFFE8F5E9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(activeContext.emoji, fontSize = 14.sp)
                        Text(
                            text = activeContext.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeContext.type == ActivePlanningContext.ContextType.GROUP) Color(0xFF4A148C) else Color(0xFF1B5E20)
                        )
                    }
                    IconButton(
                        onClick = onResetActiveContext,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Volver a planes globales",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

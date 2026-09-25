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
    onResetActiveContext: () -> Unit,
    onReturnToMainMenu: (() -> Unit)? = null
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
            // 1. LEFT: Return to Main Menu or Friends and Groups Hub Button
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onReturnToMainMenu != null) {
                    IconButton(
                        onClick = onReturnToMainMenu,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F3F9))
                            .border(1.5.dp, Color(0xFFE2E8F0), CircleShape)
                            .semantics {
                                contentDescription = "Regresar al Menú Principal"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Menú Principal",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = onOpenFriendsHub,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                            .border(
                                1.5.dp,
                                Color(0xFF2E7D32),
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
                    if (onReturnToMainMenu != null) {
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
                                            .background(Color(0xFFEDE7F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = Color(0xFF673AB7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = "Menú Principal",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "Amigos, Grupos y Barajas",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onReturnToMainMenu()
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFFF0F0F0))
                    }

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

                    // Item 2: Signo '+' (Crear Nuevo Plan)
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

        // Active Targeted Planning Banner (Showing active Deck, Friend, or Group)
        AnimatedVisibility(visible = true) {
            val bannerBg = when (activeContext.type) {
                ActivePlanningContext.ContextType.GROUP -> Color(0xFFEDE7F6)
                ActivePlanningContext.ContextType.DECK -> Color(0xFFFFF0F5)
                else -> Color(0xFFE8F5E9)
            }
            val textColor = when (activeContext.type) {
                ActivePlanningContext.ContextType.GROUP -> Color(0xFF4A148C)
                ActivePlanningContext.ContextType.DECK -> Color(0xFFC2185B)
                else -> Color(0xFF1B5E20)
            }

            Surface(
                color = bannerBg,
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
                            color = textColor
                        )
                    }
                    IconButton(
                        onClick = onResetActiveContext,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cambiar contexto",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

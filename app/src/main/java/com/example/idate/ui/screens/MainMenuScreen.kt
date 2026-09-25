package com.example.idate.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idate.R
import com.example.idate.model.DateDeck
import com.example.idate.model.Friend
import com.example.idate.model.FriendGroup
import com.example.idate.model.UserProfile

@Composable
fun MainMenuScreen(
    userProfile: UserProfile?,
    friendsList: List<Friend>,
    groupsList: List<FriendGroup>,
    customDecks: List<DateDeck>,
    groupDecksMap: Map<String, List<DateDeck>> = emptyMap(),
    totalPlansCount: Int,
    savedPlansCount: Int,
    incomingRequestsCount: Int,
    incomingDeckInvitations: List<com.example.idate.data.remote.FriendEvent.DeckInvitationReceived> = emptyList(),
    onAcceptDeckInvitation: (com.example.idate.data.remote.FriendEvent.DeckInvitationReceived, Boolean) -> Unit = { _, _ -> },
    onDismissDeckInvitation: (com.example.idate.data.remote.FriendEvent.DeckInvitationReceived) -> Unit = {},
    onStartFriendSession: (Friend) -> Unit,
    onStartGroupSession: (FriendGroup) -> Unit,
    onStartDeckSession: (DateDeck) -> Unit,
    onOpenCreateDeckModal: () -> Unit,
    onOpenEditDeckModal: (DateDeck) -> Unit,
    onOpenShareDeckModal: (DateDeck) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onOpenFriendsHub: () -> Unit,
    onOpenGroupsHub: () -> Unit = {},
    onOpenSavedPlans: () -> Unit,
    onUpdateProfile: (name: String, avatarEmoji: String, bio: String) -> Unit
) {
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 0. Invitaciones de Barajas Recibidas
        if (incomingDeckInvitations.isNotEmpty()) {
            items(incomingDeckInvitations, key = { it.inviteId }) { invite ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF4B72)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(invite.iconEmoji, fontSize = 28.sp)
                                Column {
                                    Text(
                                        text = "¡Baraja Compartida!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF4B72)
                                    )
                                    Text(
                                        text = invite.deckName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF111827)
                                    )
                                }
                            }
                            IconButton(onClick = { onDismissDeckInvitation(invite) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Descartar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${invite.fromFriendName} te compartió esta baraja con ${invite.plans.size} planes exclusivos para jugar juntos.",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAcceptDeckInvitation(invite, false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                            ) {
                                Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onAcceptDeckInvitation(invite, true) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B72))
                            ) {
                                Text("Aceptar y Jugar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 1. Top Header & User Profile
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_idate_logo),
                            contentDescription = "IDATE Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "IDATE",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF111827),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Encuentra la cita ideal",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Top Right Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Saved Plans Button
                        IconButton(
                            onClick = onOpenSavedPlans,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (savedPlansCount > 0) {
                                        Badge(containerColor = Color(0xFFFF4B72)) {
                                            Text("$savedPlansCount", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Guardados", tint = Color(0xFFFF4B72), modifier = Modifier.size(20.dp))
                            }
                        }

                        // Friends & Groups Hub Button
                        IconButton(
                            onClick = onOpenFriendsHub,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, Color(0xFFC8E6C9), CircleShape)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (incomingRequestsCount > 0) {
                                        Badge(containerColor = Color(0xFFEF4444)) {
                                            Text("$incomingRequestsCount", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.People, contentDescription = "Amigos y Grupos", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // User Profile Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFE8F5E9), CircleShape)
                                    .border(1.5.dp, Color(0xFF2E7D32), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile?.avatarEmoji ?: "👤",
                                    fontSize = 24.sp
                                )
                            }
                            Column {
                                Text(
                                    text = userProfile?.name ?: "Usuario IDATE",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable {
                                        userProfile?.friendCode?.let { code ->
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("IDATE Friend Code", code))
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Código: ${userProfile?.friendCode ?: "..."}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copiar Código",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF3F4F6), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Perfil", tint = Color(0xFF374151), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 2. Section: "Mis Amigos"
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "👥 Mis Amigos",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        if (friendsList.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${friendsList.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    TextButton(onClick = onOpenFriendsHub) {
                        Text("Gestionar", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (friendsList.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFriendsHub() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🤝", fontSize = 28.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¡Agrega a tus amigos!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Comparte tu código o ingresa el de un amigo para votar planes juntos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF2E7D32))
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(friendsList, key = { it.id }) { friend ->
                            Surface(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { onStartFriendSession(friend) },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                shadowElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFF3F4F6), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = friend.avatarEmoji, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = friend.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                            Text("Votar", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Section: "Mis Grupos" (con Baraja Grupal)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎉 Mis Grupos",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        if (groupsList.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFEDE7F6),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${groupsList.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8A2BE2),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    TextButton(onClick = onOpenGroupsHub) {
                        Text("Crear / Unirse", fontSize = 12.sp, color = Color(0xFF8A2BE2), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (groupsList.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGroupsHub() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🎪", fontSize = 28.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¡Crea un grupo con tus amigos!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Importen sus barajas a la Baraja Grupal y voten en equipo con matches en tiempo real.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF8A2BE2))
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupsList, key = { it.id }) { group ->
                            val importedDecks = groupDecksMap[group.groupCode] ?: emptyList()
                            val totalGroupPlansCount = importedDecks.flatMap { it.planIds }.distinct().size

                            Surface(
                                modifier = Modifier
                                    .width(170.dp)
                                    .clickable { onStartGroupSession(group) },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                shadowElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = group.iconEmoji, fontSize = 24.sp)
                                        Surface(
                                            color = Color(0xFFEDE7F6),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = group.groupCode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8A2BE2),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = group.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Baraja Grupal Badge
                                    Surface(
                                        color = if (totalGroupPlansCount > 0) Color(0xFFE8F5E9) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (totalGroupPlansCount > 0)
                                                "🃏 Baraja Grupal: $totalGroupPlansCount planes"
                                            else
                                                "🃏 Sin barajas importadas",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (totalGroupPlansCount > 0) Color(0xFF2E7D32) else Color(0xFF6B7280),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { onStartGroupSession(group) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Votar en Grupo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Section: "Mis Barajas de Dates"
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🃏 Mis Barajas de Dates",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Surface(
                            color = Color(0xFFFF4B72).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${customDecks.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF4B72),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onOpenCreateDeckModal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B72)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crear Baraja", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (customDecks.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCreateDeckModal() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🃏", fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Aún no tienes barajas de dates",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Crea una baraja nueva vacía, agrégale tus planes favoritos y guárdala para jugar con amigos o importarla a tus grupos.",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenCreateDeckModal,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B72)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Crear mi Primera Baraja", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        customDecks.forEach { deck ->
                            DeckCardItemLight(
                                deck = deck,
                                onPlayDeck = { onStartDeckSession(deck) },
                                onShareDeck = { onOpenShareDeckModal(deck) },
                                onEditDeck = { onOpenEditDeckModal(deck) },
                                onDeleteDeck = { onDeleteDeck(deck.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Profile Edit Dialog
    if (showEditProfileDialog) {
        EditProfileModal(
            currentProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, emoji, bio ->
                onUpdateProfile(name, emoji, bio)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun DeckCardItemLight(
    deck: DateDeck,
    onPlayDeck: () -> Unit,
    onShareDeck: () -> Unit,
    onEditDeck: () -> Unit,
    onDeleteDeck: () -> Unit
) {
    val deckColor = remember(deck.colorHex) {
        Color(deck.colorHex)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Deck Top Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(deckColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, deckColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = deck.iconEmoji, fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deck.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (deck.description.isNotBlank()) {
                            Text(
                                text = deck.description,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    color = deckColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${deck.planIds.size} planes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = deckColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (deck.isImported) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "📥 Baraja Compartida / Importada",
                        fontSize = 10.sp,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Deck Button
                Button(
                    onClick = onPlayDeck,
                    colors = ButtonDefaults.buttonColors(containerColor = deckColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jugar Baraja", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share / Invite / Import
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { onShareDeck() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir / Invitar", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                }

                // Edit Deck
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { onEditDeck() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                }

                // Delete Deck
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE2E2))
                        .clickable { onDeleteDeck() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EditProfileModal(
    currentProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarEmoji: String, bio: String) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile?.name ?: "") }
    var avatarEmoji by remember { mutableStateOf(currentProfile?.avatarEmoji ?: "🦊") }
    var bio by remember { mutableStateOf(currentProfile?.bio ?: "") }

    val emojiOptions = listOf("🦊", "🦁", "🐼", "🐨", "🐯", "🦄", "🐶", "🐱", "🐰", "🐧", "🚀", "✨", "🍕", "🎸")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Editar Mi Perfil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                // Emoji Picker
                Text(text = "Elige tu Avatar Emoji", fontSize = 12.sp, color = Color(0xFF6B7280))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiOptions.forEach { emoji ->
                        val isSelected = avatarEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF3F4F6))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE5E7EB),
                                    shape = CircleShape
                                )
                                .clickable { avatarEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name, avatarEmoji, bio) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

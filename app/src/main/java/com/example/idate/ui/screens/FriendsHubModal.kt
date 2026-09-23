package com.example.idate.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.idate.model.ActivePlanningContext
import com.example.idate.model.Friend
import com.example.idate.model.FriendGroup
import com.example.idate.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsHubModal(
    userProfile: UserProfile?,
    friendsList: List<Friend>,
    groupsList: List<FriendGroup>,
    activeContext: ActivePlanningContext,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAddFriendByCode: (code: String) -> Unit,
    onRemoveFriend: (friendId: String) -> Unit,
    onCreateGroup: (name: String, description: String, iconEmoji: String) -> Unit,
    onJoinGroupByCode: (code: String) -> Unit,
    onRemoveGroup: (groupId: String) -> Unit,
    onUpdateProfile: (name: String, avatarEmoji: String, bio: String) -> Unit,
    onSelectContext: (ActivePlanningContext) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Amigos, 1: Grupos, 2: Mi Perfil

    // Sub-states
    var friendCodeInput by remember { mutableStateOf("") }
    var groupCodeInput by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }
    var newGroupEmoji by remember { mutableStateOf("🎉") }

    // Profile editing states
    var profileNameInput by remember(userProfile) { mutableStateOf(userProfile?.name ?: "") }
    var profileBioInput by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var profileEmojiInput by remember(userProfile) { mutableStateOf(userProfile?.avatarEmoji ?: "😎") }
    var copyCodeSuccess by remember { mutableStateOf(false) }

    val emojis = listOf("😎", "🔥", "🚀", "🍕", "🎉", "🍹", "❤️", "✨", "🌟", "🎬", "🎨", "🎮")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Comunidad y Amigos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Conexiones permanentes",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF5F5F5),
                    contentColor = Color(0xFF2E7D32),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .height(44.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Amigos (${friendsList.size})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Grupos (${groupsList.size})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Mi Perfil",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 0: AMIGOS
                if (selectedTab == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Add Friend Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = friendCodeInput,
                                onValueChange = { if (it.length <= 10) friendCodeInput = it.uppercase() },
                                label = { Text("Código de Amigo") },
                                placeholder = { Text("Ej: ID-ABCDE") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF2E7D32))
                                }
                            )

                            Button(
                                onClick = {
                                    if (friendCodeInput.isNotBlank()) {
                                        onAddFriendByCode(friendCodeInput)
                                        friendCodeInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Añadir", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Planning context reset button if in friend context
                        if (activeContext.type == ActivePlanningContext.ContextType.FRIEND) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectContext(ActivePlanningContext()) }
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(activeContext.emoji, fontSize = 18.sp)
                                        Column {
                                            Text(
                                                "Deslizando con ${activeContext.friend?.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF1B5E20)
                                            )
                                            Text(
                                                "Toca para volver a ver Todos los Planes",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF2E7D32))
                                }
                            }
                        }

                        if (friendsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👋", fontSize = 42.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Aún no tienes amigos agregados",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        "Comparte tu código desde la pestaña 'Mi Perfil' o añade amigos con su código.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(friendsList, key = { it.id }) { friend ->
                                    val isSelected = activeContext.type == ActivePlanningContext.ContextType.FRIEND &&
                                            activeContext.friend?.id == friend.id

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF9F9F9)
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2E7D32)) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                        .border(1.dp, Color(0xFFE0E0E0), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(friend.avatarEmoji, fontSize = 20.sp)
                                                }

                                                Column {
                                                    Text(
                                                        friend.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        "Código: ${friend.friendCode} • ${friend.mutualMatchesCount} matches",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        onSelectContext(
                                                            ActivePlanningContext(
                                                                type = ActivePlanningContext.ContextType.FRIEND,
                                                                friend = friend
                                                            )
                                                        )
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE8F5E9),
                                                        contentColor = if (isSelected) Color.White else Color(0xFF1B5E20)
                                                    )
                                                ) {
                                                    Icon(
                                                        Icons.Default.Swipe,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isSelected) "Activo" else "Planes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(
                                                    onClick = { onRemoveFriend(friend.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "Eliminar amigo",
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: GRUPOS
                else if (selectedTab == 1) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = groupCodeInput,
                                onValueChange = { if (it.length <= 10) groupCodeInput = it.uppercase() },
                                label = { Text("Código de Grupo") },
                                placeholder = { Text("Ej: GRP-XXXX") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color(0xFF6200EE))
                                }
                            )

                            Button(
                                onClick = {
                                    if (groupCodeInput.isNotBlank()) {
                                        onJoinGroupByCode(groupCodeInput)
                                        groupCodeInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Unirme", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showCreateGroupDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear Nuevo Grupo", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (groupsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👥", fontSize = 42.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No estás en ningún grupo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        "Crea un grupo para votar planes con tus amigos y encontrar citas que todos quieran.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(groupsList, key = { it.id }) { group ->
                                    val isSelected = activeContext.type == ActivePlanningContext.ContextType.GROUP &&
                                            activeContext.group?.id == group.id

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFFEDE7F6) else Color(0xFFF9F9F9)
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6200EE)) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                        .border(1.dp, Color(0xFFE0E0E0), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(group.iconEmoji, fontSize = 20.sp)
                                                }

                                                Column {
                                                    Text(
                                                        group.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        "PIN: ${group.groupCode} • ${group.memberCount} miembros",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        onSelectContext(
                                                            ActivePlanningContext(
                                                                type = ActivePlanningContext.ContextType.GROUP,
                                                                group = group
                                                            )
                                                        )
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFEDE7F6),
                                                        contentColor = if (isSelected) Color.White else Color(0xFF4A148C)
                                                    )
                                                ) {
                                                    Icon(
                                                        Icons.Default.HowToVote,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isSelected) "Activo" else "Votar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(
                                                    onClick = { onRemoveGroup(group.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ExitToApp,
                                                        contentDescription = "Salir de grupo",
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: MI PERFIL / CÓDIGO
                else if (selectedTab == 2) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            // Friend Code Card with Copy
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF1F8E9))
                                    .border(1.5.dp, Color(0xFF7CB342), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "TU CÓDIGO DE AMIGO PERMANENTE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = Color(0xFF33691E)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = userProfile?.friendCode ?: "ID-12345",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = {
                                            val clipboard =
                                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText(
                                                "Código Amigo IDATE",
                                                userProfile?.friendCode ?: ""
                                            )
                                            clipboard.setPrimaryClip(clip)
                                            copyCodeSuccess = true
                                        },
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copyCodeSuccess) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = Color(0xFF33691E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (copyCodeSuccess) "¡Código Copiado!" else "Copiar para compartir",
                                            fontSize = 12.sp,
                                            color = Color(0xFF33691E),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Avatar Emoji Selector
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Elige tu Avatar Emoji:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    emojis.take(6).forEach { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (profileEmojiInput == emoji) Color(0xFFC8E6C9) else Color(0xFFF0F0F0))
                                                .border(
                                                    if (profileEmojiInput == emoji) 2.dp else 0.dp,
                                                    Color(0xFF2E7D32),
                                                    CircleShape
                                                )
                                                .clickable { profileEmojiInput = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    emojis.drop(6).take(6).forEach { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (profileEmojiInput == emoji) Color(0xFFC8E6C9) else Color(0xFFF0F0F0))
                                                .border(
                                                    if (profileEmojiInput == emoji) 2.dp else 0.dp,
                                                    Color(0xFF2E7D32),
                                                    CircleShape
                                                )
                                                .clickable { profileEmojiInput = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = profileNameInput,
                                onValueChange = { profileNameInput = it },
                                label = { Text("Tu Nombre / Apodo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = profileBioInput,
                                onValueChange = { profileBioInput = it },
                                label = { Text("Estado o Biografía") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    onUpdateProfile(profileNameInput, profileEmojiInput, profileBioInput)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar Perfil", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Crear Grupo
    if (showCreateGroupDialog) {
        Dialog(onDismissRequest = { showCreateGroupDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Crear Grupo de Amigos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nombre del Grupo") },
                        placeholder = { Text("Ej: Salidas Universitarias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newGroupDesc,
                        onValueChange = { newGroupDesc = it },
                        label = { Text("Descripción (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("🎉", "🍻", "🎬", "⛰️", "🎮", "🍔").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (newGroupEmoji == emoji) Color(0xFFEDE7F6) else Color(0xFFF5F5F5))
                                    .border(
                                        if (newGroupEmoji == emoji) 2.dp else 0.dp,
                                        Color(0xFF6200EE),
                                        CircleShape
                                    )
                                    .clickable { newGroupEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateGroupDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateGroup(newGroupName, newGroupDesc, newGroupEmoji)
                                    showCreateGroupDialog = false
                                    newGroupName = ""
                                    newGroupDesc = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text("Crear", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

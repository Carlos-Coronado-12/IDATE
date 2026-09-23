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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.example.idate.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsHubModal(
    userProfile: UserProfile?,
    friendsList: List<Friend>,
    incomingRequests: List<FriendRequest>,
    groupsList: List<FriendGroup>,
    activeContext: ActivePlanningContext,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSendFriendRequest: (code: String) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onRemoveFriend: (friendId: String) -> Unit,
    onCreateGroup: (name: String, description: String, iconEmoji: String) -> Unit,
    onJoinGroupByCode: (code: String) -> Unit,
    onAddFriendToGroup: (groupCode: String, friend: Friend) -> Unit,
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
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var groupToInviteFriends by remember { mutableStateOf<FriendGroup?>(null) }
    var friendToDelete by remember { mutableStateOf<Friend?>(null) }

    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }
    var newGroupEmoji by remember { mutableStateOf("🎉") }

    // Profile editing states
    var isEditingProfile by remember { mutableStateOf(false) }
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
                .fillMaxHeight(0.90f)
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header del Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userProfile?.avatarEmoji ?: "👥", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "Comunidad y Amigos",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Planifica citas y salidas juntos",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de Pestañas
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F8E9),
                    contentColor = Color(0xFF2E7D32),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Amigos (${friendsList.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                if (incomingRequests.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${incomingRequests.size}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Grupos (${groupsList.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Mi Perfil",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // ==========================================
                // TAB 0: AMIGOS Y SOLICITUDES
                // ==========================================
                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Input para enviar solicitud de amistad
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF7)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCEDC8)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Enviar Solicitud de Amistad:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = friendCodeInput,
                                            onValueChange = { friendCodeInput = it.uppercase() },
                                            placeholder = { Text("Código (ej. ID-XXXXX)", fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (friendCodeInput.isNotBlank()) {
                                                    onSendFriendRequest(friendCodeInput.trim())
                                                    friendCodeInput = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            modifier = Modifier.height(54.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = "Enviar Solicitud", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Enviar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Solicitudes de Amistad Pendientes
                        if (incomingRequests.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text("📬", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Solicitudes Pendientes (${incomingRequests.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            items(incomingRequests) { request ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(request.fromUserAvatar, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                request.fromUserName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                "Código: ${request.fromUserCode}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        IconButton(
                                            onClick = { onAcceptFriendRequest(request) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2E7D32))
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Aceptar",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        IconButton(
                                            onClick = { onRejectFriendRequest(request) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE0E0E0))
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Rechazar",
                                                tint = Color.DarkGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Lista de Amigos
                        if (friendsList.isEmpty() && incomingRequests.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("👋", fontSize = 38.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Aún no tienes amigos agregados",
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Comparte tu código de amigo (en 'Mi Perfil') o envía una solicitud ingresando el código de alguien.",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else if (friendsList.isNotEmpty()) {
                            item {
                                Text(
                                    "Mis Amigos:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(friendsList) { friend ->
                                val isSelected =
                                    activeContext.type == ActivePlanningContext.ContextType.FRIEND && activeContext.friend?.id == friend.id
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF9F9F9)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        Color(0xFF2E7D32)
                                    ) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDCEDC8)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(friend.avatarEmoji, fontSize = 22.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                friend.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                "Cód: ${friend.friendCode}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        // Botón para seleccionar como contexto de planificación
                                        IconButton(
                                            onClick = {
                                                onSelectContext(
                                                    ActivePlanningContext(
                                                        type = ActivePlanningContext.ContextType.FRIEND,
                                                        friend = friend
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(
                                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.FavoriteBorder,
                                                contentDescription = "Planear con amigo",
                                                tint = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                                            )
                                        }

                                        // Botón para eliminar amigo
                                        IconButton(
                                            onClick = { friendToDelete = friend }
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Eliminar amigo",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 1: GRUPOS Y GESTIÓN DE MIEMBROS
                // ==========================================
                else if (selectedTab == 1) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showCreateGroupDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Crear Grupo", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showJoinGroupDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unirse con Código", fontSize = 12.sp)
                                }
                            }
                        }

                        if (groupsList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("👥", fontSize = 42.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Aún no estás en ningún grupo",
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "Crea un grupo para votar citas con tus amigos o únete con un código.",
                                            fontSize = 12.sp,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(groupsList) { group ->
                                val isSelected =
                                    activeContext.type == ActivePlanningContext.ContextType.GROUP && activeContext.group?.id == group.id
                                val isCreator = group.createdBy == userProfile?.userId

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF9F9F9)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        Color(0xFF2E7D32)
                                    ) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFECB3)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(group.iconEmoji, fontSize = 22.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        group.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Color.Black
                                                    )
                                                    if (isCreator) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFE8F5E9))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                "ADMIN",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFF2E7D32),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    "Código: ${group.groupCode} • ${group.memberCount} miembros",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                                if (group.memberNames.isNotEmpty()) {
                                                    Text(
                                                        "Miembros: ${group.memberNames.joinToString(", ")}",
                                                        fontSize = 10.sp,
                                                        color = Color.DarkGray,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    onSelectContext(
                                                        ActivePlanningContext(
                                                            type = ActivePlanningContext.ContextType.GROUP,
                                                            group = group
                                                        )
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.Group,
                                                    contentDescription = "Planear en grupo",
                                                    tint = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                                                )
                                            }

                                            IconButton(
                                                onClick = { onRemoveGroup(group.id) }
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ExitToApp,
                                                    contentDescription = "Salir de grupo",
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Si el usuario actual es el creador, puede agregar a sus amigos
                                        if (isCreator) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { groupToInviteFriends = group },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp)
                                            ) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Agregar Amigos al Grupo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 2: MI PERFIL / CÓDIGO
                // ==========================================
                else if (selectedTab == 2) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isEditingProfile) {
                            // --- VISTA DE PERFIL (PANEL DE USUARIO) ---
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9))
                                        .border(3.dp, Color(0xFF2E7D32), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile?.avatarEmoji ?: profileEmojiInput,
                                        fontSize = 46.sp
                                    )
                                }
                            }

                            item {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = (userProfile?.name?.ifBlank { "Mi Perfil" }) ?: "Mi Perfil",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    if (!userProfile?.bio.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = userProfile?.bio ?: "",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                // Tarjeta de Código de Amigo con botón de copiar
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
                                Button(
                                    onClick = {
                                        profileNameInput = userProfile?.name ?: ""
                                        profileBioInput = userProfile?.bio ?: ""
                                        profileEmojiInput = userProfile?.avatarEmoji ?: "😎"
                                        isEditingProfile = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Editar Perfil", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        } else {
                            // --- MODO DE EDICIÓN DE PERFIL ---
                            item {
                                Text(
                                    text = "✏️ Editar Mi Perfil",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2E7D32)
                                )
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditingProfile = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("Cancelar", fontSize = 14.sp)
                                    }

                                    Button(
                                        onClick = {
                                            onUpdateProfile(profileNameInput, profileEmojiInput, profileBioInput)
                                            isEditingProfile = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Crear", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Unirse a Grupo por Código
    if (showJoinGroupDialog) {
        Dialog(onDismissRequest = { showJoinGroupDialog = false }) {
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
                        "Unirse a Grupo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = groupCodeInput,
                        onValueChange = { groupCodeInput = it.uppercase() },
                        label = { Text("Código de Grupo") },
                        placeholder = { Text("Ej: GRP-ABC12") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showJoinGroupDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (groupCodeInput.isNotBlank()) {
                                    onJoinGroupByCode(groupCodeInput.trim())
                                    showJoinGroupDialog = false
                                    groupCodeInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Unirse", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal para que el Creador agregue a sus amigos al grupo
    if (groupToInviteFriends != null) {
        val targetGroup = groupToInviteFriends!!
        Dialog(onDismissRequest = { groupToInviteFriends = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Text(
                        "Agregar Amigos a ${targetGroup.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        "Selecciona a tus amigos para añadirlos a este grupo:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val availableFriends = friendsList.filter { !targetGroup.memberNames.contains(it.name) }

                    if (availableFriends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tienes más amigos disponibles o todos ya forman parte de este grupo.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableFriends) { friend ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(friend.avatarEmoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            friend.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                onAddFriendToGroup(targetGroup.groupCode, friend)
                                                groupToInviteFriends = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Añadir", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { groupToInviteFriends = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar Amigo
    if (friendToDelete != null) {
        val targetFriend = friendToDelete!!
        AlertDialog(
            onDismissRequest = { friendToDelete = null },
            title = {
                Text("¿Eliminar amigo?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Estás seguro de que deseas eliminar a ${targetFriend.name}? Ya no podrán ver sus matches mutuos ni planear juntos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveFriend(targetFriend.id)
                        friendToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { friendToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

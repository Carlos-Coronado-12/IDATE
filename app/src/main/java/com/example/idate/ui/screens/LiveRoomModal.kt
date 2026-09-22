package com.example.idate.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.idate.data.remote.model.LiveRoom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomModal(
    liveRoom: LiveRoom?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreateRoom: (hostName: String) -> Unit,
    onJoinRoom: (code: String, guestName: String) -> Unit,
    onLeaveRoom: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Crear, 1: Unirse
    var hostNameInput by remember { mutableStateOf("") }
    var guestNameInput by remember { mutableStateOf("") }
    var roomCodeInput by remember { mutableStateOf("") }
    var copySuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .semantics { contentDescription = "Modal de Sala en Vivo para dos dispositivos" },
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                                .background(Color(0xFFFFE4E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Icono Sala Parejas",
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Sala en Tiempo Real",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Cerrar modal de sala en vivo" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (liveRoom == null) {
                    // Not connected yet: Tab selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFF5F5F5),
                        contentColor = Color(0xFFE91E63),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .height(44.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Crear Sala", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Unirme con PIN", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (selectedTab == 0) {
                        // CREAR SALA
                        Text(
                            text = "Crea una sala para deslizar planes al mismo tiempo con tu pareja y encontrar coincidencias al instante.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = hostNameInput,
                            onValueChange = { hostNameInput = it },
                            label = { Text("Tu nombre / Apodo") },
                            singleLine = true,
                            placeholder = { Text("Ej: Carlos") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onCreateRoom(hostNameInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar Sala de Citas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        // UNIRSE A SALA
                        Text(
                            text = "Ingresa el código PIN de 6 dígitos que te compartió tu pareja:",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = guestNameInput,
                            onValueChange = { guestNameInput = it },
                            label = { Text("Tu nombre / Apodo") },
                            singleLine = true,
                            placeholder = { Text("Ej: Andrea") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { if (it.length <= 8) roomCodeInput = it.uppercase() },
                            label = { Text("Código de Sala (PIN)") },
                            singleLine = true,
                            placeholder = { Text("Ej: ABC-123") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFE91E63))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF1744),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { onJoinRoom(roomCodeInput, guestNameInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectar Dispositivo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    // CONNECTED TO ROOM ACTIVE STATE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Room Code Display Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFF0F5))
                                .border(1.5.dp, Color(0xFFE91E63), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CÓDIGO DE TU SALA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFFE91E63)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = liveRoom.roomCode,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Código Sala IDATE", liveRoom.roomCode)
                                        clipboard.setPrimaryClip(clip)
                                        copySuccess = true
                                    },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copySuccess) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copiar código de sala",
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (copySuccess) "¡Código Copiado!" else "Copiar Código",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE91E63),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connection Status Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF9F9F9))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Estado: Sincronización en Vivo Activa",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = liveRoom.lastEventMessage.ifBlank { "Ambos dispositivos conectados" },
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Matches Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Matches en Vivo", fontSize = 11.sp, color = Color(0xFF7B1FA2))
                                    Text(
                                        "${liveRoom.matchedPlanIds.size}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7B1FA2)
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Dispositivos", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                    Text(
                                        if (liveRoom.partnerName != null) "2 Conectados" else "1 Conectado",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = onLeaveRoom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF1744))
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salir de la Sala", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

package com.example.idate.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.idate.R
import com.example.idate.data.remote.FirebaseStorageManager
import com.example.idate.model.Friend
import com.example.idate.model.FriendGroup
import com.example.idate.model.Plan
import com.example.idate.model.PlanScope
import com.example.idate.ui.components.PlanImage
import com.example.idate.ui.utils.ImagePickerUtils
import kotlinx.coroutines.launch

data class PresetImageOption(
    val name: String,
    val resName: String,
    val resId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanModal(
    editingPlan: Plan? = null,
    friendsList: List<Friend> = emptyList(),
    groupsList: List<FriendGroup> = emptyList(),
    onDismiss: () -> Unit,
    onCreatePlan: (
        title: String,
        category: String,
        description: String,
        location: String,
        duration: String,
        budget: String,
        tags: List<String>,
        imageUrl: String,
        imageResName: String,
        targetFriendId: String?,
        targetGroupId: String?,
        targetFriendName: String?,
        targetGroupName: String?,
        scope: PlanScope,
        peopleCount: String,
        showBudget: Boolean,
        showDuration: Boolean,
        showLocation: Boolean,
        showPeopleCount: Boolean,
        existingPlanId: Int?
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember(editingPlan) { mutableStateOf(editingPlan?.title ?: "") }
    var selectedCategory by remember(editingPlan) { mutableStateOf(editingPlan?.category ?: "Comida") }
    var description by remember(editingPlan) { mutableStateOf(editingPlan?.description ?: "") }
    
    // Configurable Fields & Toggles
    var showLocation by remember(editingPlan) { mutableStateOf(editingPlan?.showLocation ?: true) }
    var location by remember(editingPlan) { mutableStateOf(editingPlan?.location ?: "") }

    var showBudget by remember(editingPlan) { mutableStateOf(editingPlan?.showBudget ?: true) }
    var budget by remember(editingPlan) { mutableStateOf(editingPlan?.budget ?: "") }

    var showDuration by remember(editingPlan) { mutableStateOf(editingPlan?.showDuration ?: true) }
    var duration by remember(editingPlan) { mutableStateOf(editingPlan?.duration ?: "") }

    var showPeopleCount by remember(editingPlan) { mutableStateOf(editingPlan?.showPeopleCount ?: true) }
    var peopleCount by remember(editingPlan) { mutableStateOf(editingPlan?.peopleCount ?: "2 personas") }

    var tagInput by remember(editingPlan) { mutableStateOf("") }
    val tags = remember(editingPlan) {
        mutableStateListOf<String>().apply {
            if (editingPlan != null && editingPlan.tags.isNotEmpty()) {
                addAll(editingPlan.tags)
            } else {
                addAll(listOf("Favorito", "Nuevo"))
            }
        }
    }

    // Scope selection (Global, Friend, Group)
    var selectedScope by remember(editingPlan) { mutableStateOf(editingPlan?.scope ?: PlanScope.GLOBAL) }
    var selectedFriend by remember(editingPlan) {
        mutableStateOf<Friend?>(
            editingPlan?.targetFriendId?.let { id -> friendsList.find { it.id == id } }
        )
    }
    var selectedGroup by remember(editingPlan) {
        mutableStateOf<FriendGroup?>(
            editingPlan?.targetGroupId?.let { id -> groupsList.find { it.id == id } }
        )
    }
    var friendDropdownExpanded by remember { mutableStateOf(false) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    // Image selection state
    val presetImages = remember {
        listOf(
            PresetImageOption("Sushi", "plan_sushi", R.drawable.plan_sushi),
            PresetImageOption("Pizza", "plan_pizza", R.drawable.plan_pizza),
            PresetImageOption("Tacos", "plan_tacos", R.drawable.plan_tacos),
            PresetImageOption("Cine", "plan_cine", R.drawable.plan_cine),
            PresetImageOption("Drinks / Bar", "plan_drinks", R.drawable.plan_drinks),
            PresetImageOption("Escape Room", "plan_escape", R.drawable.plan_escape),
            PresetImageOption("Boliche", "plan_boliche", R.drawable.plan_boliche),
            PresetImageOption("Picnic", "plan_picnic", R.drawable.plan_picnic),
            PresetImageOption("Senderismo", "plan_senderismo", R.drawable.plan_senderismo),
            PresetImageOption("Jazz / Concierto", "plan_jazz", R.drawable.plan_jazz),
            PresetImageOption("Cerámica", "plan_ceramica", R.drawable.plan_ceramica),
            PresetImageOption("Armar Legos", "plan_legos", R.drawable.plan_legos)
        )
    }

    var selectedPresetResName by remember(editingPlan) { mutableStateOf("plan_sushi") }
    var customImageUrl by remember(editingPlan) { mutableStateOf(editingPlan?.imageUrl ?: "") }
    var isFromGallery by remember(editingPlan) {
        mutableStateOf(
            editingPlan?.imageUrl?.startsWith("file://") == true ||
            editingPlan?.imageUrl?.startsWith("data:image") == true ||
            editingPlan?.imageUrl?.contains("firebasestorage") == true
        )
    }

    val coroutineScope = rememberCoroutineScope()
    var isUploadingImage by remember { mutableStateOf(false) }

    // Gallery Picker launcher with Firebase Cloud Storage upload
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingImage = true
            isFromGallery = true
            coroutineScope.launch {
                val uploadResult = FirebaseStorageManager.uploadPlanImage(context, uri)
                uploadResult.onSuccess { cloudUrl ->
                    customImageUrl = cloudUrl
                    isUploadingImage = false
                }.onFailure {
                    val localFallback = ImagePickerUtils.processAndSaveGalleryImage(context, uri)
                    if (localFallback != null) {
                        customImageUrl = localFallback
                    }
                    isUploadingImage = false
                }
            }
        }
    }

    // Validation errors
    var titleError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var budgetError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Comida", "Película", "Fiesta", "Juegos", "Aire Libre", "Música", "Arte", "Planes")
    val peopleQuickOptions = listOf("1 persona", "2 personas", "3-5 personas", "Grupal (6+)", "Cualquiera")

    fun validateAndSubmit() {
        var isValid = true

        if (title.trim().length < 3) {
            titleError = "El título debe tener al menos 3 caracteres"
            isValid = false
        } else {
            titleError = null
        }

        if (showLocation && location.trim().isBlank()) {
            locationError = "La ubicación es requerida si el campo está activo"
            isValid = false
        } else {
            locationError = null
        }

        if (showBudget && budget.trim().isBlank()) {
            budgetError = "El presupuesto es requerido si el campo está activo"
            isValid = false
        } else {
            budgetError = null
        }

        if (isValid) {
            onCreatePlan(
                title.trim(),
                selectedCategory,
                description.trim().ifBlank { "Plan personalizado creado por ti." },
                if (showLocation) location.trim() else "",
                if (showDuration) duration.trim().ifBlank { "2 horas" } else "",
                if (showBudget) budget.trim() else "",
                tags.toList(),
                customImageUrl.trim(),
                selectedPresetResName,
                if (selectedScope == PlanScope.FRIEND_ONLY) selectedFriend?.id else null,
                if (selectedScope == PlanScope.GROUP_ONLY) selectedGroup?.id else null,
                if (selectedScope == PlanScope.FRIEND_ONLY) selectedFriend?.name else null,
                if (selectedScope == PlanScope.GROUP_ONLY) selectedGroup?.name else null,
                selectedScope,
                if (showPeopleCount) peopleCount.trim().ifBlank { "2 personas" } else "",
                showBudget,
                showDuration,
                showLocation,
                showPeopleCount,
                editingPlan?.id
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(vertical = 6.dp)
                .semantics { contentDescription = "Modal de formulario de plan" },
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (editingPlan != null) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (editingPlan != null) Icons.Default.Edit else Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = if (editingPlan != null) Color(0xFF1976D2) else Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (editingPlan != null) "Editar Plan" else "Nuevo Plan Personalizado",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Cerrar formulario de plan" }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Scope Selector (Global, Friend, Group)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3E5F5).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFFCE93D8), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "¿Para quién es este plan?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF6A1B9A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedScope == PlanScope.GLOBAL,
                                onClick = { selectedScope = PlanScope.GLOBAL },
                                label = { Text("🌍 General", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedScope == PlanScope.FRIEND_ONLY,
                                onClick = {
                                    selectedScope = PlanScope.FRIEND_ONLY
                                    if (selectedFriend == null && friendsList.isNotEmpty()) {
                                        selectedFriend = friendsList.first()
                                    }
                                },
                                label = { Text("👤 Amigo", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedScope == PlanScope.GROUP_ONLY,
                                onClick = {
                                    selectedScope = PlanScope.GROUP_ONLY
                                    if (selectedGroup == null && groupsList.isNotEmpty()) {
                                        selectedGroup = groupsList.first()
                                    }
                                },
                                label = { Text("👥 Grupo", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Friend Picker Dropdown
                        if (selectedScope == PlanScope.FRIEND_ONLY) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (friendsList.isEmpty()) {
                                Text(
                                    "No tienes amigos agregados aún. Agrega amigos desde el Hub.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC62828)
                                )
                            } else {
                                OutlinedCard(
                                    onClick = { friendDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Amigo: ${selectedFriend?.name ?: "Seleccionar amigo"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = friendDropdownExpanded,
                                    onDismissRequest = { friendDropdownExpanded = false }
                                ) {
                                    friendsList.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text("${f.avatarEmoji} ${f.name} (${f.friendCode})") },
                                            onClick = {
                                                selectedFriend = f
                                                friendDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Group Picker Dropdown
                        if (selectedScope == PlanScope.GROUP_ONLY) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (groupsList.isEmpty()) {
                                Text(
                                    "No perteneces a ningún grupo aún. Crea un grupo desde el Hub.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC62828)
                                )
                            } else {
                                OutlinedCard(
                                    onClick = { groupDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Grupo: ${selectedGroup?.name ?: "Seleccionar grupo"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = groupDropdownExpanded,
                                    onDismissRequest = { groupDropdownExpanded = false }
                                ) {
                                    groupsList.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text("${g.iconEmoji} ${g.name} (${g.groupCode})") },
                                            onClick = {
                                                selectedGroup = g
                                                groupDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (titleError != null) titleError = null
                        },
                        label = { Text("Título del Plan *") },
                        placeholder = { Text("Ej: Tarde de Juegos y Pizza") },
                        isError = titleError != null,
                        supportingText = titleError?.let { { Text(it, color = Color(0xFFFF1744)) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selection Chips
                    Text(
                        text = "Categoría:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = cat
                                    when (cat) {
                                        "Comida" -> selectedPresetResName = "plan_sushi"
                                        "Película" -> selectedPresetResName = "plan_cine"
                                        "Fiesta" -> selectedPresetResName = "plan_drinks"
                                        "Juegos" -> selectedPresetResName = "plan_escape"
                                        "Aire Libre" -> selectedPresetResName = "plan_picnic"
                                        "Música" -> selectedPresetResName = "plan_jazz"
                                        "Arte" -> selectedPresetResName = "plan_ceramica"
                                        "Planes" -> selectedPresetResName = "plan_legos"
                                    }
                                },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE91E63),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Image Selector Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Imagen del Plan:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        // 1. Botón Cargar de Galería
                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFromGallery && customImageUrl.isNotBlank()) "Cambiar Foto de la Galería" else "Cargar Foto de mi Galería",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Uploading Progress Indicator
                        if (isUploadingImage) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color(0xFF2E7D32),
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = "Subiendo imagen a la nube...",
                                        color = Color(0xFF1B5E20),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Visible al instante para todos tus amigos",
                                        color = Color(0xFF388E3C),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Live Preview
                        if (customImageUrl.isNotBlank() && !isUploadingImage) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PlanImage(
                                    imageUrl = customImageUrl,
                                    contentDescription = "Vista previa de foto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFromGallery) "✓ Foto de galería seleccionada" else "✓ URL personalizada activa",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isFromGallery) "Lista para mostrarse en las tarjetas" else customImageUrl,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        customImageUrl = ""
                                        isFromGallery = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Quitar foto",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(vertical = 2.dp))

                        // 2. Presets Carousel
                        Text(
                            text = "O elige una de nuestras imágenes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presetImages.forEach { option ->
                                val isSelected = selectedPresetResName == option.resName && customImageUrl.isBlank()
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(76.dp)
                                        .clickable {
                                            selectedPresetResName = option.resName
                                            customImageUrl = ""
                                            isFromGallery = false
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFE91E63) else Color.LightGray,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        Image(
                                            painter = painterResource(id = option.resId),
                                            contentDescription = option.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFE91E63).copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Seleccionado",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFE91E63) else Color.DarkGray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // 3. Custom Image URL Input
                        OutlinedTextField(
                            value = if (isFromGallery) "" else customImageUrl,
                            onValueChange = {
                                customImageUrl = it
                                isFromGallery = false
                            },
                            label = { Text("O pega una URL de Imagen Web") },
                            placeholder = { Text("https://ejemplo.com/foto.jpg") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color.Gray)
                            },
                            trailingIcon = {
                                if (customImageUrl.isNotBlank() && !isFromGallery) {
                                    IconButton(onClick = { customImageUrl = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar URL", tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Description Input
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        placeholder = { Text("¿De qué trata este plan especial?") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // --- CONFIGURABLE ATTRIBUTES (Toggles for Price, Duration, Location, People) ---
                    Text(
                        text = "Configuración de Campos en la Tarjeta:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    // 1. UBICACIÓN (Location)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF9FAFB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Campo de Ubicación", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = if (showLocation) "Visible en la tarjeta" else "Oculto (apagado)",
                                            fontSize = 11.sp,
                                            color = if (showLocation) Color(0xFF2E7D32) else Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked = showLocation,
                                    onCheckedChange = { showLocation = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE91E63))
                                )
                            }

                            AnimatedVisibility(visible = showLocation) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = location,
                                        onValueChange = {
                                            location = it
                                            if (locationError != null) locationError = null
                                        },
                                        placeholder = { Text("Ej: Casa / Parque / Cafetería") },
                                        isError = locationError != null,
                                        supportingText = locationError?.let { { Text(it, color = Color(0xFFFF1744)) } },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. PRECIO / PRESUPUESTO (Budget)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF9FAFB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Campo de Precio / Presupuesto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = if (showBudget) "Visible en la tarjeta" else "Oculto (apagado)",
                                            fontSize = 11.sp,
                                            color = if (showBudget) Color(0xFF2E7D32) else Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked = showBudget,
                                    onCheckedChange = { showBudget = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4CAF50))
                                )
                            }

                            AnimatedVisibility(visible = showBudget) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = budget,
                                        onValueChange = {
                                            budget = it
                                            if (budgetError != null) budgetError = null
                                        },
                                        placeholder = { Text("Ej: $200 MXN o Gratis") },
                                        isError = budgetError != null,
                                        supportingText = budgetError?.let { { Text(it, color = Color(0xFFFF1744)) } },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. TIEMPO / DURACIÓN (Duration)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF9FAFB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Campo de Tiempo / Duración", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = if (showDuration) "Visible en la tarjeta" else "Oculto (apagado)",
                                            fontSize = 11.sp,
                                            color = if (showDuration) Color(0xFF2E7D32) else Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked = showDuration,
                                    onCheckedChange = { showDuration = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF9800))
                                )
                            }

                            AnimatedVisibility(visible = showDuration) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = duration,
                                        onValueChange = { duration = it },
                                        placeholder = { Text("Ej: 2 horas, Tarde completa...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. NÚMERO DE PERSONAS (People / Capacity)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF9FAFB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("¿Cuántas personas pueden participar?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = if (showPeopleCount) "Visible en la tarjeta" else "Oculto (apagado)",
                                            fontSize = 11.sp,
                                            color = if (showPeopleCount) Color(0xFF2E7D32) else Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked = showPeopleCount,
                                    onCheckedChange = { showPeopleCount = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF673AB7))
                                )
                            }

                            AnimatedVisibility(visible = showPeopleCount) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        peopleQuickOptions.forEach { opt ->
                                            val isSelected = peopleCount == opt
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { peopleCount = opt },
                                                label = { Text(opt, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF673AB7),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = peopleCount,
                                        onValueChange = { peopleCount = it },
                                        placeholder = { Text("Ej: 2 personas, 4 amigos...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tags Input
                    Column {
                        Text(
                            text = "Etiquetas:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                placeholder = { Text("Añadir tag (ej: Romántico)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (tagInput.isNotBlank() && !tags.contains(tagInput.trim())) {
                                        tags.add(tagInput.trim())
                                        tagInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF0F5))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir etiqueta", tint = Color(0xFFE91E63))
                            }
                        }

                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { tags.remove(tag) },
                                        label = { Text(tag, fontSize = 11.sp) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Eliminar tag",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = { validateAndSubmit() },
                    enabled = !isUploadingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editingPlan != null) Color(0xFF1976D2) else Color(0xFFE91E63)
                    )
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subiendo imagen...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(
                            imageVector = if (editingPlan != null) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingPlan != null) "Guardar Cambios del Plan" else "Guardar Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

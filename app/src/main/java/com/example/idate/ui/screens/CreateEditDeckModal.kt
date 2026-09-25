package com.example.idate.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.idate.R
import com.example.idate.data.remote.FirebaseStorageManager
import com.example.idate.model.DateDeck
import com.example.idate.model.Plan
import com.example.idate.ui.components.PlanImage
import com.example.idate.ui.utils.ImagePickerUtils
import kotlinx.coroutines.launch

private val EMOJI_OPTIONS = listOf("🃏", "💕", "🍔", "🎬", "🌲", "☕", "🎮", "🎢", "✨", "🍷", "🏎️", "🏖️", "🍕", "🧁", "🎧")
private val COLOR_OPTIONS = listOf(
    0xFFFF4B72L, // Pink / Rose
    0xFF2E7D32L, // Emerald / Green
    0xFF8A2BE2L, // Purple
    0xFF3B82F6L, // Blue
    0xFFF59E0BL, // Amber / Orange
    0xFFEF4444L, // Red
    0xFF06B6D4L, // Cyan
    0xFFEC4899L  // Fuchsia
)

data class PresetImageItem(val title: String, val resName: String, val resId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditDeckModal(
    allPlans: List<Plan>,
    editingDeck: DateDeck? = null,
    onDismiss: () -> Unit,
    onCreatePlanInline: (
        title: String,
        category: String,
        description: String,
        location: String,
        duration: String,
        budget: String,
        tags: List<String>,
        imageUrl: String,
        imageResName: String,
        peopleCount: String,
        showBudget: Boolean,
        showDuration: Boolean,
        showLocation: Boolean,
        showPeopleCount: Boolean,
        existingPlanId: Int?,
        onPlanCreated: (Plan) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveDeck: (
        name: String,
        description: String,
        iconEmoji: String,
        colorHex: Long,
        planIds: List<Int>,
        deckId: String?
    ) -> Unit
) {
    var deckName by remember { mutableStateOf(editingDeck?.name ?: "") }
    var description by remember { mutableStateOf(editingDeck?.description ?: "") }
    var selectedEmoji by remember { mutableStateOf(editingDeck?.iconEmoji ?: "🃏") }
    var selectedColorHex by remember { mutableStateOf(editingDeck?.colorHex ?: 0xFFFF4B72L) }
    val selectedPlanIds = remember {
        mutableStateListOf<Int>().apply {
            editingDeck?.planIds?.let { addAll(it) }
        }
    }

    var showExistingPlansSelector by remember { mutableStateOf(false) }
    var showCreateNewPlanDialog by remember { mutableStateOf(false) }
    var editingPlanInDeck by remember { mutableStateOf<Plan?>(null) }

    // Map selected plans
    val plansInDeck = remember(selectedPlanIds.toList(), allPlans) {
        selectedPlanIds.mapNotNull { id -> allPlans.find { it.id == id } }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(selectedColorHex).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedEmoji, fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingDeck == null) "Crear Nueva Baraja" else "Editar Baraja",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Llénala con planes y guárdala para jugar o compartir",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE5E7EB))

                // Scrollable Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Deck Name Input
                    item {
                        OutlinedTextField(
                            value = deckName,
                            onValueChange = { deckName = it },
                            label = { Text("Nombre de la Baraja") },
                            placeholder = { Text("Ej: Citas Románticas, Salidas Chill...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                                focusedBorderColor = Color(selectedColorHex),
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    // Description Input
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción (opcional)") },
                            placeholder = { Text("Ej: Planes ideales para una noche tranquila en pareja") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                                focusedBorderColor = Color(selectedColorHex),
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    // Emoji Selection
                    item {
                        Text(
                            text = "Ícono de la Baraja",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EMOJI_OPTIONS.forEach { emoji ->
                                val isSelected = selectedEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(selectedColorHex).copy(alpha = 0.2f)
                                            else Color(0xFFF3F4F6)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(selectedColorHex) else Color(0xFFE5E7EB),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // Color Accent Selection
                    item {
                        Text(
                            text = "Color de Acento",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            COLOR_OPTIONS.forEach { hexLong ->
                                val composeColor = Color(hexLong)
                                val isSelected = selectedColorHex == hexLong
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(composeColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF111827) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hexLong },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Plans Section Header
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE5E7EB))
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
                                    text = "Planes en esta Baraja",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Surface(
                                    color = Color(selectedColorHex).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${selectedPlanIds.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(selectedColorHex),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Add buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button 1: Create New Plan for Deck
                            Button(
                                onClick = { showCreateNewPlanDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColorHex)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crear Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Button 2: Add Existing Plans
                            OutlinedButton(
                                onClick = { showExistingPlansSelector = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1F2937)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir Existentes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Empty State or List of Plans in Deck
                    if (plansInDeck.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF9FAFB),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "📭", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Esta baraja está vacía",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151)
                                    )
                                    Text(
                                        text = "Añade planes existentes o crea planes nuevos con fotos personalizadas para llenarla.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(plansInDeck, key = { it.id }) { plan ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF9FAFB),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Plan thumbnail image
                                    PlanImage(
                                        plan = plan,
                                        contentDescription = plan.title,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF111827),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val planMeta = listOfNotNull(
                                            plan.category.takeIf { it.isNotBlank() },
                                            plan.duration.takeIf { it.isNotBlank() },
                                            plan.budget.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ")

                                        if (planMeta.isNotBlank()) {
                                            Text(
                                                text = planMeta,
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Edit Plan Button
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(selectedColorHex).copy(alpha = 0.12f))
                                                .clickable { editingPlanInDeck = plan },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar Plan",
                                                tint = Color(selectedColorHex),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Delete/Remove from Deck Button
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFEE2E2))
                                                .clickable { selectedPlanIds.remove(plan.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Quitar",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE5E7EB))

                // Bottom Save CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (deckName.isNotBlank() && selectedPlanIds.isNotEmpty()) {
                                onSaveDeck(
                                    deckName,
                                    description,
                                    selectedEmoji,
                                    selectedColorHex,
                                    selectedPlanIds.toList(),
                                    editingDeck?.id
                                )
                            }
                        },
                        enabled = deckName.isNotBlank() && selectedPlanIds.isNotEmpty(),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(selectedColorHex),
                            disabledContainerColor = Color(selectedColorHex).copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (editingDeck == null) "Guardar Baraja" else "Actualizar",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Modal to pick and add existing plans
    if (showExistingPlansSelector) {
        ExistingPlansSelectorModal(
            allPlans = allPlans,
            initiallySelectedIds = selectedPlanIds.toList(),
            accentColor = Color(selectedColorHex),
            onDismiss = { showExistingPlansSelector = false },
            onConfirmSelection = { newIds ->
                selectedPlanIds.clear()
                selectedPlanIds.addAll(newIds)
                showExistingPlansSelector = false
            }
        )
    }

    // Modal to create a new plan directly into this deck with custom image selection
    if (showCreateNewPlanDialog) {
        InlinePlanCreatorModal(
            planToEdit = null,
            accentColor = Color(selectedColorHex),
            onDismiss = { showCreateNewPlanDialog = false },
            onCreatePlan = { title, category, desc, loc, dur, bud, tags, imageUrl, imageResName, peopleCount, showBudget, showDuration, showLocation, showPeopleCount ->
                onCreatePlanInline(title, category, desc, loc, dur, bud, tags, imageUrl, imageResName, peopleCount, showBudget, showDuration, showLocation, showPeopleCount, null) { createdPlan ->
                    if (!selectedPlanIds.contains(createdPlan.id)) {
                        selectedPlanIds.add(createdPlan.id)
                    }
                    showCreateNewPlanDialog = false
                }
            }
        )
    }

    // Modal to edit an existing plan directly inside this deck
    if (editingPlanInDeck != null) {
        InlinePlanCreatorModal(
            planToEdit = editingPlanInDeck,
            accentColor = Color(selectedColorHex),
            onDismiss = { editingPlanInDeck = null },
            onCreatePlan = { title, category, desc, loc, dur, bud, tags, imageUrl, imageResName, peopleCount, showBudget, showDuration, showLocation, showPeopleCount ->
                onCreatePlanInline(title, category, desc, loc, dur, bud, tags, imageUrl, imageResName, peopleCount, showBudget, showDuration, showLocation, showPeopleCount, editingPlanInDeck?.id) { _ ->
                    editingPlanInDeck = null
                }
            }
        )
    }
}

@Composable
fun ExistingPlansSelectorModal(
    allPlans: List<Plan>,
    initiallySelectedIds: List<Int>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirmSelection: (List<Int>) -> Unit
) {
    val tempSelectedIds = remember { mutableStateListOf<Int>().apply { addAll(initiallySelectedIds) } }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val categories = remember(allPlans) {
        listOf("Todos") + allPlans.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredPlans = remember(allPlans, searchQuery, selectedCategory) {
        allPlans.filter { plan ->
            val matchesCategory = selectedCategory == "Todos" || plan.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    plan.title.contains(searchQuery, ignoreCase = true) ||
                    plan.description.contains(searchQuery, ignoreCase = true) ||
                    plan.tags.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Añadir Planes a la Baraja",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "${tempSelectedIds.size} seleccionados",
                            fontSize = 12.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF374151))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por título o etiqueta...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier.clickable { selectedCategory = cat },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) accentColor else Color(0xFFF3F4F6)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF374151),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Plan Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPlans, key = { it.id }) { plan ->
                        val isChecked = plan.id in tempSelectedIds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) tempSelectedIds.remove(plan.id)
                                    else tempSelectedIds.add(plan.id)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) accentColor.copy(alpha = 0.08f) else Color(0xFFF9FAFB),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChecked) accentColor else Color(0xFFE5E7EB)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) tempSelectedIds.add(plan.id)
                                        else tempSelectedIds.remove(plan.id)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                PlanImage(
                                    plan = plan,
                                    contentDescription = plan.title,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${plan.category} • ${plan.duration} • ${plan.budget}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { onConfirmSelection(tempSelectedIds.toList()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Añadir (${tempSelectedIds.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InlinePlanCreatorModal(
    planToEdit: Plan? = null,
    accentColor: Color,
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
        peopleCount: String,
        showBudget: Boolean,
        showDuration: Boolean,
        showLocation: Boolean,
        showPeopleCount: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember(planToEdit) { mutableStateOf(planToEdit?.title ?: "") }
    var category by remember(planToEdit) { mutableStateOf(planToEdit?.category ?: "Romántico") }
    var description by remember(planToEdit) { mutableStateOf(planToEdit?.description ?: "") }

    var showLocation by remember(planToEdit) { mutableStateOf(planToEdit?.showLocation ?: true) }
    var location by remember(planToEdit) { mutableStateOf(planToEdit?.location ?: "") }

    var showDuration by remember(planToEdit) { mutableStateOf(planToEdit?.showDuration ?: true) }
    var duration by remember(planToEdit) { mutableStateOf(planToEdit?.duration ?: "2 horas") }

    var showBudget by remember(planToEdit) { mutableStateOf(planToEdit?.showBudget ?: true) }
    var budget by remember(planToEdit) { mutableStateOf(planToEdit?.budget ?: "$$") }

    var showPeopleCount by remember(planToEdit) { mutableStateOf(planToEdit?.showPeopleCount ?: true) }
    var peopleCount by remember(planToEdit) { mutableStateOf(planToEdit?.peopleCount ?: "2 personas") }

    val presetImages = remember {
        listOf(
            PresetImageItem("Legos", "plan_legos", R.drawable.plan_legos),
            PresetImageItem("Sushi", "plan_sushi", R.drawable.plan_sushi),
            PresetImageItem("Pizza", "plan_pizza", R.drawable.plan_pizza),
            PresetImageItem("Tacos", "plan_tacos", R.drawable.plan_tacos),
            PresetImageItem("Cine", "plan_cine", R.drawable.plan_cine),
            PresetImageItem("Drinks", "plan_drinks", R.drawable.plan_drinks),
            PresetImageItem("Escape", "plan_escape", R.drawable.plan_escape),
            PresetImageItem("Boliche", "plan_boliche", R.drawable.plan_boliche),
            PresetImageItem("Picnic", "plan_picnic", R.drawable.plan_picnic),
            PresetImageItem("Montaña", "plan_senderismo", R.drawable.plan_senderismo),
            PresetImageItem("Jazz", "plan_jazz", R.drawable.plan_jazz),
            PresetImageItem("Cerámica", "plan_ceramica", R.drawable.plan_ceramica)
        )
    }

    var customImageUrl by remember(planToEdit) {
        mutableStateOf(
            if (planToEdit != null && (planToEdit.imageUrl.startsWith("http") || planToEdit.imageUrl.startsWith("file://") || planToEdit.imageUrl.startsWith("data:"))) {
                planToEdit.imageUrl
            } else ""
        )
    }
    var selectedPresetResName by remember(planToEdit) {
        mutableStateOf(
            if (planToEdit != null) {
                val matched = presetImages.find { it.resId == planToEdit.imageResId }
                matched?.resName ?: (if (planToEdit.imageUrl.isNotBlank() && !planToEdit.imageUrl.startsWith("http")) planToEdit.imageUrl else "plan_legos")
            } else "plan_legos"
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

    val categories = listOf("Romántico", "Aventura", "Gastronomía", "Chill", "Cultura", "Nocturno")
    val durations = listOf("1 hora", "2 horas", "Tarde completa", "Todo el día")
    val budgets = listOf("$", "$$", "$$$", "Gratis")
    val peopleQuickOptions = listOf("1 persona", "2 personas", "3-5 personas", "Grupal (6+)", "Cualquiera")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (planToEdit != null) "✏️ Editar Plan de la Baraja" else "✨ Crear Plan para la Baraja",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF374151))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Image Picker Section
                    item {
                        Text("Imagen del Plan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Live Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF3F4F6))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingImage) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = accentColor,
                                        strokeWidth = 2.5.dp
                                    )
                                    Text("Subiendo imagen...", fontSize = 12.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
                                }
                            } else if (customImageUrl.isNotBlank()) {
                                PlanImage(
                                    imageUrl = customImageUrl,
                                    contentDescription = "Foto elegida",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val currentPreset = presetImages.find { it.resName == selectedPresetResName }
                                if (currentPreset != null) {
                                    Image(
                                        painter = painterResource(id = currentPreset.resId),
                                        contentDescription = currentPreset.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Gallery Button Overlay
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable { galleryLauncher.launch("image/*") },
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("Elegir de Galería", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom URL Field
                        OutlinedTextField(
                            value = if (customImageUrl.startsWith("file://")) "" else customImageUrl,
                            onValueChange = { customImageUrl = it },
                            placeholder = { Text("O ingresa URL de imagen web (https://...)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Presets Carousel
                        Text("O elige una imagen predeterminada:", fontSize = 11.sp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetImages.forEach { preset ->
                                val isSelected = customImageUrl.isBlank() && selectedPresetResName == preset.resName
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) accentColor else Color(0xFFE5E7EB),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            customImageUrl = ""
                                            selectedPresetResName = preset.resName
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(id = preset.resId),
                                        contentDescription = preset.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Title
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título del Plan") },
                            placeholder = { Text("Ej: Picnic nocturno bajo las estrellas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Category
                    item {
                        Text("Categoría", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category == cat
                                Surface(
                                    modifier = Modifier.clickable { category = cat },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) accentColor else Color(0xFFF3F4F6)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF374151),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción") },
                            placeholder = { Text("Detalles del plan, qué llevar o cómo prepararlo...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Configurable Field: Personas
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9FAFB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                        Text("Personas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Switch(
                                        checked = showPeopleCount,
                                        onCheckedChange = { showPeopleCount = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }
                                if (showPeopleCount) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        peopleQuickOptions.forEach { opt ->
                                            val isSel = peopleCount == opt
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { peopleCount = opt },
                                                label = { Text(opt, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = accentColor,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = peopleCount,
                                        onValueChange = { peopleCount = it },
                                        placeholder = { Text("Ej: 2 personas", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Configurable Field: Location
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9FAFB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                        Text("Ubicación", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Switch(
                                        checked = showLocation,
                                        onCheckedChange = { showLocation = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                    )
                                }
                                if (showLocation) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = location,
                                        onValueChange = { location = it },
                                        placeholder = { Text("Ej: Parque Mirador, Cafetería Roma...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF111827),
                                            unfocusedTextColor = Color(0xFF111827),
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = Color(0xFFD1D5DB),
                                            focusedContainerColor = Color(0xFFF9FAFB),
                                            unfocusedContainerColor = Color(0xFFF9FAFB)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Configurable Field: Duration & Budget
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9FAFB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                        Text("Duración & Precio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = showDuration,
                                            onClick = { showDuration = !showDuration },
                                            label = { Text(if (showDuration) "⏱️ Sí" else "⏱️ No", fontSize = 10.sp) }
                                        )
                                        FilterChip(
                                            selected = showBudget,
                                            onClick = { showBudget = !showBudget },
                                            label = { Text(if (showBudget) "💰 Sí" else "💰 No", fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (showDuration) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Duración", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            durations.forEach { dur ->
                                                val isSelected = duration == dur
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp)
                                                        .clickable { duration = dur },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null
                                                ) {
                                                    Text(
                                                        text = dur,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) accentColor else Color(0xFF374151),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (showBudget) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Presupuesto", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            budgets.forEach { bud ->
                                                val isSelected = budget == bud
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp)
                                                        .clickable { budget = bud },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null
                                                ) {
                                                    Text(
                                                        text = bud,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) accentColor else Color(0xFF374151),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreatePlan(
                                    title.trim(),
                                    category,
                                    description.trim(),
                                    if (showLocation) location.trim() else "",
                                    if (showDuration) duration else "",
                                    if (showBudget) budget else "",
                                    listOf(category, duration, budget),
                                    customImageUrl.trim(),
                                    selectedPresetResName,
                                    if (showPeopleCount) peopleCount else "",
                                    showBudget,
                                    showDuration,
                                    showLocation,
                                    showPeopleCount
                                )
                            }
                        },
                        enabled = title.isNotBlank() && !isUploadingImage,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Subiendo...", fontWeight = FontWeight.Bold)
                        } else {
                            Text(if (planToEdit != null) "Guardar Cambios" else "Crear y Añadir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

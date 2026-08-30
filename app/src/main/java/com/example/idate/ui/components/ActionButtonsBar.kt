package com.example.idate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ActionButtonsBar(
    onRewind: () -> Unit,
    onDislike: () -> Unit,
    onLike: () -> Unit,
    canRewind: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 36.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rewind / Deshacer Button (Orange)
        ActionButton(
            icon = Icons.Default.Replay,
            tint = if (canRewind) Color(0xFFF57C00) else Color.LightGray,
            size = 48.dp,
            iconSize = 24.dp,
            enabled = canRewind,
            onClick = onRewind
        )

        // 2. Rechazar / Paso Button (Red ⬇️)
        ActionButton(
            icon = Icons.Default.KeyboardArrowDown,
            tint = Color(0xFFFF1744),
            size = 58.dp,
            iconSize = 34.dp,
            onClick = onDislike
        )

        // 3. Me Interesa / Aceptar Button (Green ⬆️) - Moved to star's position!
        ActionButton(
            icon = Icons.Default.KeyboardArrowUp,
            tint = Color(0xFF00E676),
            size = 58.dp,
            iconSize = 34.dp,
            onClick = onLike
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    tint: Color,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = if (enabled) 6.dp else 1.dp,
        modifier = Modifier.size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

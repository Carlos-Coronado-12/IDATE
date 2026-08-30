package com.example.idate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idate.R

@Composable
fun TopBar(
    savedPlansCount: Int,
    onOpenSavedPlans: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile & Saved Plans Button (Left)
        BadgedBox(
            badge = {
                if (savedPlansCount > 0) {
                    Badge(
                        containerColor = Color(0xFFE91E63),
                        contentColor = Color.White
                    ) {
                        Text(text = "$savedPlansCount", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        ) {
            IconButton(
                onClick = onOpenSavedPlans,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0F5))
                    .border(1.5.dp, Color(0xFFE91E63), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Mis Planes Guardados",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Exact IDATE Central PNG Logo (Uncropped Heart)
        Box(
            modifier = Modifier
                .clickable { onOpenSavedPlans() }
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_idate_logo),
                contentDescription = "IDATE Logo",
                modifier = Modifier.height(44.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Settings Button (Right)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = Color.DarkGray,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

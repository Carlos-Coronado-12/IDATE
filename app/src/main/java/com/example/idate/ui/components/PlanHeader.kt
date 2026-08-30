package com.example.idate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idate.model.Plan

@Composable
fun PlanHeader(plan: Plan) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Icon + Category (Red) + Title (Black)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = true)
        ) {
            Icon(
                imageVector = plan.icon,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = plan.category,
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = plan.title,
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right side: Detail
        Text(
            text = plan.detail,
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }
}

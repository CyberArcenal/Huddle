package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InteractionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.Black,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onPositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .onGloballyPositioned { coordinates ->
                onPositioned?.invoke(coordinates)
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = tint
            )
        }
    }
}
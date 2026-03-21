package com.cyberarcenal.huddle.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType

@Composable
fun ReactionPicker(
    anchorOffset: IntOffset?,
    onReactionSelected: (ReactionType) -> Unit,
    onDismiss: () -> Unit
) {
    val reactions = listOf(
        ReactionType.LIKE to Icons.Filled.ThumbUp,
        ReactionType.LOVE to Icons.Filled.Favorite,
        ReactionType.CARE to Icons.Filled.VolunteerActivism,
        ReactionType.HAHA to Icons.Filled.SentimentVerySatisfied,
        ReactionType.WOW to Icons.Filled.SentimentSatisfied,
        ReactionType.SAD to Icons.Filled.SentimentDissatisfied,
        ReactionType.ANGRY to Icons.Filled.SentimentVeryDissatisfied
    )

    if (anchorOffset != null) {
        Popup(
            alignment = Alignment.BottomStart,
            offset = anchorOffset,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reactions.forEach { (type, icon) ->
                    ReactionIcon(
                        type = type,
                        icon = icon,
                        onClick = {
                            onReactionSelected(type)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionIcon(
    type: ReactionType,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scale.animateTo(1f, animationSpec = spring())
    }
    Icon(
        imageVector = icon,
        contentDescription = type.toString(),
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable { onClick() }
            .scale(scale.value),
        tint = when (type) {
            ReactionType.LIKE -> Color(0xFF4267B2)
            ReactionType.LOVE -> Color(0xFFF44336)
            else -> Color.Black
        }
    )
}
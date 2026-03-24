// CommentInteractionBar.kt (final with color fix)
package com.cyberarcenal.huddle.ui.comments.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.CommentStatistics
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.ui.common.feed.getReactionIcon
import com.cyberarcenal.huddle.ui.common.feed.mapCurrentReaction

@Composable
fun CommentInteractionBar(
    statistics: CommentStatistics? = null,
    onReactionSelected: (ReactionType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactionItems = remember {
        listOf(
            Reaction(key = ReactionType.LIKE, label = "Like", painterResource = R.drawable.like),
            Reaction(key = ReactionType.LOVE, label = "Love", painterResource = R.drawable.love),
            Reaction(key = ReactionType.CARE, label = "Care", painterResource = R.drawable.care),
            Reaction(key = ReactionType.HAHA, label = "Haha", painterResource = R.drawable.haha),
            Reaction(key = ReactionType.WOW, label = "Wow", painterResource = R.drawable.wow),
            Reaction(key = ReactionType.SAD, label = "Sad", painterResource = R.drawable.sad),
            Reaction(key = ReactionType.ANGRY, label = "Angry", painterResource = R.drawable.angry),
        )
    }

    // 1. Gawing State ang currentReaction para mag-trigger ng UI update
    // Ginagamit ang remember(statistics) para mag-sync kapag nag-refresh ang data mula sa server
    var localReaction by remember(statistics?.currentReaction) {
        mutableStateOf(mapCurrentReaction(statistics?.currentReaction))
    }

    val reactionCount = statistics?.reactionCount ?: 0

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == localReaction }
    )

    // 2. Makinig sa pagbabago mula sa Picker
    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionType
        if (selectedKey != localReaction) {
            localReaction = selectedKey // Update local UI immediately
            onReactionSelected(selectedKey)
        }
    }

    // 3. Kunin ang tamang icon base sa local state
    val (icon, tint) = getReactionIcon(localReaction)
    val finalTint = if (localReaction == null) Color.Gray else tint

    Row(
        modifier = modifier
            .reactionPickerAnchor(pickerState)
            .clickable {
                // Toggle logic: Like o Null
                val newReaction = if (localReaction != null) null else ReactionType.LIKE
                localReaction = newReaction
                onReactionSelected(newReaction)
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (icon) {
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = finalTint
                )
            }
            is Int -> {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified // Importante para sa colored drawables
                )
            }
        }

        if (reactionCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = reactionCount.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}
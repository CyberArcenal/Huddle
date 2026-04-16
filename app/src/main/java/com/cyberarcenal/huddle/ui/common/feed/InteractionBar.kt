// InteractionBar.kt – updated with reversed button order (share left, like right)
package com.cyberarcenal.huddle.ui.common.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.data.models.Reaction

fun getReactionIcon(reactionType: ReactionTypeEnum?): Pair<Any, Color> {
    return when (reactionType) {
        ReactionTypeEnum.LIKE -> Pair(R.drawable.like, Color(0xFF2196F3))
        ReactionTypeEnum.DISLIKE -> Pair(R.drawable.dislike, Color(0xFFF44336))
        ReactionTypeEnum.LOVE -> Pair(R.drawable.love, Color(0xFFE91E63))
        ReactionTypeEnum.CARE -> Pair(R.drawable.care, Color(0xFF4CAF50))
        ReactionTypeEnum.HAHA -> Pair(R.drawable.haha, Color(0xFFFFC107))
        ReactionTypeEnum.WOW -> Pair(R.drawable.wow, Color(0xFFFFC107))
        ReactionTypeEnum.SAD -> Pair(R.drawable.sad, Color(0xFFFFC107))
        ReactionTypeEnum.ANGRY -> Pair(R.drawable.angry, Color(0xFFF44336))
        else -> Pair(Icons.Outlined.FavoriteBorder, Color.Black)
    }
}

@Composable
fun InteractionBar(
    currentReaction: ReactionTypeEnum?,
    reactionCount: Int,
    commentCount: Int,
    onReactionSelected: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReactionSummaryClick: () -> Unit = onCommentClick
) {
    val reactionItems = remember {
        listOf(
            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", painterResource = R.drawable.like),
            Reaction(key = ReactionTypeEnum.DISLIKE, label = "Dislike", painterResource = R.drawable.dislike),
            Reaction(key = ReactionTypeEnum.LOVE, label = "Love", painterResource = R.drawable.love),
            Reaction(key = ReactionTypeEnum.CARE, label = "Care", painterResource = R.drawable.care),
            Reaction(key = ReactionTypeEnum.HAHA, label = "Haha", painterResource = R.drawable.haha),
            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", painterResource = R.drawable.wow),
            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", painterResource = R.drawable.sad),
            Reaction(key = ReactionTypeEnum.ANGRY, label = "Angry", painterResource = R.drawable.angry),
        )
    }

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == currentReaction }
    )

    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionTypeEnum
        if (selectedKey != currentReaction) {
            onReactionSelected(selectedKey)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Share button (left)
        InteractionButton(
            icon = R.drawable.share,
            label = "Share",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onShareClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Comment button (middle)
        InteractionButton(
            icon = R.drawable.comment,
            label = if (commentCount > 0) commentCount.toString() else "Comment",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onCommentClick
        )

        // Push like button to the right
        Spacer(modifier = Modifier.weight(1f))

        // Reaction button (right) – anchor for long press
        val (icon, tint) = getReactionIcon(currentReaction)
//        val reactionLabel = if (reactionCount > 0) reactionCount.toString() else "Like"
        val interactionTint = if (currentReaction != null) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant
        InteractionButton(
            icon = icon,
            label = "",
            tint = interactionTint,
            onClick = {
                val newReaction = if (currentReaction != null) null else ReactionTypeEnum.LIKE
                onReactionSelected(newReaction)
            },
            modifier = Modifier.reactionPickerAnchor(pickerState)
        )
    }
}

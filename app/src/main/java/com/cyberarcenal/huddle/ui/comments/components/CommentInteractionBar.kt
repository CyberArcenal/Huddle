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
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.ui.common.getReactionIcon

@Composable
fun CommentInteractionBar(
    reactionCount: Int,
    userReaction: ReactionType?,
    onReact: (ReactionType?) -> Unit,
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

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == userReaction }
    )

    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionType
        if (selectedKey != userReaction) {
            onReact(selectedKey)
        }
    }

    val (icon, tint) = getReactionIcon(userReaction)
    val finalTint = if (userReaction == null) Color.Gray else tint

    Row(
        modifier = modifier
            .reactionPickerAnchor(pickerState)
            .clickable {
                val newReaction = if (userReaction == ReactionType.LIKE) null else ReactionType.LIKE
                onReact(newReaction)
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
                    tint = Color.Unspecified // preserve original colors of the drawable
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
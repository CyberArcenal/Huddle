package com.cyberarcenal.huddle.ui.comments.components
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType

@Composable
fun CommentInteractionBar(
    reactionCount: Int,
    userReaction: ReactionType?,
    onReactClick: () -> Unit,          // short press -> default reaction (like)
    onReactLongPress: () -> Unit,      // long press -> show reaction picker
    onPositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    val isLiked = userReaction == ReactionType.LIKE
    val icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    val tint = if (isLiked) Color(0xFFEF5350) else Color.Gray

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onReactClick,
                onLongClick = onReactLongPress
            )
            .onGloballyPositioned { coordinates ->
                onPositioned?.invoke(coordinates)
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
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
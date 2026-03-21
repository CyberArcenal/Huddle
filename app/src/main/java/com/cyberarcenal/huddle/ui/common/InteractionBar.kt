package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp

@Composable
fun InteractionBar(
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onLikeClick: () -> Unit,
    onLikeLongPress: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onLikePositioned: (LayoutCoordinates) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heartIcon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        val heartTint = if (isLiked) Color(0xFFEF5350) else Color.Black

        // Like/Reaction Button
        InteractionButton(
            icon = heartIcon,
            label = if (likeCount > 0) likeCount.toString() else "Like",
            tint = heartTint,
            onClick = onLikeClick,
            onLongPress = onLikeLongPress,
            onPositioned = onLikePositioned
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Comment Button
        InteractionButton(
            icon = Icons.Outlined.ChatBubbleOutline,
            label = if (commentCount > 0) commentCount.toString() else "Comment",
            tint = Color.Black,
            onClick = onCommentClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // Share Button
        InteractionButton(
            icon = Icons.Outlined.Share,
            label = "Share", // Pwede ring "" kung icon lang ang gusto mo
            tint = Color.Black,
            onClick = onShareClick
        )
    }
}
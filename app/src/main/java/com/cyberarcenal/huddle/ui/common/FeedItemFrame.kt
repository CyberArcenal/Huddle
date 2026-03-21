package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.ui.feed.components.Avatar
import com.cyberarcenal.huddle.ui.comments.formatRelativeTime
import java.time.OffsetDateTime

/**
 * Main Shell/Frame for all feed items.
 * Handles Header, Interaction Bar, and Reaction Logic.
 */
@Composable
fun FeedItemFrame(
    user: com.cyberarcenal.huddle.api.models.UserMinimal?,
    createdAt: Any?, // Can be String or OffsetDateTime
    statistics: PostStatsSerializers?,
    headerSuffix: String = "",
    onReactionClick: (ReactionType?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val stats = statistics
    val initialLiked = stats?.liked ?: false
    val initialLikeCount = stats?.likeCount ?: 0
    val commentCount = stats?.commentCount ?: 0

    var isLiked by remember { mutableStateOf(initialLiked) }
    var likeCount by remember { mutableIntStateOf(initialLikeCount) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var reactionPickerOffset by remember { mutableStateOf<IntOffset?>(null) }

    // Sync state when data changes
    LaunchedEffect(statistics) {
        isLiked = initialLiked
        likeCount = initialLikeCount
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 4.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable { user?.id?.let { onProfileClick(it) } }) {
                Avatar(url = user?.profilePictureUrl, username = user?.username)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.username ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val timeLabel = when(createdAt) {
                    is OffsetDateTime -> formatRelativeTime(createdAt)
                    is String -> createdAt // Already formatted
                    else -> ""
                }
                Text(
                    text = "$timeLabel $headerSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
            }
        }

        // --- CONTENT SLOT ---
        content()

        // --- INTERACTION BAR ---
        InteractionBar(
            isLiked = isLiked,
            likeCount = likeCount,
            commentCount = commentCount,
            onLikeClick = {
                isLiked = !isLiked
                if (isLiked) likeCount++ else if (likeCount > 0) likeCount--
                onReactionClick(if (isLiked) ReactionType.LIKE else null)
            },
            onLikeLongPress = { showReactionPicker = true },
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            onLikePositioned = { coords ->
                val pos = coords.positionInWindow()
                reactionPickerOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
            }
        )

        if (showReactionPicker && reactionPickerOffset != null) {
            ReactionPicker(
                anchorOffset = reactionPickerOffset!!,
                onReactionSelected = { type ->
                    onReactionClick(type)
                    showReactionPicker = false
                },
                onDismiss = { showReactionPicker = false }
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
    }
}
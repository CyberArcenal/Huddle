// CommentItem.kt (final)
package com.cyberarcenal.huddle.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.CommentDisplay
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.api.models.UserReactionA51Enum
import com.cyberarcenal.huddle.ui.comments.components.CommentInteractionBar
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Helper to map UserReactionA51Enum to ReactionType
fun mapUserReaction(userReaction: UserReactionA51Enum?): ReactionType? {
    return when (userReaction) {
        UserReactionA51Enum.LIKE -> ReactionType.LIKE
        UserReactionA51Enum.LOVE -> ReactionType.LOVE
        UserReactionA51Enum.CARE -> ReactionType.CARE
        UserReactionA51Enum.HAHA -> ReactionType.HAHA
        UserReactionA51Enum.WOW -> ReactionType.WOW
        UserReactionA51Enum.SAD -> ReactionType.SAD
        UserReactionA51Enum.ANGRY -> ReactionType.ANGRY
        null -> null
    }
}

@Composable
fun CommentItem(
    comment: CommentDisplay,
    replies: List<CommentDisplay>,
    isExpanded: Boolean,
    currentUserId: Int?,
    onToggleExpand: () -> Unit,
    onReact: (Int, ReactionType?) -> Unit,
    onReplyClick: (String) -> Unit,
    onReport: () -> Unit,
    level: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 4.dp)
    ) {
        // Thread line logic
        if (level > 0) {
            Spacer(modifier = Modifier.width((level * 32).dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                val profileUrl = comment.user?.profilePictureUrl
                if (!profileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comment.user?.username?.take(1)?.uppercase() ?: "?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comment.user?.username ?: "Unknown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTime(comment.createdAt),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Interaction Row
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reply",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.clickable {
                                onReplyClick(comment.user?.username ?: "user")
                            }
                        )

                        // Use the updated CommentInteractionBar

                        CommentInteractionBar(
                            reactionCount = comment.likeCount ?: 0,
                            userReaction = mapUserReaction(comment.userReaction),
                            onReact = { newReaction ->
                                onReact(comment.id!!, newReaction)
                            }
                        )
                    }
                }
            }

            // Replies
            if (isExpanded && replies.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    replies.forEach { reply ->
                        CommentItem(
                            comment = reply,
                            replies = emptyList(),
                            isExpanded = false,
                            currentUserId = currentUserId,
                            onToggleExpand = {},
                            onReplyClick = onReplyClick,
                            onReport = onReport,
                            level = level + 1,
                            onReact = onReact
                        )
                    }
                }
            } else if (replies.isNotEmpty()) {
                Text(
                    text = "—— View ${replies.size} more replies",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(start = 42.dp, top = 8.dp)
                        .clickable { onToggleExpand() }
                )
            }
        }
    }
}

fun formatRelativeTime(dateTime: OffsetDateTime?): String {
    val now = OffsetDateTime.now(ZoneId.systemDefault())
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    val hours = ChronoUnit.HOURS.between(dateTime, now)
    val days = ChronoUnit.DAYS.between(dateTime, now)

    return when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "now"
    }
}
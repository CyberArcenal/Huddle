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
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.ui.comments.components.CommentInteractionBar
import com.cyberarcenal.huddle.utils.formatRelativeTime

@Composable
fun CommentItem(
    comment: CommentDisplay,
    replies: List<CommentDisplay>,
    isExpanded: Boolean,
    currentUserId: Int?,
    onToggleExpand: () -> Unit,
    onReact: (Int, ReactionTypeEnum?) -> Unit,
    onReplyClick: (String) -> Unit,
    onReport: () -> Unit,
    level: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp)
    ) {
        // Thread line logic
        if (level > 0) {
            Spacer(modifier = Modifier.width((level * 32).dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comment.user?.username?.take(1)?.uppercase() ?: "?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTime(comment.createdAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                onReplyClick(comment.user?.username ?: "user")
                            }
                        )

                        // Use the updated CommentInteractionBar

                        CommentInteractionBar(
                            statistics = comment.statistics,
                            onReactionSelected = { newReaction ->
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 42.dp, top = 8.dp)
                        .clickable { onToggleExpand() }
                )
            }
        }
    }
}
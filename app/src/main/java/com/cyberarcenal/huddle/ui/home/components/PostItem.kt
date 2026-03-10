package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PostFeed
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun PostItem(
    post: PostFeed,
    onLikeClick: (currentLiked: Boolean, currentCount: Int) -> Unit,
    onCommentClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Extract values from the statistics map safely
    val initialLiked = (post.statistics["has_liked"] as? Boolean) ?: false
    val initialLikeCount = (post.statistics["like_count"] as? Number)?.toInt() ?: 0
    val commentCount = (post.statistics["comment_count"] as? Number)?.toInt() ?: 0

    var isLiked by remember { mutableStateOf(initialLiked) }
    var likeCount by remember { mutableStateOf(initialLikeCount) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header: avatar, username, timestamp, more button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.user?.profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = post.user?.username ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }

            // Post content (text)
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Media (image/video)
            post.mediaUrl?.let { url ->
                AsyncImage(
                    model = url.toString(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { /* open post detail */ },
                    contentScale = ContentScale.Crop
                )
            }

            // Action buttons row – refined with smaller icons and tighter spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button with count
                ActionItem(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    count = likeCount,
                    tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        // Optimistic update
                        isLiked = !isLiked
                        likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                        onLikeClick(isLiked, likeCount)
                    }
                )

                // Comment button with count
                ActionItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comment",
                    count = commentCount,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onCommentClick
                )

                // Share button (no count)
                ActionItem(
                    icon = Icons.Default.Share,
                    contentDescription = "Share",
                    count = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = { /* Share */ }
                )
            }
        }
    }
}

/**
 * Reusable component for action items (icon + optional count).
 * Icons are 18.dp, with a clickable area of 32.dp for easy tapping.
 */
@Composable
fun ActionItem(
    icon: ImageVector,
    contentDescription: String,
    count: Int?,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(32.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = tint
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatRelativeTime(dateTime: OffsetDateTime): String {
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
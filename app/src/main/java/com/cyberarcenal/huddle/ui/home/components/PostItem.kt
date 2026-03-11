package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Safely extract statistics
    val stats = post.statistics
    val initialLiked = when (val value = stats["liked"]) {
        is Boolean -> value
        is Number -> value.toInt() == 1
        else -> false
    }
    val initialLikeCount = (stats["like_count"] as? Number)?.toInt() ?: 0
    val commentCount = (stats["comment_count"] as? Number)?.toInt() ?: 0

    var isLiked by remember(post.id) { mutableStateOf(initialLiked) }
    var likeCount by remember(post.id) { mutableStateOf(initialLikeCount) }

    // Sync state if post updates
    LaunchedEffect(post.id, initialLiked, initialLikeCount) {
        isLiked = initialLiked
        likeCount = initialLikeCount
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val profileUrl = post.user?.profilePictureUrl

                if (!profileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onProfileClick() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = post.user?.username?.take(1)?.uppercase() ?: "?"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.user?.username ?: "User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Post content text
            if (!post.content.isNullOrBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 18.sp,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // Multi‑media display
            val mediaList = post.media
            if (!mediaList.isNullOrEmpty()) {
                // Show the first media item
                val firstMedia = mediaList.first()
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(firstMedia.fileUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post Media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .aspectRatio(1.2f)
                        .clickable { /* open detail view with gallery */ },
                    contentScale = ContentScale.Crop
                )

                // Badge for additional media
                if (mediaList.size > 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "1/${mediaList.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Interaction Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                val heartIcon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                val heartTint = if (isLiked) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant

                InteractionButton(
                    icon = heartIcon,
                    label = likeCount.toString(),
                    tint = heartTint,
                    onClick = {
                        // Optimistic Update
                        isLiked = !isLiked
                        likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                        onLikeClick(isLiked, likeCount)
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Comment Button
                InteractionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = commentCount.toString(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onCommentClick
                )

                Spacer(modifier = Modifier.weight(1f))

                // Share Button
                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InteractionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tint
        )
        if (label != "0") {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = tint,
                fontSize = 11.sp
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
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}
package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.ui.comments.formatRelativeTime

@Composable
fun ReelCard(
    reel: ReelDisplay,
    onReelClick: (reelId: Int) -> Unit,
    onReactionClick: (reelId: Int, reactionType: ReactionType?) -> Unit,
    onCommentClick: (reelId: Int) -> Unit,
    onProfileClick: (userId: Int) -> Unit
) {
    val reelId = reel.id ?: return
    val user = reel.user ?: return
    val userId = user.id ?: return
    val timestamp = reel.createdAt ?: java.time.OffsetDateTime.now()
    val isLiked = reel.hasLiked ?: false
    val likeCount = reel.likeCount ?: 0
    val commentCount = reel.commentCount ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onReelClick(reelId) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Avatar + Username + Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    url = user.profilePictureUrl,
                    username = user.username ?: user.fullName ?: "User",
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onProfileClick(userId) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.username ?: user.fullName ?: "User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onProfileClick(userId) }
                    )
                    Text(
                        text = formatRelativeTime(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Thumbnail / Video placeholder
            if (!reel.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = reel.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder if no thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Reel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Caption
            if (!reel.caption.isNullOrBlank()) {
                Text(
                    text = reel.caption,
                    modifier = Modifier.padding(12.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Interaction Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                IconButton(
                    onClick = { onReactionClick(reelId, if (isLiked) null else ReactionType.LIKE) }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$likeCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )

                // Comment button
                IconButton(
                    onClick = { onCommentClick(reelId) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment"
                    )
                }
                Text(
                    text = "$commentCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
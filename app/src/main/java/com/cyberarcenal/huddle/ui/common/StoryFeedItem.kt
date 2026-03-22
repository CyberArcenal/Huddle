package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.ui.comments.formatRelativeTime
import java.time.OffsetDateTime

@Composable
fun StoryCard(
    story: Story,
    onStoryClick: (userId: Int) -> Unit
) {
    val user = story.user ?: return
    val userId = user.id ?: return
    val timestamp = story.createdAt ?: OffsetDateTime.now()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onStoryClick(userId) },
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
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user.username ?: user.fullName ?: "User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRelativeTime(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content: depending on story type
            when (story.storyType) {
                StoryTypeEnum.IMAGE -> {
                    story.mediaUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        // Fallback if no media
                        Text(
                            text = story.content ?: "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                StoryTypeEnum.VIDEO -> {
                    // For video, we can show a placeholder with a play icon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        // Show a preview image if thumbnail exists, else show an icon
                        if (story.mediaUrl != null) {
                            AsyncImage(
                                model = story.mediaUrl, // This might be the video file; we need a thumbnail.
                                // Better to have a thumbnail field. For now, we'll just show a placeholder.
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Video story", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    // Text story (or any other type)
                    Text(
                        text = story.content ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
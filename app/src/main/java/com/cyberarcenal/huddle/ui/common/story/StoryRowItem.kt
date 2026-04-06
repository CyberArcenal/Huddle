package com.cyberarcenal.huddle.ui.common.story

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.utils.formatRelativeTime
import java.time.OffsetDateTime

/**
 * Modern story item for horizontal scrolling row (Instagram‑style).
 * Shows a circular avatar with a gradient ring for unviewed stories.
 */
@Composable
fun StoryRowItem(
    username: String?,
    profilePictureUrl: String?,
    thumbnailUrl: String? = null,
    hasViewedAll: Boolean = false,
    isMyStory: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        // Circular avatar with ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (!isMyStory && !hasViewedAll) {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF58529), Color(0xFFFEDA77), Color(0xFFDD2A7B))
                        )
                    } else {
                        SolidColor(Color.Transparent)
                    }
                )
                .padding(3.dp) // ring thickness
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface) // use theme surface
                    .padding(2.dp)
            ) {
                if (isMyStory) {
                    // Add Story button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    // User avatar
                    if (!profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username?.take(1)?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface // theme text
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Username (truncated)
        Text(
            text = if (isMyStory) "Your story" else (username ?: "User"),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            color = if (hasViewedAll) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Modern story feed item for the feed (shows full content).
 * Designed as an immersive card with a header and full‑width media.
 */
@Composable
fun StoryFeedItem(
    story: Story,
    onStoryClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                story.id?.let {
                    onStoryClick(it)
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media Content
            if (story.storyType == StoryTypeEnum.IMAGE && story.mediaUrl != null) {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Background for text stories or missing media - More subtle/minimal
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!story.content.isNullOrBlank()) {
                        Text(
                            text = story.content,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }

            // Overlay for content (Gradient is more subtle)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: User Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(1.dp)
                    ) {
                        Avatar(
                            size = 40.dp,
                            url = story.user?.profilePictureUrl,
                            username = story.user?.fullName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column {
                        Text(
                            text = story.user?.username ?: "Unknown",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatRelativeTime(story.createdAt),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Bottom: Content text (Caption for image stories)
                if (story.storyType == StoryTypeEnum.IMAGE && !story.content.isNullOrBlank()) {
                    Text(
                        text = story.content,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


// Previews to demonstrate the design
@Preview(showBackground = true)
@Composable
fun PreviewStoryRow() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // My story
            StoryRowItem(
                username = "you",
                profilePictureUrl = null,
                isMyStory = true,
                onClick = {}
            )
            // Unviewed story
            StoryRowItem(
                username = "maria_clara",
                profilePictureUrl = "https://picsum.photos/200/200?random=1",
                hasViewedAll = false,
                onClick = {}
            )
            // Viewed story
            StoryRowItem(
                username = "juan_delacruz",
                profilePictureUrl = "https://picsum.photos/200/200?random=2",
                hasViewedAll = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStoryFeedItem() {
    MaterialTheme {
        Column {
            // Image story
            StoryFeedItem(
                story = Story(
                    id = 1,
                    user = UserMinimal(
                        id = 1,
                        username = "johndoe",
                        fullName = "John Doe",
                        profilePictureUrl = "https://picsum.photos/200/200?random=3"
                    ),
                    storyType = StoryTypeEnum.IMAGE,
                    content = "Check out this amazing sunset! \uD83C\uDF05",
                    mediaUrl = "https://picsum.photos/800/600?random=4",
                    createdAt = OffsetDateTime.now().minusHours(2)
                ),
                onStoryClick = {}
            )
            // Text story
            StoryFeedItem(
                story = Story(
                    id = 2,
                    user = UserMinimal(
                        id = 2,
                        username = "janedoe",
                        fullName = "Jane Doe",
                        profilePictureUrl = "https://picsum.photos/200/200?random=5"
                    ),
                    storyType = StoryTypeEnum.TEXT,
                    content = "Just a thought...",
                    mediaUrl = null,
                    createdAt = OffsetDateTime.now().minusHours(5)
                ),
                onStoryClick = {}
            )
        }
    }
}
//package com.cyberarcenal.huddle.ui.common.story
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import com.cyberarcenal.huddle.api.models.Story
//import com.cyberarcenal.huddle.api.models.StoryTypeEnum
//import com.cyberarcenal.huddle.ui.common.user.Avatar
//import com.cyberarcenal.huddle.utils.formatRelativeTime
//import java.time.OffsetDateTime
//
//@Composable
//fun StoryFeedItem(
//    story: Story,
//    onStoryClick: (userId: Int) -> Unit
//) {
//    val user = story.user ?: return
//    val userId = user.id ?: return
//    val timestamp = story.createdAt ?: OffsetDateTime.now()
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp, vertical = 6.dp)
//            .clickable { onStoryClick(userId) },
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            // Header: Avatar + Username + Time
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(12.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Avatar(
//                    url = user.profilePictureUrl,
//                    username = user.username ?: user.fullName ?: "User",
//                    modifier = Modifier.size(40.dp)
//                )
//                Spacer(modifier = Modifier.width(12.dp))
//                Column {
//                    Text(
//                        text = user.username ?: user.fullName ?: "User",
//                        style = MaterialTheme.typography.titleSmall,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Text(
//                        text = formatRelativeTime(timestamp),
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            // Content (image/video/text)
//            when (story.storyType) {
//                StoryTypeEnum.IMAGE -> {
//                    story.mediaUrl?.let { url ->
//                        AsyncImage(
//                            model = url,
//                            contentDescription = null,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(320.dp)
//                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
//                            contentScale = ContentScale.Crop
//                        )
//                    } ?: run {
//                        Text(
//                            text = story.content ?: "",
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp),
//                            maxLines = 5,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                }
//                StoryTypeEnum.VIDEO -> {
//                    // For video, show a thumbnail with a play overlay
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(320.dp)
//                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
//                            .background(MaterialTheme.colorScheme.surfaceVariant),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        if (story.mediaUrl != null) {
//                            // Use the media URL as thumbnail (if it's an image) or show a placeholder
//                            // Ideally you'd have a thumbnail field; here we use the media URL.
//                            AsyncImage(
//                                model = story.mediaUrl,
//                                contentDescription = null,
//                                modifier = Modifier.fillMaxSize(),
//                                contentScale = ContentScale.Crop
//                            )
//                            // Play icon overlay
//                            Icon(
//                                imageVector = Icons.Default.PlayArrow,
//                                contentDescription = "Play",
//                                tint = Color.White,
//                                modifier = Modifier.size(48.dp)
//                            )
//                        } else {
//                            Text("Video story", color = MaterialTheme.colorScheme.onSurfaceVariant)
//                        }
//                    }
//                }
//                else -> {
//                    // Text story
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(MaterialTheme.colorScheme.surfaceVariant)
//                            .padding(20.dp)
//                    ) {
//                        Text(
//                            text = story.content ?: "",
//                            style = MaterialTheme.typography.bodyLarge,
//                            maxLines = 8,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
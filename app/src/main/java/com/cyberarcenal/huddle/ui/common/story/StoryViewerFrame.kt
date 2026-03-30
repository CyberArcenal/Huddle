package com.cyberarcenal.huddle.ui.common.story

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.ui.common.feed.ReactionSummary
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.utils.formatRelativeTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerFrame(
    story: Story,
    totalStories: Int,
    currentIndex: Int,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onMoreClick: () -> Unit,
    onCommentClick: () -> Unit,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onProfileClick: (Int) -> Unit,
    onAddToHighlightClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = story.user
    val createdAt = story.createdAt
    val timeLabel = createdAt?.let { formatRelativeTime(it) } ?: ""
    val statistics = story.statistics
    val reactionCount = statistics?.reactionCount
    val commentCount = statistics?.commentCount ?: 0
    val isAuthor = statistics?.isAuthor == true

    // Current story progress state (0.0 to 1.0)
    var currentProgress by remember(story.id) { mutableFloatStateOf(0f) }
    
    val isVideo = story.storyType == StoryTypeEnum.VIDEO

    // Timer for non-video stories
    if (!isVideo) {
        val storyDuration = 10000 // 10 seconds for images and text
        LaunchedEffect(story.id) {
            val startTime = System.currentTimeMillis()
            while (currentProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                currentProgress = (elapsed.toFloat() / storyDuration).coerceIn(0f, 1f)
                if (currentProgress >= 1f) {
                    onTapRight()
                }
                delay(16) // Smooth progress update
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Media content
        when (story.storyType) {
            StoryTypeEnum.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(story.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            StoryTypeEnum.VIDEO -> {
                story.mediaUrl?.let { url ->
                    StoryVideoPlayer(
                        videoUrl = url,
                        isPlaying = true,
                        onVideoFinished = {
                            onTapRight()
                        },
                        onProgressUpdate = { progress ->
                            currentProgress = progress
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            StoryTypeEnum.TEXT -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF212121)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.content ?: "",
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

        // Tap zones (left/right) – placed behind the overlay
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTapLeft() })
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTapRight() })
        }

        // Overlay (progress bars, user info, interaction bar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: progress bars + user info
            Column {
                // Progress bars (Instagram style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(totalStories) { index ->
                        LinearProgressIndicator(
                            progress = { 
                                when {
                                    index == currentIndex -> currentProgress
                                    index < currentIndex -> 1f
                                    else -> 0f
                                }
                            },
                            modifier = Modifier.weight(1f).height(2.dp).clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User info and more button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.clickable { user?.id?.let { onProfileClick(it) } }) {
                        Avatar(
                            url = user?.profilePictureUrl,
                            username = user?.fullName ?: user?.username,
                            size = 40.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user?.fullName ?: user?.username ?: "User",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = timeLabel,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Add to Highlight Button (Only for author)
                    if (isAuthor && onAddToHighlightClick != null) {
                        IconButton(onClick = onAddToHighlightClick) {
                            Icon(
                                imageVector = if (isHighlighted) Icons.Default.AutoAwesome else Icons.Default.AddCircleOutline,
                                contentDescription = "Highlight",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }

            // Bottom section: stats, comments, interaction bar
            Column {
                // Comment Preview Pop-up bubble
                if (commentCount > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clickable { onCommentClick() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$commentCount comments",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Story Interaction bar
                StoryInteractionBar(
                    currentReaction = mapCurrentReaction(statistics?.currentReaction),
                    onReactionClick = onReactionClick,
                    onCommentClick = onCommentClick,
                    onShareClick = {
                        val shareData = ShareRequestData(
                            contentType = "story",
                            contentId = story.id ?: 0,
                            caption = null,
                            privacy = PrivacyB23Enum.PUBLIC,
                            groupId = null
                        )
                        onShareClick(shareData)
                    },
                    isOwnStory = isAuthor
                )
            }
        }
    }
}

@Composable
private fun StoryInteractionBar(
    currentReaction: ReactionTypeEnum?,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    isOwnStory: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Send message / Comment input
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                .clickable { onCommentClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (isOwnStory) "View comments..." else "Send message...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Reaction Button
        val isLiked = currentReaction == ReactionTypeEnum.LIKE
        IconButton(onClick = {
            onReactionClick(if (isLiked) null else ReactionTypeEnum.LIKE)
        }) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) Color.Red else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Share Button
        IconButton(onClick = onShareClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// Helper functions
private fun getTotalReactionCount(reactionCount: ReactionCount?): Int {
    if (reactionCount == null) return 0
    return (reactionCount.like ?: 0) +
            (reactionCount.love ?: 0) +
            (reactionCount.care ?: 0) +
            (reactionCount.haha ?: 0) +
            (reactionCount.wow ?: 0) +
            (reactionCount.sad ?: 0) +
            (reactionCount.angry ?: 0)
}

private fun mapCurrentReaction(currentReaction: String?): ReactionTypeEnum? {
    return when (currentReaction?.lowercase()) {
        "like" -> ReactionTypeEnum.LIKE
        "love" -> ReactionTypeEnum.LOVE
        "care" -> ReactionTypeEnum.CARE
        "haha" -> ReactionTypeEnum.HAHA
        "wow" -> ReactionTypeEnum.WOW
        "sad" -> ReactionTypeEnum.SAD
        "angry" -> ReactionTypeEnum.ANGRY
        else -> null
    }
}

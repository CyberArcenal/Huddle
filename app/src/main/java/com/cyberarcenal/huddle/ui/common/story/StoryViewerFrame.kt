package com.cyberarcenal.huddle.ui.common.story

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.ui.common.feed.InteractionBar
import com.cyberarcenal.huddle.ui.common.feed.ReactionSummary
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.utils.formatRelativeTime

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
    onReactionClick: (ReactionCreateRequest.ReactionType?) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onProfileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = story.user
    val createdAt = story.createdAt
    val timeLabel = createdAt?.let { formatRelativeTime(it) } ?: ""
    val statistics = story.statistics
    val reactionCount = statistics?.reactionCount
    val commentCount = statistics?.commentCount ?: 0

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
                        val animatedProgress = remember { Animatable(0f) }
                        LaunchedEffect(currentIndex) {
                            if (index == currentIndex) {
                                animatedProgress.snapTo(0f)
                                animatedProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 10000)
                                )
                            } else {
                                animatedProgress.snapTo(if (index < currentIndex) 1f else 0f)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { if (index == currentIndex) animatedProgress.value else if (index < currentIndex) 1f else 0f },
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
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }

            // Bottom section: stats, comments, interaction bar
            Column {
                // Reaction summary (if any)
                if (reactionCount != null && getTotalReactionCount(reactionCount) > 0) {
                    ReactionSummary(reactionCount = reactionCount, textColor = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Interaction bar
                InteractionBar(
                    currentReaction = mapCurrentReaction(statistics?.currentReaction),
                    reactionCount = getTotalReactionCount(reactionCount),
                    commentCount = commentCount,
                    onReactionSelected = { reactionType -> onReactionClick(reactionType) },
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
                    }
                )
            }
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

private fun mapCurrentReaction(currentReaction: String?): ReactionCreateRequest.ReactionType? {
    return when (currentReaction?.lowercase()) {
        "like" -> ReactionCreateRequest.ReactionType.LIKE
        "love" -> ReactionCreateRequest.ReactionType.LOVE
        "care" -> ReactionCreateRequest.ReactionType.CARE
        "haha" -> ReactionCreateRequest.ReactionType.HAHA
        "wow" -> ReactionCreateRequest.ReactionType.WOW
        "sad" -> ReactionCreateRequest.ReactionType.SAD
        "angry" -> ReactionCreateRequest.ReactionType.ANGRY
        else -> null
    }
}
// StoryViewerFrame.kt (updated)

package com.cyberarcenal.huddle.ui.common.story

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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
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
    onCommentClick: (String, Int, stats: PostStatsSerializers) -> Unit,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onProfileClick: (Int) -> Unit,
    onAddToHighlightClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,          // new: save/bookmark
    onReportClick: (() -> Unit)? = null,        // new: report story
    onShareAsReelClick: (() -> Unit)? = null,   // new: share as reel
    isHighlighted: Boolean = false,
    isSaved: Boolean = false,                   // new: saved state
    isPaused: Boolean = false,                  // new: external pause control
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = story.user
    val createdAt = story.createdAt
    val timeLabel = createdAt?.let { formatRelativeTime(it) } ?: ""
    val statistics = story.statistics
    val commentCount = statistics?.commentCount ?: 0
    val isAuthor = statistics?.isAuthor == true

    // Progress state
    var currentProgress by remember(story.id) { mutableFloatStateOf(0f) }
    val isVideo = story.storyType == StoryTypeEnum.VIDEO

    // Volume state for video
    var isMuted by remember(story.id) { mutableStateOf(true) } // default muted for auto-play

    // Timer for non-video stories
    if (!isVideo) {
        val storyDuration = 10000
        LaunchedEffect(story.id, isPaused) {
            if (isPaused) return@LaunchedEffect
            val startTime = System.currentTimeMillis() - (currentProgress * storyDuration).toLong()
            while (currentProgress < 1f) {
                if (isPaused) break
                val elapsed = System.currentTimeMillis() - startTime
                currentProgress = (elapsed.toFloat() / storyDuration).coerceIn(0f, 1f)
                if (currentProgress >= 1f) {
                    onTapRight()
                }
                delay(16)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Media content
        when (story.storyType) {
            StoryTypeEnum.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(story.mediaUrl).crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            StoryTypeEnum.VIDEO -> {
                story.mediaUrl?.let { url ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        StoryVideoPlayer(
                            videoUrl = url,
                            isPlaying = !isPaused,
                            isMuted = isMuted,      // pass mute state
                            onVideoFinished = { onTapRight() },
                            onProgressUpdate = { progress -> currentProgress = progress },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Volume toggle button overlay (bottom right)
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape).size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }
                    }
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

        // Tap zones
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTapLeft() })
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTapRight() })
        }

        // Overlay
        Column(
            modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ), startY = 0f, endY = Float.POSITIVE_INFINITY
                    )
                ).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section
            Column {
                // Progress bars
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

                // User info and action buttons
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

                    // Add to Highlight (author only)
                    if (isAuthor && onAddToHighlightClick != null) {
                        IconButton(onClick = onAddToHighlightClick) {
                            Icon(
                                imageVector = if (isHighlighted) Icons.Default.AutoAwesome else Icons.Default.AddCircleOutline,
                                contentDescription = "Highlight",
                                tint = Color.White
                            )
                        }
                    }

                    // More options button (includes Report and Share as Reel)
                    IconButton(onClick = onMoreClick) {
                        Icon(
                            Icons.Default.MoreVert, contentDescription = "More", tint = Color.White
                        )
                    }
                }
            }

            // Bottom section
            Column {
                // Comment preview bubble
                if (commentCount > 0) {
                    Surface(
                        modifier = Modifier.padding(bottom = 12.dp)
                            .clickable { onCommentClick("story", story.id ?: 0, statistics!!) },
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

                // Enhanced interaction bar
                StoryInteractionBar(
                    story = story,
                    currentReaction = mapCurrentReaction(statistics?.currentReaction),
                    onReactionClick = onReactionClick,
                    onCommentClick = { onCommentClick("story", story.id ?: 0, statistics!!) },
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
                    onSaveClick = onSaveClick,
                    isSaved = isSaved,
                    isOwnStory = isAuthor
                )
            }
        }
    }
}

@Composable
private fun StoryInteractionBar(
    story: Story,
    currentReaction: ReactionTypeEnum?,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: (() -> Unit)?,
    isSaved: Boolean,
    isOwnStory: Boolean
) {
    val reactionItems = remember {
        listOf(
            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", painterResource = R.drawable.like),
            Reaction(key = ReactionTypeEnum.DISLIKE, label = "Dislike", painterResource = R.drawable.dislike_svgrepo_com),
            Reaction(key = ReactionTypeEnum.LOVE, label = "Love", painterResource = R.drawable.love),
            Reaction(key = ReactionTypeEnum.CARE, label = "Care", painterResource = R.drawable.care),
            Reaction(key = ReactionTypeEnum.HAHA, label = "Haha", painterResource = R.drawable.haha),
            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", painterResource = R.drawable.wow),
            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", painterResource = R.drawable.sad),
            Reaction(key = ReactionTypeEnum.ANGRY, label = "Angry", painterResource = R.drawable.angry),
        )
    }

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == currentReaction }
    )

    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionTypeEnum
        if (selectedKey != currentReaction && selectedKey != null) {
            onReactionClick(
                ReactionCreateRequest(
                    contentType = "story",
                    objectId = story.id ?: 0,
                    reactionType = selectedKey
                )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Comment input box
        Box(
            modifier = Modifier.weight(1f).height(44.dp).clip(CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable { onCommentClick() }.padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isOwnStory) "View comments..." else "Send message...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Reaction button
        val isLiked = currentReaction != null
        IconButton(
            onClick = {
                onReactionClick(
                    ReactionCreateRequest(
                        contentType = "story",
                        objectId = story.id ?: 0,
                        reactionType = if (isLiked) null else ReactionTypeEnum.LIKE
                    )
                )
            },
            modifier = Modifier.reactionPickerAnchor(pickerState)
        ) {
            if (currentReaction != null) {
                val (icon, _) = com.cyberarcenal.huddle.ui.common.feed.getReactionIcon(currentReaction)
                if (icon is Int) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Save/Bookmark button
        if (onSaveClick != null) {
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isSaved) "Unsave" else "Save",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Share button (internal)
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
    return (reactionCount.like ?: 0) + (reactionCount.love ?: 0) + (reactionCount.care
        ?: 0) + (reactionCount.haha ?: 0) + (reactionCount.wow ?: 0) + (reactionCount.sad
        ?: 0) + (reactionCount.angry ?: 0)
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
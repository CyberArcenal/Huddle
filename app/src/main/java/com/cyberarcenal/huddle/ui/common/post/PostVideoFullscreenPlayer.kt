package com.cyberarcenal.huddle.ui.common.post

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.data.reactionPicker.ReactionPickerLayout
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.data.videoPlayer.LocalVideoPlayerManager
import com.cyberarcenal.huddle.data.videoPlayer.VideoAnchor
import com.cyberarcenal.huddle.ui.common.feed.getReactionIcon
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.reel.feed.components.ReelActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PostVideoFullscreenPlayer(
    post: PostFeed,
    videoUrl: String,
    onDismiss: () -> Unit,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val videoManager = LocalVideoPlayerManager.current
    val isPlaying by videoManager.isPlaying.collectAsState()
    var showPlayIcon by remember { mutableStateOf(false) }
    var showFullCaption by remember { mutableStateOf(false) }

    DisposableEffect(videoUrl) {
        videoManager.setExternalControl(videoUrl, true)
        onDispose {
            videoManager.setExternalControl(videoUrl, false)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ReactionPickerLayout(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoAnchor(
                    videoUrl = videoUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (videoManager.isPlaying.value) {
                                videoManager.pause()
                            } else {
                                videoManager.resume()
                            }
                            coroutineScope.launch {
                                showPlayIcon = true
                                delay(500)
                                showPlayIcon = false
                            }
                        },
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                )

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Play Icon Overlay
                AnimatedVisibility(
                    visible = !isPlaying || showPlayIcon,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                }

                // Right side action buttons
                PostVideoActions(
                    post = post,
                    onReactionClick = onReactionClick,
                    onCommentClick = onCommentClick,
                    onShareClick = onShareClick
                )

                // Bottom info (Profile, Caption)
                PostVideoInfo(
                    post = post,
                    onProfileClick = onProfileClick,
                    onCaptionClick = { showFullCaption = true }
                )

                if (showFullCaption) {
                    FullCaptionBottomSheet(
                        caption = post.content,
                        onDismiss = { showFullCaption = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PostVideoActions(
    post: PostFeed,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val stats = post.statistics
    
    // Logic for reactions (similar to ReelOverlay)
    val currentReaction = remember(stats?.currentReaction) {
        when (stats?.currentReaction?.lowercase()) {
            "like" -> ReactionTypeEnum.LIKE
            "dislike" -> ReactionTypeEnum.DISLIKE
            "love" -> ReactionTypeEnum.LOVE
            "care" -> ReactionTypeEnum.CARE
            "haha" -> ReactionTypeEnum.HAHA
            "wow" -> ReactionTypeEnum.WOW
            "sad" -> ReactionTypeEnum.SAD
            "angry" -> ReactionTypeEnum.ANGRY
            else -> null
        }
    }

    var localReaction by remember { mutableStateOf(currentReaction) }
    var localLikeCount by remember { mutableIntStateOf(stats?.likeCount ?: 0) }

    val reactionItems = remember {
        listOf(
            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", painterResource = R.drawable.like),
            Reaction(key = ReactionTypeEnum.DISLIKE, label = "Dislike", painterResource = R.drawable.dislike),
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
        initialSelection = reactionItems.find { it.key == localReaction }
    )

    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionTypeEnum
        if (selectedKey != localReaction) {
            val hadReaction = localReaction != null
            val willHaveReaction = selectedKey != null
            if (!hadReaction && willHaveReaction) localLikeCount++
            else if (hadReaction && !willHaveReaction) localLikeCount--
            localReaction = selectedKey
            onReactionClick(selectedKey)
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 12.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val (iconData, _) = getReactionIcon(localReaction)

        ReelActionButton(
            icon = iconData,
            label = "$localLikeCount",
            modifier = Modifier.reactionPickerAnchor(pickerState),
            tint = if (localReaction != null) Color.Unspecified else Color.White,
            onClick = {
                val next = if (localReaction != null) null else ReactionTypeEnum.LIKE
                if (next != localReaction) {
                     if (localReaction == null) localLikeCount++ else localLikeCount--
                     localReaction = next
                     onReactionClick(next)
                }
            }
        )

        ReelActionButton(
            icon = R.drawable.comment,
            label = "${stats?.commentCount ?: 0}",
            tint = Color.White,
            onClick = onCommentClick
        )

        ReelActionButton(
            icon = R.drawable.share_ios,
            label = "Share",
            tint = Color.White,
            onClick = onShareClick
        )
    }
}

@Composable
private fun BoxScope.PostVideoInfo(
    post: PostFeed,
    onProfileClick: (Int) -> Unit,
    onCaptionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                )
            )
            .padding(start = 16.dp, bottom = 32.dp, end = 80.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { post.user?.id?.let { onProfileClick(it) } }
        ) {
            Avatar(
                url = post.user?.profilePictureUrl,
                username = post.user?.username,
                modifier = Modifier.size(36.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = post.user?.fullName ?: post.user?.username ?: "Unknown",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (post.content.isNotBlank()) {
            Text(
                text = post.content,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                modifier = Modifier.clickable { onCaptionClick() }
            )
        }
    }
}


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullCaptionBottomSheet(
    caption: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Caption",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            );
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            );
        };
    };
};
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
import com.cyberarcenal.huddle.ui.common.feed.ShareBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.feed.getReactionIcon
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.reel.feed.components.ReelActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostVideoFullscreenPlayer(
    post: PostFeed,
    videoUrl: String,
    onDismiss: () -> Unit,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val videoManager = LocalVideoPlayerManager.current
    val isPlaying by videoManager.isPlaying.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showFullCaption by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    val currentPosition by videoManager.currentPosition.collectAsState()
    val duration by videoManager.duration.collectAsState()

    // Auto-hide controls (mas matagal para hindi nakakainis)
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    DisposableEffect(videoUrl) {
        videoManager.setExternalControl(videoUrl, true)
        onDispose { videoManager.setExternalControl(videoUrl, false) }
    }

    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false
        )
    ) {
        ReactionPickerLayout(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).navigationBarsPadding()
            ) {
                // Video layer (tapping on video toggles controls)
                Box(
                    modifier = Modifier.fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showControls = !showControls }) {
                    VideoAnchor(
                        videoUrl = videoUrl,
                        modifier = Modifier.fillMaxSize(),
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        showMuteButton = false,
                        isExternalControl = true,
                        isPaused = false
                    )
                }

                // Top Controls (Close and Mute)
                Row(
                    modifier = Modifier.statusBarsPadding().fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }



                    IconButton(
                        onClick = onMoreClick
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.statusBarsPadding().fillMaxWidth()
                        .padding(0.dp, 60.dp, 13.dp, 0.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.cyberarcenal.huddle.data.videoPlayer.MuteButton()
                }

                // Controls overlay (fade animation)
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        // Center transport controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    videoManager.seekTo(
                                        (currentPosition - 10000).coerceAtLeast(
                                            0
                                        )
                                    )
                                }, modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Replay10,
                                    contentDescription = "Back 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) videoManager.pause() else videoManager.resume()
                                        // Show controls ulit after manual play/pause
                                        showControls = true
                                    }, modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    videoManager.seekTo(
                                        (currentPosition + 10000).coerceAtMost(
                                            duration
                                        )
                                    )
                                }, modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Right side action buttons (mas maganda ang spacing)
                        PostVideoActions(
                            post = post,
                            onReactionClick = onReactionClick,
                            onCommentClick = onCommentClick,
                            onShareClick = { showShareSheet = true },
                            onMoreClick = onMoreClick
                        )

                        // Bottom info (profile + caption)
                        PostVideoInfo(
                            post = post,
                            onProfileClick = onProfileClick,
                            onCaptionClick = { showFullCaption = true })

                        // Progress slider - nasa ibabaw ng info pero hindi natatakpan
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Slider(
                                value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(
                                    0f, 1f
                                ) else 0f,
                                onValueChange = { newProgress ->
                                    if (duration > 0) {
                                        videoManager.seekTo((newProgress * duration).toLong())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }

                // Full caption bottom sheet
                if (showFullCaption) {
                    FullCaptionBottomSheet(
                        caption = post.content, onDismiss = { showFullCaption = false })
                }

                if (showShareSheet) {
                    ShareBottomSheet(
                        onDismiss = { showShareSheet = false },
                        onShare = { shareData ->
                            showShareSheet = false
                            onShareClick(shareData)
                        },
                        contentType = "feed.post",
                        contentId = post.id ?: 0
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
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val stats = post.statistics
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
            Reaction(
                key = ReactionTypeEnum.LIKE, label = "Like", painterResource = R.drawable.like
            ),
            Reaction(
                key = ReactionTypeEnum.DISLIKE,
                label = "Dislike",
                painterResource = R.drawable.dislike
            ),
            Reaction(
                key = ReactionTypeEnum.LOVE, label = "Love", painterResource = R.drawable.love
            ),
            Reaction(
                key = ReactionTypeEnum.CARE, label = "Care", painterResource = R.drawable.care
            ),
            Reaction(
                key = ReactionTypeEnum.HAHA, label = "Haha", painterResource = R.drawable.haha
            ),
            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", painterResource = R.drawable.wow),
            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", painterResource = R.drawable.sad),
            Reaction(
                key = ReactionTypeEnum.ANGRY, label = "Angry", painterResource = R.drawable.angry
            ),
        )
    }

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == localReaction })

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
        modifier = Modifier.align(Alignment.BottomEnd).padding(
                end = 16.dp, bottom = 80.dp
            ), // dinagdagan ang bottom para hindi dumikit sa slider
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val (iconData, _) = getReactionIcon(localReaction)

        ReelActionButton(
            icon = iconData,
            label = localLikeCount.toString(),
            modifier = Modifier.reactionPickerAnchor(pickerState),
            tint = if (localReaction != null) Color.Unspecified else Color.White,
            onClick = {
                val next = if (localReaction != null) null else ReactionTypeEnum.LIKE
                if (next != localReaction) {
                    if (localReaction == null) localLikeCount++ else localLikeCount--
                    localReaction = next
                    onReactionClick(next)
                }
            })

        ReelActionButton(
            icon = R.drawable.comment,
            label = "${stats?.commentCount ?: 0}",
            tint = Color.White,
            onClick = onCommentClick
        )

        ReelActionButton(
            icon = R.drawable.share_ios, label = "Share", tint = Color.White, onClick = onShareClick
        )
    }
}

@Composable
private fun BoxScope.PostVideoInfo(
    post: PostFeed, onProfileClick: (Int) -> Unit, onCaptionClick: () -> Unit
) {
    Column(
        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                )
            ).padding(
                start = 16.dp, bottom = 70.dp, end = 80.dp
            ) // dinagdagan ang bottom para hindi matabunan ng slider
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { post.user?.id?.let { onProfileClick(it) } }) {
            Avatar(
                url = post.user?.profilePictureUrl,
                username = post.user?.username,
                modifier = Modifier.size(40.dp).clip(CircleShape)
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
                modifier = Modifier.clickable { onCaptionClick() })
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullCaptionBottomSheet(
    caption: String, onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)
        ) {
            Text(
                text = "Caption",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = caption, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp
            )
        }
    }
}

//@Composable
//private fun BoxScope.PostVideoActions(
//    post: PostFeed,
//    onReactionClick: (ReactionTypeEnum?) -> Unit,
//    onCommentClick: () -> Unit,
//    onShareClick: () -> Unit,
//    onMoreClick: () -> Unit = {}
//) {
//    val stats = post.statistics
//
//    // Logic for reactions (similar to ReelOverlay)
//    val currentReaction = remember(stats?.currentReaction) {
//        when (stats?.currentReaction?.lowercase()) {
//            "like" -> ReactionTypeEnum.LIKE
//            "dislike" -> ReactionTypeEnum.DISLIKE
//            "love" -> ReactionTypeEnum.LOVE
//            "care" -> ReactionTypeEnum.CARE
//            "haha" -> ReactionTypeEnum.HAHA
//            "wow" -> ReactionTypeEnum.WOW
//            "sad" -> ReactionTypeEnum.SAD
//            "angry" -> ReactionTypeEnum.ANGRY
//            else -> null
//        }
//    }
//
//    var localReaction by remember { mutableStateOf(currentReaction) }
//    var localLikeCount by remember { mutableIntStateOf(stats?.likeCount ?: 0) }
//
//    val reactionItems = remember {
//        listOf(
//            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", painterResource = R.drawable.like),
//            Reaction(key = ReactionTypeEnum.DISLIKE, label = "Dislike", painterResource = R.drawable.dislike),
//            Reaction(key = ReactionTypeEnum.LOVE, label = "Love", painterResource = R.drawable.love),
//            Reaction(key = ReactionTypeEnum.CARE, label = "Care", painterResource = R.drawable.care),
//            Reaction(key = ReactionTypeEnum.HAHA, label = "Haha", painterResource = R.drawable.haha),
//            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", painterResource = R.drawable.wow),
//            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", painterResource = R.drawable.sad),
//            Reaction(key = ReactionTypeEnum.ANGRY, label = "Angry", painterResource = R.drawable.angry),
//        )
//    }
//
//    val pickerState = rememberReactionPickerState(
//        reactions = reactionItems,
//        initialSelection = reactionItems.find { it.key == localReaction }
//    )
//
//    LaunchedEffect(pickerState.selectedReaction) {
//        val selectedKey = pickerState.selectedReaction?.key as? ReactionTypeEnum
//        if (selectedKey != localReaction) {
//            val hadReaction = localReaction != null
//            val willHaveReaction = selectedKey != null
//            if (!hadReaction && willHaveReaction) localLikeCount++
//            else if (hadReaction && !willHaveReaction) localLikeCount--
//            localReaction = selectedKey
//            onReactionClick(selectedKey)
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .align(Alignment.BottomEnd)
//            .padding(end = 12.dp, bottom = 80.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        val (iconData, _) = getReactionIcon(localReaction)
//
//        ReelActionButton(
//            icon = iconData,
//            label = "$localLikeCount",
//            modifier = Modifier.reactionPickerAnchor(pickerState),
//            tint = if (localReaction != null) Color.Unspecified else Color.White,
//            onClick = {
//                val next = if (localReaction != null) null else ReactionTypeEnum.LIKE
//                if (next != localReaction) {
//                     if (localReaction == null) localLikeCount++ else localLikeCount--
//                     localReaction = next
//                     onReactionClick(next)
//                }
//            }
//        )
//
//        ReelActionButton(
//            icon = R.drawable.comment,
//            label = "${stats?.commentCount ?: 0}",
//            tint = Color.White,
//            onClick = onCommentClick
//        )
//
//        ReelActionButton(
//            icon = R.drawable.share_ios,
//            label = "Share",
//            tint = Color.White,
//            onClick = { onShareClick() }
//        )
//
//        ReelActionButton(
//            icon = Icons.Default.MoreHoriz,
//            label = "",
//            tint = Color.White,
//            onClick = { onMoreClick() }
//        )
//    }
//}

//@Composable
//private fun BoxScope.PostVideoInfo(
//    post: PostFeed,
//    onProfileClick: (Int) -> Unit,
//    onCaptionClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .align(Alignment.BottomStart)
//            .fillMaxWidth()
//            .background(
//                brush = Brush.verticalGradient(
//                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
//                )
//            )
//            .padding(start = 16.dp, bottom = 32.dp, end = 80.dp)
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.clickable { post.user?.id?.let { onProfileClick(it) } }
//        ) {
//            Avatar(
//                url = post.user?.profilePictureUrl,
//                username = post.user?.username,
//                modifier = Modifier.size(36.dp).clip(CircleShape)
//            )
//            Spacer(modifier = Modifier.width(12.dp))
//            Text(
//                text = post.user?.fullName ?: post.user?.username ?: "Unknown",
//                color = Color.White,
//                fontWeight = FontWeight.Bold,
//                style = MaterialTheme.typography.titleMedium
//            )
//        }
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        if (post.content.isNotBlank()) {
//            Text(
//                text = post.content,
//                color = Color.White,
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis,
//                style = MaterialTheme.typography.bodyMedium,
//                lineHeight = 20.sp,
//                modifier = Modifier.clickable { onCaptionClick() }
//            )
//        }
//    }
//}


//@kotlin.OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun FullCaptionBottomSheet(
//    caption: String,
//    onDismiss: () -> Unit
//) {
//    ModalBottomSheet(
//        onDismissRequest = onDismiss,
//        containerColor = MaterialTheme.colorScheme.surface,
//        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//                .padding(bottom = 32.dp)
//        ) {
//            Text(
//                text = "Caption",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(bottom = 12.dp)
//            );
//            Text(
//                text = caption,
//                style = MaterialTheme.typography.bodyLarge,
//                lineHeight = 24.sp
//            );
//        };
//    };
//};
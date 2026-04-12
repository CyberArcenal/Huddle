package com.cyberarcenal.huddle.ui.reel.feed.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.common.feed.ShareBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri


@UnstableApi
@Composable
fun ReelPlayerItem(
    reel: ReelDisplay,
    isActive: Boolean,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onProfileClick: (Int?) -> Unit,
    onFollowClick: (Int, Boolean, String) -> Unit,
    onMoreClick: (Int) -> Unit,
    onCreateClick: () -> Unit,
    currentUser: UserProfile?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(true) }
    var showPlayIcon by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val videoUri = reel.videoUrl ?: reel.media?.firstOrNull()?.fileUrl ?: ""
            setMediaItem(MediaItem.fromUri(videoUri.toUri()))
            prepare()
        }
    }

    // Share Bottom Sheet
    if (showShareSheet) {
        ShareBottomSheet(
            onDismiss = { showShareSheet = false },
            onShare = { shareData ->
                showShareSheet = false
                onShareClick(shareData)
            },
            contentType = "reel",
            contentId = reel.id ?: 0
        )
    }

    // Lifecycle Observer for the individual player
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive && isPlaying) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Control playback based on active status
    LaunchedEffect(isActive) {
        if (isActive) {
            if (isPlaying) exoPlayer.play()
        } else {
            exoPlayer.pause()
            exoPlayer.seekTo(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isPlaying = !isPlaying
                    if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                    coroutineScope.launch {
                        showPlayIcon = true
                        delay(500)
                        showPlayIcon = false
                    }
                }
        )

        AnimatedVisibility(
            visible = !isPlaying || showPlayIcon,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
        }

        ReelOverlay(
            reel = reel,
            onReactionClick = onReactionClick,
            onCommentClick = onCommentClick,
            onShareClick = { showShareSheet = true },
            onProfileClick = onProfileClick,
            onFollowClick = onFollowClick,
            onCreateClick = onCreateClick,
            onMoreClick = { reel.id?.let { onMoreClick(it) } },
            currentUserId = currentUser?.id,
        )
    }
}
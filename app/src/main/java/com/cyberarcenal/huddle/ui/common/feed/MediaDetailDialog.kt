package com.cyberarcenal.huddle.ui.common.feed

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.post.VideoPlaybackManager
import com.cyberarcenal.huddle.ui.common.post.VideoPreferences

@Composable
fun MediaDetailDialog(
    media: MediaDetailData,
    onDismiss: () -> Unit,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int, stats: PostStatsSerializers?) -> Unit,
) {
    // When the dialog opens, pause any feed video by clearing the active URL
    LaunchedEffect(Unit) {
        VideoPlaybackManager.setActive(null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        MediaDetailFrame(
            user = media.user,
            createdAt = media.createdAt,
            statistics = media.stats,
            onCloseClick = onDismiss,
            onReactionClick = { reactionType ->
                val request = ReactionCreateRequest(
                    contentType = media.type,
                    objectId = media.id,
                    reactionType = reactionType
                )
                onReactionClick(request)
            },
            onCommentClick = {
                onCommentClick(media.type, media.id, media.stats)
            },
            onShareClick = { /* Handle share logic */ }
        ) {
            // Determine if the media is a video
            val isVideo = isVideoUrl(media.url)

            if (isVideo) {
                // Video content – fullscreen player with controls
                VideoPlayerDialog(
                    videoUrl = media.url,
                    onDismiss = onDismiss // optional, to pause on dismiss
                )
            } else {
                // Image content – zoomable
                var scale by remember { mutableFloatStateOf(1f) }
                val transformState = rememberTransformableState { zoomChange, _, _ ->
                    scale *= zoomChange
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformState),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = media.url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale.coerceIn(1f, 5f),
                                scaleY = scale.coerceIn(1f, 5f)
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

/**
 * Helper to detect video URLs (same logic as in PostItem)
 */
private fun isVideoUrl(url: String?): Boolean {
    return url?.let {
        it.endsWith(".mp4", ignoreCase = true) ||
                it.endsWith(".mov", ignoreCase = true) ||
                it.endsWith(".avi", ignoreCase = true) ||
                it.endsWith(".mkv", ignoreCase = true) ||
                it.endsWith(".webm", ignoreCase = true)
    } ?: false
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (VideoPreferences.isMuted) 0f else 1f
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            playWhenReady = true  // auto-play when opened
        }
    }

    // Sync volume with global preference
    LaunchedEffect(VideoPreferences.isMuted) {
        exoPlayer.volume = if (VideoPreferences.isMuted) 0f else 1f
    }

    // Listen to player state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isError = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Lifecycle observer – pause when app goes background, stop when dialog is dismissed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.pause()
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Play/Pause overlay
        IconButton(
            onClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (exoPlayer.isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Volume toggle (global)
        IconButton(
            onClick = { VideoPreferences.isMuted = !VideoPreferences.isMuted },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(
                imageVector = if (VideoPreferences.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (VideoPreferences.isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BrokenImage, contentDescription = "Error", tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        isError = false
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }
    }
}
package com.cyberarcenal.huddle.ui.common.post

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay


@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerItem(
    videoUrl: String,
    shouldBeActive: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Auto‑hide controls after 2 seconds
    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) {
            delay(2000)
            showControls = false
        } else if (!isPlaying && !showControls) {
            showControls = true
        }
    }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (VideoPreferences.isMuted) 0f else 1f
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
        }
    }

    LaunchedEffect(VideoPreferences.isMuted) {
        exoPlayer.volume = if (VideoPreferences.isMuted) 0f else 1f
    }

    val activeVideoUrl by VideoPlaybackManager.activeVideoUrl.collectAsState()
    val isActive = shouldBeActive && activeVideoUrl == videoUrl

    LaunchedEffect(isActive) {
        if (isActive && !isPlaying) {
            exoPlayer.play()
        } else if (!isActive && isPlaying) {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(shouldBeActive, videoUrl) {
        if (shouldBeActive) {
            VideoPlaybackManager.setActive(videoUrl)
        } else if (VideoPlaybackManager.isActive(videoUrl)) {
            VideoPlaybackManager.setActive(null)
        }
    }

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
            if (VideoPlaybackManager.isActive(videoUrl)) {
                VideoPlaybackManager.setActive(null)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                    if (VideoPlaybackManager.isActive(videoUrl)) {
                        VideoPlaybackManager.setActive(null)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (shouldBeActive) {
                        VideoPlaybackManager.setActive(videoUrl)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        )

        // Play/Pause button (centered, appears when controls are shown)
        if (showControls) {
            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        if (VideoPlaybackManager.isActive(videoUrl)) {
                            VideoPlaybackManager.setActive(null)
                        }
                    } else {
                        exoPlayer.play()
                        VideoPlaybackManager.setActive(videoUrl)
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (exoPlayer.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }


        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(32.dp)   // exact size ng buong clickable area
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .clickable { VideoPreferences.isMuted = !VideoPreferences.isMuted },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (VideoPreferences.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (VideoPreferences.isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
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
                        exoPlayer.playWhenReady = isActive
                    }) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }
    }
}
package com.cyberarcenal.huddle.ui.common.story

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cyberarcenal.huddle.data.videoPlayer.VideoPlayerManager
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun StoryVideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    onVideoFinished: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoManager = VideoPlayerManager.getInstance(context)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    LaunchedEffect(isMuted) {
        when (isMuted) {
            true -> exoPlayer.mute()
            false -> exoPlayer.unmute()
        }
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) {
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.stop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load video kapag nagbago ang URL
    LaunchedEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
    }

    // Kontrolin ang playback at i-manage ang global video manager
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // I-pause ang kahit anong tumutugtog na feed video
            videoManager.pause()
            exoPlayer.play()
        } else {
            exoPlayer.pause()
            // I-resume ang feed video player
            videoManager.resume()
        }
    }

    // Progress polling para sa progress bars
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                if (exoPlayer.duration > 0) {
                    val progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration
                    onProgressUpdate(progress.coerceIn(0f, 1f))
                }
                delay(16)
                // Itigil ang polling kung hindi na playing
                if (!exoPlayer.isPlaying && !isPlaying) break
            }
        }
    }

    // Pakinggan ang pagtatapos ng video
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoFinished()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            // Siguraduhing mag-resume ang global manager kapag na-dispose ang player na ito
            videoManager.resume()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

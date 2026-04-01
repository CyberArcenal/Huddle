package com.cyberarcenal.huddle.data.videoPlayer

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

val LocalVideoPlayerManager = compositionLocalOf<VideoPlayerManager> {
    error("No VideoPlayerManager provided")
}

@Composable
fun VideoPlayerLayout(
    modifier: Modifier = Modifier,
    positionProvider: VideoPositionProvider = DefaultVideoPositionProvider(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { VideoPlayerManager.getInstance(context) }
    val configuration = LocalConfiguration.current

    LaunchedEffect(configuration) {
        val metrics = context.resources.displayMetrics
        manager.updateScreenSize(IntSize(metrics.widthPixels, metrics.heightPixels))
    }

    LaunchedEffect(positionProvider) {
        manager.setPositionProvider(positionProvider)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> manager.onPause()
                Lifecycle.Event.ON_RESUME -> manager.onResume()
                Lifecycle.Event.ON_STOP -> manager.onStop()
                Lifecycle.Event.ON_DESTROY -> manager.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalVideoPlayerManager provides manager) {
        Box(modifier = modifier) {
            content()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun HuddleVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = {}
) {
    val manager = LocalVideoPlayerManager.current
    val activeUrl by manager.currentVideoUrl.collectAsState()
    val player by manager.currentPlayer.collectAsState()

    val isActive = activeUrl == videoUrl

    Box(
        modifier = modifier.videoPlayerAnchor(videoUrl)
    ) {
        if (isActive && player != null) {
            // 🔥 Ito ang tamang paraan ng paglalagay ng key
            key(videoUrl) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            setKeepContentOnPlayerReset(false)   // clears old frame
                            this.player = player
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != player) {
                            playerView.player = player
                        }
                    },
                    onRelease = { playerView ->
                        playerView.player = null
                        playerView.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            placeholder()
        }
    }
}

// 🔥 FIXED: gumamit na ng unique key per video instance
@Composable
fun Modifier.videoPlayerAnchor(url: String): Modifier = composed {
    val manager = LocalVideoPlayerManager.current
    val anchorKey = remember { Any() }   // unique key, hindi na yung url

    DisposableEffect(anchorKey) {
        onDispose {
            manager.removeAnchor(anchorKey)
        }
    }

    this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            manager.updateAnchorBounds(anchorKey, url, coordinates.boundsInRoot())
        }
    }
}
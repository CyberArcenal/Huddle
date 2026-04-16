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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.suspendCancellableCoroutine

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
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    isExternalControl: Boolean = false,
    placeholder: @Composable () -> Unit = {}
) {
    val manager = LocalVideoPlayerManager.current
    val activeUrl by manager.currentVideoUrl.collectAsState()
    val activeKey by manager.activeAnchorKey.collectAsState()
    val player by manager.currentPlayer.collectAsState()
    val externallyManagedUrls by manager.externallyManagedUrls.collectAsState()

    // Create a unique key for this specific instance of the video player
    val anchorKey = remember { Any() }

    // Only active if URL matches AND it's either the active anchor or it's the fullscreen instance
    val isActive = activeUrl == videoUrl && (isExternalControl || activeKey == anchorKey)
    val isBeingManagedElsewhere = externallyManagedUrls.contains(videoUrl) && !isExternalControl

    // Track frame rendering for this specific instance to hide placeholder at the right time
    var isRendered by remember(videoUrl) { mutableStateOf(false) }

    // Logic to manage the isRendered state
    LaunchedEffect(isActive, isBeingManagedElsewhere, player) {
        if (isActive && !isBeingManagedElsewhere && player != null) {
            val p = player!!
            
            // If the player is already ready (e.g., returning from fullscreen), 
            // we show it almost immediately to avoid a long thumbnail hang.
            if (p.playbackState == Player.STATE_READY && p.playWhenReady) {
                delay(200) // Slightly increased buffer for surface attachment
                isRendered = true
            }

            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    isRendered = true
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && p.playWhenReady) {
                        // Backup check if onRenderedFirstFrame didn't fire
                        isRendered = true
                    }
                }
            }
            p.addListener(listener)
            try {
                // Wait indefinitely while active. The listener will trigger isRendered = true
                suspendCancellableCoroutine<Unit> { }
            } finally {
                p.removeListener(listener)
            }
        } else {
            // When not active or managed elsewhere, reset isRendered immediately
            isRendered = false
        }
    }

    Box(
        modifier = modifier.videoPlayerAnchor(videoUrl, anchorKey)
    ) {
        if (isActive && player != null && !isBeingManagedElsewhere) {
            key(videoUrl) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            this.resizeMode = resizeMode
                            setKeepContentOnPlayerReset(false)
                            this.player = player
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != player) {
                            playerView.player = player
                        }
                        if (playerView.resizeMode != resizeMode) {
                            playerView.resizeMode = resizeMode
                        }
                    },
                    onRelease = { playerView ->
                        playerView.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Show placeholder if:
        // 1. Not the active video
        // 2. Active but being managed elsewhere (e.g. fullscreen is open)
        // 3. Active but hasn't rendered the first frame yet (to avoid black flash/stale frame)
        if (!isActive || isBeingManagedElsewhere || !isRendered) {
            placeholder()
        }
    }
}

@Composable
fun Modifier.videoPlayerAnchor(url: String, key: Any): Modifier = composed {
    val manager = LocalVideoPlayerManager.current

    DisposableEffect(key) {
        onDispose {
            manager.removeAnchor(key)
        }
    }

    this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            manager.updateAnchorBounds(key, url, coordinates.boundsInRoot())
        }
    }
}
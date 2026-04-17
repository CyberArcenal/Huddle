package com.cyberarcenal.huddle.data.videoPlayer

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

@OptIn(UnstableApi::class)
@Composable
fun VideoAnchor(
    videoUrl: String,
    modifier: Modifier = Modifier,
    thumbnailUri: String? = null,
    resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    showMuteButton: Boolean = true,
    isExternalControl: Boolean = false,
    isPaused: Boolean = false,
    muteButtonModifier: Modifier = Modifier
        .padding(8.dp),
    placeholder: @Composable () -> Unit = { 
        if (thumbnailUri != null) {
            VideoThumbnail(thumbnailUri)
        } else {
            DefaultVideoPlaceholder()
        }
    },
) {
    Box(modifier = modifier) {
        // Render local player that attaches to global ExoPlayer when active
        HuddleVideoPlayer(
            videoUrl = videoUrl,
            modifier = Modifier.fillMaxSize(),
            resizeMode = resizeMode,
            isExternalControl = isExternalControl,
            isPaused = isPaused,
            placeholder = placeholder
        )

        // Overlay mute toggle
        if (showMuteButton) {
            MuteButton(
                modifier = muteButtonModifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun DefaultVideoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Video",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun VideoThumbnail(
    thumbnailUrl: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
            }
        )
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().shimmerEffect())
        }

        // Play button overlay to indicate it's a video
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun MuteButton(modifier: Modifier = Modifier) {
    val manager = LocalVideoPlayerManager.current
    val isMuted by manager.isMuted.collectAsState()

    IconButton(
        onClick = { manager.toggleMute() },
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = if (isMuted) "Unmute" else "Mute",
            tint = Color.White
        )
    }
}

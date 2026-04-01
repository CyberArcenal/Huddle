package com.cyberarcenal.huddle.data.videoPlayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VideoAnchor(
    videoUrl: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = { DefaultVideoPlaceholder() },
) {
    Box(modifier = modifier) {
        // Render local player that attaches to global ExoPlayer when active
        HuddleVideoPlayer(
            videoUrl = videoUrl,
            modifier = Modifier.fillMaxSize(),
            placeholder = placeholder
        )

        // Overlay mute toggle
        MuteButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
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

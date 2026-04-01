package com.cyberarcenal.huddle.ui.common.reel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.data.videoPlayer.VideoAnchor
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

@Composable
fun ReelFeedItem(
    reel: ReelDisplay,
    onReelClick: (reelId: Int) -> Unit,      // Para sa pag-navigate sa dedicated reel screen
    onProfileClick: (userId: Int) -> Unit,
    onVideoClick: (String) -> Unit = {}      // Bagong parameter para sa fullscreen video
) {
    // Early return kung walang essential data
    val statistics = reel.statistics
    val reelId = reel.id ?: return
    val user = reel.user
    val userId = user?.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video container na may fixed height at clipping para maiwasan ang overflow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                // Kung may video URL, gamitin ang VideoAnchor (katulad ng PostItem)
                val videoUrl = reel.videoUrl
                if (!videoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()                     // Pumipigil sa overflow
                            .clickable { onVideoClick(videoUrl) } // Fullscreen on click
                    ) {
                        VideoAnchor(
                            videoUrl = videoUrl,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = {
                                // Ipakita ang thumbnail habang hindi aktibo ang video
                                VideoThumbnailContent(
                                    thumbnailUrl = reel.thumbnailUrl,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                        )
                    }
                } else {
                    // Fallback: thumbnail lang kung walang video URL
                    VideoThumbnailContent(
                        thumbnailUrl = reel.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // User Info & Caption (walang pagbabago)
            Column(modifier = Modifier.padding(12.dp)) {
                if (user != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clickable { userId?.let { onProfileClick(it) } }) {
                            Avatar(url = user.profilePictureUrl, username = user.username, size = 32.dp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = user.fullName ?: user.username ?: "User",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!reel.caption.isNullOrBlank()) {
                    Text(
                        text = reel.caption,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnailContent(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (!thumbnailUrl.isNullOrBlank()) {
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
                    isError = state is AsyncImagePainter.State.Error
                }
            )
        } else {
            // Kung walang thumbnail, dark background
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)))
            isLoading = false
        }

        // Loading shimmer
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect()
            )
        }

        // Error fallback
        if (isError && thumbnailUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Play icon overlay (opsyonal)
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
        )
    }
}
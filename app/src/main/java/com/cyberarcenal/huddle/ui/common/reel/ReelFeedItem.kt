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
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect
import com.cyberarcenal.huddle.utils.formatRelativeTime
import java.time.OffsetDateTime

@Composable
fun ReelFeedItem(
    reel: ReelDisplay,
    onReelClick: (reelId: Int) -> Unit,
    onProfileClick: (userId: Int) -> Unit
) {
    // Early return if essential data is missing
    val statistics = reel.statistics ?: return
    val reelId = reel.id ?: return
    val user = reel.user ?: return
    val userId = user.id ?: return
    val timestamp = statistics.createdAt ?: OffsetDateTime.now()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onReelClick(reelId) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video thumbnail with play button
            VideoThumbnail(
                thumbnailUrl = reel.thumbnailUrl,
                videoUrl = reel.videoUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )

            // Caption
            if (!reel.caption.isNullOrBlank()) {
                Text(
                    text = reel.caption,
                    modifier = Modifier.padding(12.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun VideoThumbnail(
    thumbnailUrl: String?,
    videoUrl: String?,
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
            // No thumbnail provided, treat as error
            isError = true
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
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Error loading thumbnail",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thumbnail unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Play button overlay – only show if we have a video URL and no error
        if (!isError && !videoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
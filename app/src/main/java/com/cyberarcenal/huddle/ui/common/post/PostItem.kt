package com.cyberarcenal.huddle.ui.common.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.videoPlayer.VideoAnchor
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global video playback manager that ensures only one video plays at a time.
 */
object VideoPlaybackManager {
    private val _activeVideoUrl = MutableStateFlow<String?>(null)
    val activeVideoUrl: StateFlow<String?> = _activeVideoUrl.asStateFlow()

    fun setActive(videoUrl: String?) {
        if (_activeVideoUrl.value != videoUrl) {
            _activeVideoUrl.value = videoUrl
        }
    }

    fun isActive(videoUrl: String?): Boolean = _activeVideoUrl.value == videoUrl
}

object VideoPreferences {
    var isMuted by mutableStateOf(true)
}

@Composable
fun PostItem(
    post: PostFeed,
    onImageClick: (MediaDetailData) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val mediaList = post.media
        if (!mediaList.isNullOrEmpty()) {
            val pagerState = rememberPagerState(pageCount = { mediaList.size })

            // Visibility detection
            val density = LocalDensity.current
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            var isPostVisible by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { coordinates ->
                        val yPos = coordinates.positionInWindow().y
                        val height = coordinates.size.height
                        val yPosDp = with(density) { yPos.toDp().value }
                        isPostVisible = yPosDp + height > 0 && yPosDp < screenHeightDp
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> mediaList[page].id ?: page }
                ) { page ->
                    val mediaItem = mediaList[page]

                    val isVideo = when (post.postType?.value) {
                        "video" -> true
                        "image" -> false
                        else -> mediaItem.fileUrl?.let { url ->
                            url.endsWith(".mp4", ignoreCase = true) ||
                                    url.endsWith(".mov", ignoreCase = true) ||
                                    url.endsWith(".avi", ignoreCase = true) ||
                                    url.endsWith(".mkv", ignoreCase = true) ||
                                    url.endsWith(".webm", ignoreCase = true)
                        } ?: false
                    }

                    val videoUrl = mediaItem.fileUrl
                    val shouldBeActive = isVideo && videoUrl != null && pagerState.currentPage == page && isPostVisible

                    if (isVideo && videoUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds() // Puputol ng anumang  drawing sa labas ng bounds
                        ) {
                            VideoAnchor(
                                videoUrl = videoUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        ImageItem(
                            imageUrl = mediaItem.fileUrl ?: "",
                            onClick = {
                                if (mediaItem.fileUrl != null && mediaItem.id != null) {
                                    onImageClick(
                                        MediaDetailData(
                                            url = mediaItem.fileUrl,
                                            user = post.user,
                                            createdAt = post.createdAt,
                                            stats = post.statistics,
                                            id = mediaItem.id,
                                            type = "postmedia"
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                if (mediaList.size > 1) {
                    PageIndicator(
                        pageCount = mediaList.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageItem(
    imageUrl: String,
    onClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
                isError = state is AsyncImagePainter.State.Error
            },
            contentScale = ContentScale.Crop
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().shimmerEffect())
        }

        if (isError) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BrokenImage, contentDescription = "Failed to load image", modifier = Modifier.size(48.dp))
            }
        }
    }
}



@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) Color.White
                        else Color.White.copy(alpha = 0.5f)
                    )
            )
        }
    }
}
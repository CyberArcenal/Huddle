package com.cyberarcenal.huddle.ui.common.post

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val mediaItem = mediaList[page]
                    val isVideo = mediaItem.fileUrl?.let { url ->
                        url.endsWith(".mp4", ignoreCase = true) ||
                                url.endsWith(".mov", ignoreCase = true) ||
                                url.endsWith(".avi", ignoreCase = true)
                    } ?: false

                    // Check if this specific page is currently active/visible
                    val isCurrentPage = pagerState.currentPage == page

                    if (isVideo) {
                        VideoPlayerItem(
                            videoUrl = mediaItem.fileUrl ?: "",
                            isActive = isCurrentPage, // Pass visibility state
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
                Icon(Icons.Default.BrokenImage, null, modifier = Modifier.size(48.dp))
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerItem(
    videoUrl: String,
    isActive: Boolean, // New parameter for auto-play
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isMuted by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // Mute by default
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
        }
    }

    // Handle Play/Pause based on Active state (Pager visibility)
    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Handle Volume updates
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Lifecycle observer to handle app pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isActive) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            modifier = Modifier.fillMaxSize().clickable { onClick() }
        )

        // Volume Toggle Button
        IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Toggle Mute",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }

        if (isError) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BrokenImage, null, tint = Color.White)
            }
        }
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                isError = true
            }
        })
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

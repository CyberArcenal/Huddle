package com.cyberarcenal.huddle.ui.common.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

@Composable
fun PostItem(
    post: PostFeed,
    onImageClick: (MediaDetailData) -> Unit = {}
) {
    // CONTENT LAMANG: Text at Images lang ang nandito.
    // Walang Header at walang Interaction Buttons para hindi mag-double.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Media (Pager logic)
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
                    var isLoading by remember { mutableStateOf(true) }
                    var isError by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaItem.fileUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    mediaItem.let {
                                        if (it.fileUrl == null || it.id == null) return@clickable
                                        val data = MediaDetailData(
                                            url = it.fileUrl,
                                            user = post.user,
                                            createdAt = post.createdAt,
                                            stats = post.statistics,
                                            id = it.id,
                                            type = "postmedia"
                                        )
                                        onImageClick(data)
                                    }
                                },
                            onState = { state ->
                                isLoading = state is AsyncImagePainter.State.Loading
                                isError = state is AsyncImagePainter.State.Error
                            },
                            contentScale = ContentScale.Crop
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmerEffect()
                            )
                        }

                        if (isError) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Error loading image",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Failed to load media",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

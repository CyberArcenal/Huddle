package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PostFeed

@Composable
fun PostItem(
    post: PostFeed,
    onImageClick: (String) -> Unit = {}
) {
    // CONTENT LAMANG: Text at Images lang ang nandito.
    // Walang Header at walang Interaction Buttons para hindi mag-double.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Post content text
        if (!post.content.isNullOrBlank()) {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // Media (Pager logic)
        val mediaList = post.media
        if (!mediaList.isNullOrEmpty()) {
            val pagerState = rememberPagerState(pageCount = { mediaList.size })
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) { page ->
                    val mediaItem = mediaList[page]
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mediaItem.fileUrl)
                            .crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                mediaItem.fileUrl?.let { onImageClick(it) }
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
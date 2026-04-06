package com.cyberarcenal.huddle.ui.common.userimage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.UserImageDisplay
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

@Composable
fun UserImageFeedItem(
    user: UserMinimal,
    userImage: UserImageDisplay,
    onImageClick: (MediaDetailData) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f) // widescreen
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                userImage.imageUrl?.let { url ->
                    onImageClick(
                        MediaDetailData(
                            url = url,
                            user = user,
                            createdAt = userImage.createdAt,
                            stats = userImage.statistics,
                            id = userImage.id ?: user.id ?: 0,
                            type = "user_image"
                        )
                    )
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(userImage.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "User image update",
            modifier = Modifier.fillMaxSize(),
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Error loading image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Failed to load",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
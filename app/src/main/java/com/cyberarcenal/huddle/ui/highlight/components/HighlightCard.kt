package com.cyberarcenal.huddle.ui.highlight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.cyberarcenal.huddle.api.models.StoryHighlight
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun HighlightCard(
    highlight: StoryHighlight,
    onClick: (StoryHighlight) -> Unit,
    onLongClick: (StoryHighlight) -> Unit = {},
    modifier: Modifier = Modifier,
    isAdded: Boolean = false
) {
    Box(
        modifier = modifier
            .width(120.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(30.dp))
            .shadow(if (isAdded) 0.dp else 8.dp, RoundedCornerShape(30.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (!isAdded) onClick(highlight) },
                    onLongPress = { if (!isAdded) onLongClick(highlight) }
                )
            }
            .alpha(if (isAdded) 0.6f else 1f)
    ) {
        // Background image with loading/error handling
        val cover = when(highlight.coverUrl) {
            null -> {
                val stories = highlight.stories
                if (!stories.isNullOrEmpty()) {
                    stories[stories.size - 1].mediaUrl
                } else {
                    null
                }
            }
            else -> highlight.coverUrl
        }
        SubcomposeAsyncImage(
            model = cover,
            contentDescription = highlight.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            // Ang loading at error lambdas ay tumatanggap ng state parameter
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Error loading image",
                        tint = Color.Gray
                    )
                }
            }
        ) // Inalis ang trailing lambda dito dahil redundant ito

        // Gradient overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 0.6f
                    )
                )
        )

        // Title at the bottom
        Text(
            text = highlight.title ?: "",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )

        if (isAdded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Added",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
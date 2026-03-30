package com.cyberarcenal.huddle.ui.common.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Avatar(
    url: String?,
    username: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    // Dynamic Island Shape (Squircle/Rounded Rect)
    // Karaniwang 40% ng size ang corner radius para sa island look
    val islandShape = RoundedCornerShape(size * 0.35f)

    val finalModifier = modifier
        .size(size)
        .clip(islandShape)
        // Nilagyan ng manipis na border para magmukhang "hardware" component
        .border(0.5.dp, Color.White.copy(alpha = 0.1f), islandShape)

    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            modifier = finalModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = finalModifier
                // Jet Black background na katulad ng Dynamic Island
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Avatar",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

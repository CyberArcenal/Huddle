// Avatar.kt (updated)

package com.cyberarcenal.huddle.ui.common.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    // FIX: Inuna ang modifier parameter para kung may .size() sa labas, yun ang masusunod.
    // Pero kung wala, gagamitin ang default size parameter.
    val finalModifier = modifier
        .size(size)
        .clip(CircleShape)

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
            modifier = finalModifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username?.take(1)?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                // Ang font size ay mag-aadjust base sa laki ng avatar
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}
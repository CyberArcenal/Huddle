package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun StoryItem(
    username: String?,
    profilePictureUrl: String?,
    thumbnailUrl: String?,
    hasViewedAll: Boolean = false,
    isMyStory: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            // Story Thumbnail
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isMyStory) {
                // Placeholder for "Add Story" if no thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Dark Gradient Scrim (for text readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 100f
                        )
                    )
            )

            // User Avatar Overlay (Top Left)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
                    .align(Alignment.TopStart)
            ) {
                val borderColor = if (hasViewedAll) Color.LightGray else MaterialTheme.colorScheme.primary
                
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, borderColor),
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    if (!profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Username Overlay (Bottom)
            Text(
                text = if (isMyStory) "Your Story" else (username ?: "User"),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
fun PreviewStoryItem() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // My Story (Add)
            StoryItem(
                username = "darius",
                profilePictureUrl = null,
                thumbnailUrl = null,
                isMyStory = true,
                onClick = {}
            )
            
            // Unviewed Story
            StoryItem(
                username = "maria_clara",
                profilePictureUrl = "https://example.com/pfp.jpg",
                thumbnailUrl = "https://example.com/thumb.jpg",
                hasViewedAll = false,
                onClick = {}
            )

            // Viewed Story
            StoryItem(
                username = "juan_delacruz",
                profilePictureUrl = null,
                thumbnailUrl = "https://example.com/thumb2.jpg",
                hasViewedAll = true,
                onClick = {}
            )
        }
    }
}

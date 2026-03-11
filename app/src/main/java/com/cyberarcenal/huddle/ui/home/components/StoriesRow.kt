package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.StoryFeed

@Composable
fun StoriesRow(
    stories: List<StoryFeed>,
    currentUserProfilePicture: String? = null,
    onCreateStoryClick: (() -> Unit)? = null,
    onStoryClick: (StoryFeed) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // Create Story Card
        item {
            CreateStoryCard(
                profilePictureUrl = currentUserProfilePicture,
                onClick = onCreateStoryClick ?: {}
            )
        }

        // Friends' Stories Cards
        items(stories, key = { it.user?.id ?: it.hashCode() }) { storyFeed ->
            StoryCard(
                storyFeed = storyFeed,
                onClick = { onStoryClick(storyFeed) }
            )
        }
    }
}

@Composable
fun StoryCard(
    storyFeed: StoryFeed,
    onClick: () -> Unit
) {
    // Get first story image from the list, or fallback to user's profile picture
    val storyImage = storyFeed.stories.firstOrNull { it.mediaUrl != null }?.mediaUrl?.toString()
    val fallbackImage = storyFeed.user?.profilePictureUrl?.toString()
    val imageToLoad = storyImage ?: fallbackImage

    Card(
        modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image (story image or profile pic)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageToLoad)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = ColorPainter(Color.Gray) // final fallback
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 300f
                        )
                    )
            )

            // Profile picture overlay (top left)
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp)
                    .align(Alignment.TopStart)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFCAF45), Color(0xFFE1306C))
                        ),
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(storyFeed.user?.profilePictureUrl?.toString())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = ColorPainter(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            // Username (bottom)
            Text(
                text = storyFeed.user?.username ?: "",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun CreateStoryCard(
    profilePictureUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top part: Profile Picture
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUrl?.toString())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = ColorPainter(Color.Gray) // Fallback gray box
                )
                // Overlay to dim the image slightly
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
            }

            // Bottom part: Plus Icon and Label
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Plus Icon floating between top and bottom
                    Surface(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .size(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    Text(
                        text = "Create\nStory",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        modifier = Modifier.offset(y = (-8).dp)
                    )
                }
            }
        }
    }
}
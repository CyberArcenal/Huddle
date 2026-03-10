package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.StoryFeed

@Composable
fun StoriesRow(
    stories: List<StoryFeed>,
    modifier: Modifier = Modifier,
    onStoryClick: (StoryFeed) -> Unit = {},
    // Optional: pass current user to show "Your Story" circle (needs separate handling)
    // currentUserProfilePicture: String? = null,
    // onCreateStoryClick: (() -> Unit)? = null
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        // Uncomment to add a "Your Story" circle at the beginning
        // if (onCreateStoryClick != null) {
        //     item {
        //         CreateStoryCircle(
        //             profilePictureUrl = currentUserProfilePicture,
        //             onClick = onCreateStoryClick
        //         )
        //     }
        // }

        items(stories, key = { it.user?.id ?: it.hashCode() }) { storyFeed ->
            StoryCircle(
                storyFeed = storyFeed,
                onClick = { onStoryClick(storyFeed) }
            )
        }
    }
}

@Composable
fun StoryCircle(
    storyFeed: StoryFeed,
    onClick: () -> Unit,
    isViewed: Boolean = false  // You can pass this from your data if available
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        // Gradient ring around the avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = if (!isViewed) {
                        // Unseen: vibrant Instagram‑like gradient
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFCAF45), // yellow/orange
                                Color(0xFFF77737), // orange
                                Color(0xFFE1306C)  // pink
                            )
                        )
                    } else {
                        // Seen: a more subtle gray gradient (or you can omit the ring)
                        Brush.linearGradient(
                            colors = listOf(
                                Color.LightGray,
                                Color.Gray
                            )
                        )
                    }
                )
                .padding(3.dp) // ring thickness
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(storyFeed.user?.profilePictureUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = storyFeed.user?.username ?: "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/**
 * Optional "Create Story" circle – can be added as the first item.
 */
@Composable
fun CreateStoryCircle(
    profilePictureUrl: String?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            // Plus icon overlay
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add story",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(2.dp),
                tint = Color.White
            )
        }
        Text(
            text = "Your story",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
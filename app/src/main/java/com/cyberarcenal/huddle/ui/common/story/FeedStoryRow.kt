// FeedStoriesRow.kt
package com.cyberarcenal.huddle.ui.common.story

import com.cyberarcenal.huddle.ui.storyviewer.StoryCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.ui.storyviewer.SeeMoreStoryCard

@Composable
fun FeedStoriesRow(
    stories: List<StoryFeed>,  // Now expecting a list of StoryFeed (each is a user's story group)
    onCreateStoryClick: (() -> Unit)? = null,
    onStoryClick: (StoryFeed, Int) -> Unit,  // Pass the StoryFeed and its index
    onSeeMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(
            count = stories.size,
            key = { index -> "story_user_${stories[index].user.id ?: index}" }
        ) { index ->
            val storyFeed = stories[index]
            StoryCard(
                storyFeed = storyFeed,
                onClick = { onStoryClick(storyFeed, index) }
            )
        }

        if (stories.isNotEmpty() && onSeeMoreClick != null) {
            item(key = "see_more_stories") {
                SeeMoreStoryCard(onClick = onSeeMoreClick)
            }
        }
    }
}
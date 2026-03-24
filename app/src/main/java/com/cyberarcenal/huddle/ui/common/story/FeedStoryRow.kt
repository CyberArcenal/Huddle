package com.cyberarcenal.huddle.ui.common.story

import com.cyberarcenal.huddle.ui.storyviewer.StoryCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.ui.feed.StoryItem
import com.cyberarcenal.huddle.ui.storyviewer.SeeMoreStoryCard

@Composable
fun FeedStoriesRow(
    stories: List<StoryItem>,
    currentUserProfilePicture: String? = null,
    onCreateStoryClick: (() -> Unit)? = null,
    onStoryClick: (StoryFeed) -> Unit = {},
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
        // Friends' Stories Cards
        items(stories, key = { "story_user_${it.user?.id ?: it.hashCode()}" }) { item ->
            StoryCard(
                storyFeed = item.stories[0],
                onClick = { onStoryClick(item.stories[0]) }
            )
        }

        if (stories.isNotEmpty() && onSeeMoreClick != null) {
            item(key = "see_more_stories") {
                SeeMoreStoryCard(onClick = onSeeMoreClick)
            }
        }
    }
}
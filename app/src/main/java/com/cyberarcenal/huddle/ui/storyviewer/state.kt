package com.cyberarcenal.huddle.ui.storyviewer

import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.UserMinimal

sealed class StoryFeedViewerUiState {
    object Loading : StoryFeedViewerUiState()
    data class Error(val message: String) : StoryFeedViewerUiState()
    data class Success(
        val currentStory: Story,
        val totalStoriesInCurrentUser: Int,
        val currentStoryIndex: Int,
        val currentUser: UserMinimal
    ) : StoryFeedViewerUiState()
}
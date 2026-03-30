package com.cyberarcenal.huddle.ui.storyviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StoryFeedViewerViewModel(
    private val storyFeeds: List<StoryFeed>,
    private val startIndex: Int,
    private val viewManager: ViewManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<StoryFeedViewerUiState>(StoryFeedViewerUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent = _closeEvent.asSharedFlow()

    private var currentUserIndex = startIndex
    private var currentStoryIndex = 0

    init {
        loadCurrentStory()
    }

    private fun loadCurrentStory() {
        if (currentUserIndex !in storyFeeds.indices) {
            _uiState.value = StoryFeedViewerUiState.Error("No more stories")
            return
        }
        val userStories = storyFeeds[currentUserIndex].stories ?: emptyList()
        if (userStories.isEmpty()) {
            moveToNextUser()
            return
        }
        if (currentStoryIndex !in userStories.indices) {
            currentStoryIndex = 0
        }
        _uiState.value = StoryFeedViewerUiState.Success(
            currentStory = userStories[currentStoryIndex],
            totalStoriesInCurrentUser = userStories.size,
            currentStoryIndex = currentStoryIndex,
            currentUser = storyFeeds[currentUserIndex].user
        )
        val story = userStories[currentStoryIndex]
        if (story.hasViewed == false && story.id != null) {
            viewManager.recordView("story", story.id, 5)
        }
    }

    fun nextStory() {
        val currentState = _uiState.value as? StoryFeedViewerUiState.Success ?: return
        val userStories = storyFeeds[currentUserIndex].stories ?: emptyList()
        if (currentStoryIndex + 1 < userStories.size) {
            currentStoryIndex++
            loadCurrentStory()
        } else {
            moveToNextUser()
        }
    }

    fun previousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            loadCurrentStory()
        } else if (currentUserIndex > 0) {
            currentUserIndex--
            val prevUserStories = storyFeeds[currentUserIndex].stories ?: emptyList()
            currentStoryIndex = (prevUserStories.size - 1).coerceAtLeast(0)
            loadCurrentStory()
        }
    }

    private fun moveToNextUser() {
        if (currentUserIndex + 1 < storyFeeds.size) {
            currentUserIndex++
            currentStoryIndex = 0
            loadCurrentStory()
        } else {
            viewModelScope.launch { _closeEvent.emit(Unit) }
        }
    }

    fun close() {
        viewModelScope.launch { _closeEvent.emit(Unit) }
    }

    fun onReactionClick(reactionType: ReactionTypeEnum?) {}
    fun onCommentClick() {}
    fun onShareClick(shareData: ShareRequestData) {}
    fun onMoreClick() {}
}

class StoryFeedViewerViewModelFactory(
    private val storyFeeds: List<StoryFeed>,
    private val startIndex: Int,
    private val viewManager: ViewManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryFeedViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryFeedViewerViewModel(storyFeeds, startIndex, viewManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
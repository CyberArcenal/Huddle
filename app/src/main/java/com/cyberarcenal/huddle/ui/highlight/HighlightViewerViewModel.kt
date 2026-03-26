package com.cyberarcenal.huddle.ui.highlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryHighlight
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HighlightCarouselViewModel(
    private val highlights: List<StoryHighlight>,
    private val startIndex: Int,
    private val viewManager: ViewManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<HighlightCarouselUiState>(HighlightCarouselUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent = _closeEvent.asSharedFlow()

    private var currentHighlightIndex = startIndex
    private var currentStoryIndex = 0
    private var autoViewJob: Job? = null

    init {
        loadCurrentStory()
    }

    private fun loadCurrentStory() {
        if (currentHighlightIndex !in highlights.indices) {
            _uiState.value = HighlightCarouselUiState.Error("No more highlights")
            return
        }
        val stories = highlights[currentHighlightIndex].stories ?: emptyList()
        if (stories.isEmpty()) {
            moveToNextHighlight()
            return
        }
        if (currentStoryIndex !in stories.indices) {
            currentStoryIndex = 0
        }
        _uiState.value = HighlightCarouselUiState.Success(
            currentStory = stories[currentStoryIndex],
            totalStoriesInCurrentHighlight = stories.size,
            currentStoryIndex = currentStoryIndex
        )
        val story = stories[currentStoryIndex]
        if (story.hasViewed == false && story.id != null) {
            viewManager.recordView("story", story.id, 5)
        }
        scheduleAutoView()
    }

    fun nextStory() {
        val currentState = _uiState.value as? HighlightCarouselUiState.Success ?: return
        val stories = highlights[currentHighlightIndex].stories ?: emptyList()
        if (currentStoryIndex + 1 < stories.size) {
            currentStoryIndex++
            loadCurrentStory()
        } else {
            moveToNextHighlight()
        }
    }

    fun previousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            loadCurrentStory()
        } else if (currentHighlightIndex > 0) {
            currentHighlightIndex--
            val prevStories = highlights[currentHighlightIndex].stories ?: emptyList()
            currentStoryIndex = (prevStories.size - 1).coerceAtLeast(0)
            loadCurrentStory()
        }
    }

    private fun moveToNextHighlight() {
        if (currentHighlightIndex + 1 < highlights.size) {
            currentHighlightIndex++
            currentStoryIndex = 0
            loadCurrentStory()
        } else {
            viewModelScope.launch { _closeEvent.emit(Unit) }
        }
    }

    private fun scheduleAutoView() {
        autoViewJob?.cancel()
        autoViewJob = viewModelScope.launch {
            delay(5000) // 5 seconds
            val currentState = _uiState.value as? HighlightCarouselUiState.Success
            if (currentState != null) {
                nextStory()
            }
        }
    }

    fun close() {
        autoViewJob?.cancel()
        viewModelScope.launch { _closeEvent.emit(Unit) }
    }

    override fun onCleared() {
        autoViewJob?.cancel()
        super.onCleared()
    }

    fun onReactionClick(reactionType: ReactionCreateRequest.ReactionType?) {}
    fun onCommentClick() {}
    fun onShareClick(shareData: ShareRequestData) {}
    fun onMoreClick() {}
}

class HighlightCarouselViewModelFactory(
    private val highlights: List<StoryHighlight>,
    private val startIndex: Int,
    private val viewManager: ViewManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HighlightCarouselViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HighlightCarouselViewModel(highlights, startIndex, viewManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
// StoryViewerViewModel.kt
package com.cyberarcenal.huddle.ui.storyviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.StoryManager
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class StoryViewerUiState {
    object Loading : StoryViewerUiState()
    data class Error(val message: String) : StoryViewerUiState()
    data class Success(
        val stories: List<Story>,
        val currentIndex: Int,
        val isViewing: Boolean = true
    ) : StoryViewerUiState()
}

class StoryViewerViewModel(
    private val userId: Int,
    private val storyManager: StoryManager,
    private val viewManager: ViewManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoryViewerUiState>(StoryViewerUiState.Loading)
    val uiState: StateFlow<StoryViewerUiState> = _uiState.asStateFlow()

    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent: SharedFlow<Unit> = _closeEvent.asSharedFlow()

    private var autoViewJob: kotlinx.coroutines.Job? = null

    init {
        // Load stories for the given user
        storyManager.openUserStories(userId)
        observeStories()
    }

    private fun observeStories() {
        viewModelScope.launch {
            // Wait for loading to complete
            storyManager.isLoading.collect { isLoading ->
                if (isLoading) {
                    _uiState.value = StoryViewerUiState.Loading
                } else {
                    val stories = storyManager.selectedUserStories.value
                    if (stories.isEmpty()) {
                        _uiState.value = StoryViewerUiState.Error("No stories available")
                    } else {
                        _uiState.value = StoryViewerUiState.Success(
                            stories = stories,
                            currentIndex = 0
                        )
                        scheduleAutoView(0)
                    }
                }
            }
        }
    }

    fun nextStory() {
        val currentState = _uiState.value as? StoryViewerUiState.Success ?: return
        if (currentState.currentIndex + 1 < currentState.stories.size) {
            val newIndex = currentState.currentIndex + 1
            _uiState.value = currentState.copy(currentIndex = newIndex)
            scheduleAutoView(newIndex)
        } else {
            close()
        }
    }

    fun previousStory() {
        val currentState = _uiState.value as? StoryViewerUiState.Success ?: return
        if (currentState.currentIndex - 1 >= 0) {
            val newIndex = currentState.currentIndex - 1
            _uiState.value = currentState.copy(currentIndex = newIndex)
            scheduleAutoView(newIndex)
        }
    }

    fun close() {
        viewModelScope.launch {
            _closeEvent.emit(Unit)
            storyManager.closeStoryViewer()
        }
    }

    fun markCurrentStoryViewed() {
        val currentState = _uiState.value as? StoryViewerUiState.Success ?: return
        val story = currentState.stories.getOrNull(currentState.currentIndex) ?: return
        story.hasViewed?.let {
            if (!it && story.id != null) {
                viewManager.recordView(
                    targetType = "story",
                    targetId = story.id,
                    durationSeconds = 5
                )
                // Optimistically update local state
                val updatedStories = currentState.stories.toMutableList().apply {
                    this[currentState.currentIndex] = story.copy(hasViewed = true)
                }
                _uiState.value = currentState.copy(stories = updatedStories)
            }
        }
    }

    private fun scheduleAutoView(index: Int) {
        autoViewJob?.cancel()
        autoViewJob = viewModelScope.launch {
            delay(5000) // 5 seconds per story
            val currentState = _uiState.value as? StoryViewerUiState.Success
            if (currentState?.currentIndex == index) {
                markCurrentStoryViewed()
                nextStory()
            }
        }
    }

    override fun onCleared() {
        autoViewJob?.cancel()
        super.onCleared()
    }



    fun onReactionClick(reactionType: ReactionCreateRequest.ReactionType?) {
        // TODO: Implement reaction for story
    }

    fun onCommentClick() {
        // TODO: Open comment sheet for story
    }

    fun onShareClick(shareData: ShareRequestData) {
        // TODO: Implement share for story
    }

    fun onMoreClick() {
        // TODO: Show bottom sheet for story options
    }
}

class StoryViewerViewModelFactory(
    private val userId: Int,
    private val storyManager: StoryManager,
    private val viewManager: ViewManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryViewerViewModel(userId, storyManager, viewManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
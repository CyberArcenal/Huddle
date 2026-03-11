package com.cyberarcenal.huddle.ui.storyviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.data.repositories.stories.StoriesRepository
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
    private val storiesRepository: StoriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoryViewerUiState>(StoryViewerUiState.Loading)
    val uiState: StateFlow<StoryViewerUiState> = _uiState.asStateFlow()

    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent: SharedFlow<Unit> = _closeEvent.asSharedFlow()

    private var autoViewJob: kotlinx.coroutines.Job? = null

    init {
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            _uiState.value = StoryViewerUiState.Loading
            val result = storiesRepository.getUserStories(userId = userId, includeExpired = false)
            result.fold(
                onSuccess = { paginated ->
                    if (paginated.results.isEmpty()) {
                        _uiState.value = StoryViewerUiState.Error("No stories available")
                    } else {
                        _uiState.value = StoryViewerUiState.Success(
                            stories = paginated.results,
                            currentIndex = 0
                        )
                        scheduleAutoView(0)
                    }
                },
                onFailure = { error ->
                    _uiState.value = StoryViewerUiState.Error(error.message ?: "Failed to load stories")
                }
            )
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
        }
    }

    fun markCurrentStoryViewed() {
        val currentState = _uiState.value as? StoryViewerUiState.Success ?: return
        val story = currentState.stories.getOrNull(currentState.currentIndex) ?: return
        if (!story.hasViewed) {
            viewModelScope.launch {
                storiesRepository.markStoryViewed(story.id)
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
}

// Factory class para sa manual initialization ng ViewModel
class StoryViewerViewModelFactory(
    private val userId: Int,
    private val storiesRepository: StoriesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryViewerViewModel(userId, storiesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

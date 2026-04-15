package com.cyberarcenal.huddle.ui.storyviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StoryListUiState {
    object Loading : StoryListUiState()
    data class Success(
        val stories: List<StoryFeed>,
        val hasNext: Boolean,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : StoryListUiState()
    data class Error(val message: String) : StoryListUiState()
}

class StoryListViewModel(
    private val storiesRepository: StoriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoryListUiState>(StoryListUiState.Loading)
    val uiState: StateFlow<StoryListUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 20

    init {
        loadStories()
    }

    fun loadStories(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                currentOffset = 0
                val currentState = _uiState.value
                if (currentState is StoryListUiState.Success) {
                    _uiState.value = currentState.copy(isRefreshing = true)
                }
            } else {
                val currentState = _uiState.value
                if (currentState is StoryListUiState.Success) {
                    if (currentState.isLoadingMore || !currentState.hasNext) return@launch
                    _uiState.value = currentState.copy(isLoadingMore = true)
                } else {
                    _uiState.value = StoryListUiState.Loading
                }
            }

            storiesRepository.getStoryFeed(
                offset = currentOffset,
                limit = limit,
                includeOwn = true,
                limitPerUser = 1 // Get just the latest one for the grid/list view
            ).onSuccess { response ->
                val newStories = response.data.feed
                val hasNext = response.data.hasNext
                
                val currentList = if (refresh) emptyList() else {
                    (_uiState.value as? StoryListUiState.Success)?.stories ?: emptyList()
                }
                
                _uiState.value = StoryListUiState.Success(
                    stories = currentList + newStories,
                    hasNext = hasNext,
                    isRefreshing = false,
                    isLoadingMore = false
                )
                currentOffset = response.data.nextOffset ?: (currentOffset + limit)
            }.onFailure {
                _uiState.value = StoryListUiState.Error(it.message ?: "Failed to load stories")
            }
        }
    }
}

class StoryListViewModelFactory(
    private val storiesRepository: StoriesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryListViewModel(storiesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// StoryManager.kt
package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoryManager(
    private val storiesRepository: StoriesRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _storyFeed = MutableStateFlow<List<StoryFeed>>(emptyList())
    val storyFeed: StateFlow<List<StoryFeed>> = _storyFeed.asStateFlow()

    private val _selectedUserStories = MutableStateFlow<List<Story>>(emptyList())
    val selectedUserStories: StateFlow<List<Story>> = _selectedUserStories.asStateFlow()

    private var currentUserId: Int? = null
    private var currentStoryIndex = 0

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStoryFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            storiesRepository.getStoryFeed(includeOwn = true).fold(
                onSuccess = { feed ->
                    if (feed.status){
                        _storyFeed.value = feed.data.feed
                    }

                },
                onFailure = { error ->
                    actionState.value = ActionState.Error("Failed to load stories: ${error.message}")
                }
            )
            _isLoading.value = false
        }
    }

    fun openUserStories(userId: Int) {
        if (currentUserId == userId && _selectedUserStories.value.isNotEmpty()) return
        currentUserId = userId
        currentStoryIndex = 0
        _selectedUserStories.value = emptyList()
        viewModelScope.launch {
            _isLoading.value = true
            storiesRepository.getUserStories(userId, includeExpired = false).fold(
                onSuccess = { paginated ->
                    _selectedUserStories.value = paginated.data.results
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error("Failed to load stories: ${error.message}")
                }
            )
            _isLoading.value = false
        }
    }

    fun getCurrentStory(): Story? = _selectedUserStories.value.getOrNull(currentStoryIndex)

    fun nextStory(): Boolean {
        if (currentStoryIndex + 1 < _selectedUserStories.value.size) {
            currentStoryIndex++
            return true
        }
        return false
    }

    fun previousStory(): Boolean {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            return true
        }
        return false
    }

    // Optional: record view when story is displayed
    fun recordCurrentStoryView() {
        val story = getCurrentStory() ?: return
        // If your API has an endpoint to record story views, call it here.
        // For example: storiesRepository.recordStoryView(story.id)
    }

    fun closeStoryViewer() {
        currentUserId = null
        _selectedUserStories.value = emptyList()
        currentStoryIndex = 0
    }

    fun clear() {
        _storyFeed.value = emptyList()
        _selectedUserStories.value = emptyList()
        currentUserId = null
        currentStoryIndex = 0
        _isLoading.value = false
    }

    // StoryManager.kt – add this method
    fun setStories(stories: List<Story>) {
        currentUserId = null
        currentStoryIndex = 0
        _selectedUserStories.value = stories
    }

}
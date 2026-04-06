package com.cyberarcenal.huddle.ui.profile.managers

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class HighlightManager(
    private val storiesRepository: StoriesRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _userHighlights = MutableStateFlow<List<StoryHighlight>>(emptyList())
    val userHighlights: StateFlow<List<StoryHighlight>> = _userHighlights.asStateFlow()

    private val _recentStories = MutableStateFlow<List<Story>>(emptyList())
    val recentStories: StateFlow<List<Story>> = _recentStories.asStateFlow()

    private val _isCreatingHighlight = MutableStateFlow(false)
    val isCreatingHighlight: StateFlow<Boolean> = _isCreatingHighlight.asStateFlow()

    private val _isUpdatingHighlight = MutableStateFlow(false)
    val isUpdatingHighlight: StateFlow<Boolean> = _isUpdatingHighlight.asStateFlow()

    private val _isDeletingHighlight = MutableStateFlow(false)
    val isDeletingHighlight: StateFlow<Boolean> = _isDeletingHighlight.asStateFlow()

    private val _currentHighlightDetail = MutableStateFlow<StoryHighlight?>(null)
    val currentHighlightDetail: StateFlow<StoryHighlight?> = _currentHighlightDetail.asStateFlow()

    fun loadUserHighlights() {
        viewModelScope.launch {
            storiesRepository.getHighlights().fold(
                onSuccess = { response ->
                    if (response.status) {
                        _userHighlights.value = response.data.highlights
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load highlights")
                }
            )
        }
    }

    fun loadPublicHighlights(userId: Int?) {
        if (userId == null) return
        viewModelScope.launch {
            storiesRepository.getPublicHighlight(userId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _userHighlights.value = response.data.highlights
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load user highlights")
                }
            )
        }
    }

    fun loadRecentStories() {
        viewModelScope.launch {
            storiesRepository.getMyStories(includeExpired = true, page = 1, pageSize = 50).fold(
                onSuccess = { paginated ->
                    if (paginated.status) {
                        val thirtyDaysAgo = OffsetDateTime.now().minusDays(30)
                        val recent = paginated.data.results?.filter { story ->
                            story.createdAt?.let { it >= thirtyDaysAgo } ?: false
                        } ?: emptyList()
                        _recentStories.value = recent
                    } else {
                        _recentStories.value = emptyList()
                    }
                },
                onFailure = {
                    _recentStories.value = emptyList()
                }
            )
        }
    }

    fun createHighlight(title: String, selectedStoryIds: List<Int>) {
        viewModelScope.launch {
            _isCreatingHighlight.value = true
            val request = StoryHighlightCreateRequest(title = title, storyIds = selectedStoryIds)
            storiesRepository.createHighlight(request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Highlight created")
                        loadUserHighlights()
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                    _isCreatingHighlight.value = false
                },
                onFailure = { error ->
                    _isCreatingHighlight.value = false
                    actionState.value = ActionState.Error(error.message ?: "Creation failed")
                }
            )
        }
    }

    fun updateHighlightTitle(highlightId: Int, title: String) {
        viewModelScope.launch {
            _isUpdatingHighlight.value = true
            val request = StoryHighlightUpdateRequest(title = title, storyIds = null)
            storiesRepository.updateHighlight(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Highlight updated")
                        loadUserHighlights()
                        _currentHighlightDetail.value?.let {
                            loadHighlightDetail(highlightId)
                        }
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                    _isUpdatingHighlight.value = false
                },
                onFailure = { error ->
                    _isUpdatingHighlight.value = false
                    actionState.value = ActionState.Error(error.message ?: "Update failed")
                }
            )
        }
    }

    fun deleteHighlight(highlightId: Int) {
        viewModelScope.launch {
            _isDeletingHighlight.value = true
            storiesRepository.deleteHighlight(highlightId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Highlight deleted")
                        loadUserHighlights()
                        _currentHighlightDetail.value = null
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                    _isDeletingHighlight.value = false
                },
                onFailure = { error ->
                    _isDeletingHighlight.value = false
                    actionState.value = ActionState.Error(error.message ?: "Deletion failed")
                }
            )
        }
    }

    fun addStoriesToHighlight(highlightId: Int, storyIds: List<Int>) {
        if (storyIds.isEmpty()) {
            actionState.value = ActionState.Error("No stories selected")
            return
        }
        viewModelScope.launch {
            actionState.value = ActionState.Loading()  // FIXED: added parentheses
            val request = StoryHighlightAddStoriesRequest(storyIds = storyIds)
            storiesRepository.addStoriesToHighlight(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Added to highlight")
                        loadUserHighlights()
                        _currentHighlightDetail.value?.let {
                            loadHighlightDetail(highlightId)
                        }
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to add stories")
                }
            )
        }
    }

    fun addStoryToHighlight(highlightId: Int, storyId: Int) {
        addStoriesToHighlight(highlightId, listOf(storyId))
    }

    fun removeStoriesFromHighlight(highlightId: Int, storyIds: List<Int>) {
        if (storyIds.isEmpty()) return
        viewModelScope.launch {
            actionState.value = ActionState.Loading()  // FIXED: added parentheses
            val request = StoryHighlightRemoveStoriesRequest(storyIds = storyIds)
            storiesRepository.removeStoriesFromHighlight(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Removed from highlight")
                        loadUserHighlights()
                        _currentHighlightDetail.value?.let {
                            loadHighlightDetail(highlightId)
                        }
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to remove stories")
                }
            )
        }
    }

    fun setHighlightCover(highlightId: Int, storyId: Int) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading()  // FIXED: added parentheses
            val request = StoryHighlightSetCoverRequest(coverStoryId = storyId)
            storiesRepository.setHighlightCover(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Cover updated")
                        loadUserHighlights()
                        _currentHighlightDetail.value?.let {
                            loadHighlightDetail(highlightId)
                        }
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to set cover")
                }
            )
        }
    }

    fun loadHighlightDetail(highlightId: Int) {
        viewModelScope.launch {
            storiesRepository.getHighlight(highlightId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _currentHighlightDetail.value = response.data.highlight
                    } else {
                        actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load detail")
                }
            )
        }
    }

    fun clearHighlightDetail() {
        _currentHighlightDetail.value = null
    }

    fun reset() {
        _isCreatingHighlight.value = false
        _isUpdatingHighlight.value = false
        _isDeletingHighlight.value = false
    }
}
package com.cyberarcenal.huddle.ui.profile.managers

import android.content.Context
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.HighlightCache
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class HighlightManager(
    private var userId: Int?,
    private val storiesRepository: StoriesRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private var highlightObservationJob: Job? = null

    init {
        observeHighlights()
    }

    fun updateUserId(newUserId: Int?) {
        if (userId != newUserId) {
            userId = newUserId
            observeHighlights()
        }
    }

    private fun observeHighlights() {
        highlightObservationJob?.cancel()
        userId?.let { uid ->
            highlightObservationJob = viewModelScope.launch {
                storiesRepository.observeHighlights(uid).collectLatest { highlights ->
                    _userHighlights.value = highlights
                }
            }
        }
    }

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

    fun loadUserHighlights(context: Context) {
        viewModelScope.launch {
            storiesRepository.fetchAndCacheHighlights(userId, context).onFailure { error ->
                actionState.value = ActionState.Error(error.message ?: "Failed to load highlights")
            }
        }
    }

    fun loadPublicHighlights(targetUserId: Int?, context: Context) {
        if (targetUserId == null) return
        viewModelScope.launch {
            storiesRepository.fetchAndCacheHighlights(targetUserId, context).onFailure { error ->
                actionState.value = ActionState.Error(error.message ?: "Failed to load user highlights")
            }
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

    fun createHighlight(title: String, selectedStoryIds: List<Int>, context: Context) {
        viewModelScope.launch {
            _isCreatingHighlight.value = true
            val request = StoryHighlightCreateRequest(title = title, storyIds = selectedStoryIds)
            storiesRepository.createHighlight(request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Highlight created")
                        loadUserHighlights(context)
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

    fun updateHighlightTitle(highlightId: Int, title: String, context: Context) {
        viewModelScope.launch {
            _isUpdatingHighlight.value = true
            val request = StoryHighlightUpdateRequest(title = title, storyIds = null)
            storiesRepository.updateHighlight(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Highlight updated")
                        loadUserHighlights(context)
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

    fun deleteHighlight(highlightId: Int, context: Context) {
        viewModelScope.launch {
            _isDeletingHighlight.value = true
            storiesRepository.deleteHighlight(highlightId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Highlight deleted")
                        loadUserHighlights(context)
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

    fun addStoriesToHighlight(highlightId: Int, storyIds: List<Int>, context: Context) {
        if (storyIds.isEmpty()) {
            actionState.value = ActionState.Error("No stories selected")
            return
        }
        viewModelScope.launch {
            actionState.value = ActionState.Loading()
            val request = StoryHighlightAddStoriesRequest(storyIds = storyIds)
            storiesRepository.addStoriesToHighlight(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Added to highlight")
                        loadUserHighlights(context)
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

    fun addStoryToHighlight(highlightId: Int, storyId: Int, context: Context) {
        addStoriesToHighlight(highlightId, listOf(storyId), context)
    }

    fun removeStoriesFromHighlight(highlightId: Int, storyIds: List<Int>, context: Context) {
        if (storyIds.isEmpty()) return
        viewModelScope.launch {
            actionState.value = ActionState.Loading()
            val request = StoryHighlightRemoveStoriesRequest(storyIds = storyIds)
            storiesRepository.removeStoriesFromHighlight(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Removed from highlight")
                        loadUserHighlights(context)
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

    fun setHighlightCover(highlightId: Int, storyId: Int, context: Context) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading()
            val request = StoryHighlightSetCoverRequest(coverStoryId = storyId)
            storiesRepository.setHighlightCover(highlightId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        actionState.value = ActionState.Success("Cover updated")
                        loadUserHighlights(context)
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
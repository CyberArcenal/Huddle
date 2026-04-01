package com.cyberarcenal.huddle.ui.profile.managers

import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryHighlight
import com.cyberarcenal.huddle.api.models.StoryHighlightCreateRequest
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

    fun loadUserHighlights() {
        viewModelScope.launch {
            storiesRepository.getHighlights().fold(
                onSuccess = { response ->
                    if (response.status) {
                        _userHighlights.value = response.data.highlights
                    }
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun loadPublicHighlights(userId: Int?) {
        if (userId == null)return;

        viewModelScope.launch {
            storiesRepository.getPublicHighlight(userId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _userHighlights.value = response.data.highlights
                    }
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun loadRecentStories() {
        viewModelScope.launch {
            storiesRepository.getMyStories(includeExpired = true, page = 1, pageSize = 50).fold(
                onSuccess = { paginated ->
                    if (paginated.status){
                        val thirtyDaysAgo = OffsetDateTime.now().minusDays(30)
                        val recent = paginated.data?.results?.filter { story ->
                            story.createdAt?.let { it >= thirtyDaysAgo } ?: false
                        }
                        if (recent !== null){
                            _recentStories.value = recent
                        }

                    }

                },
                onFailure = { _recentStories.value = emptyList() }
            )
        }
    }

    fun createHighlight(title: String, selectedStoryIds: List<Int>) {
        viewModelScope.launch {
            _isCreatingHighlight.value = true
            val request = StoryHighlightCreateRequest(title = title, storyIds = selectedStoryIds)
            storiesRepository.createHighlight(request).fold(
                onSuccess = {
                    loadUserHighlights()
                    _isCreatingHighlight.value = false
                },
                onFailure = { error ->
                    _isCreatingHighlight.value = false
                    actionState.value =
                        ActionState.Error(error.message ?: "Failed to add highlights")
                }
            )
        }
    }
}
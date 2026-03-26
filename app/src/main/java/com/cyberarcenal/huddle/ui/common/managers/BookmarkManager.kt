// BookmarkManager.kt
package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.BookmarkActionRequest
import com.cyberarcenal.huddle.data.repositories.BookmarksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkManager(
    private val bookmarksRepository: BookmarksRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _bookmarkCount = MutableStateFlow(0)
    val bookmarkCount: StateFlow<Int> = _bookmarkCount.asStateFlow()

    private var currentTarget: Pair<String, Int>? = null

    fun setTarget(contentType: String, objectId: Int) {
        if (currentTarget == contentType to objectId) return
        currentTarget = contentType to objectId
        loadStats(contentType, objectId)
    }

    private fun loadStats(contentType: String, objectId: Int) {
        viewModelScope.launch {
            bookmarksRepository.getStatistics(objectId, contentType).fold(
                onSuccess = { stats ->
                    _isBookmarked.value = stats.hasBookmarked
                    _bookmarkCount.value = stats.bookmarkCount
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load bookmark status")
                }
            )
        }
    }

    fun toggleBookmark() {
        val (contentType, objectId) = currentTarget ?: return
        viewModelScope.launch {
            val previousBookmarked = _isBookmarked.value
            val previousCount = _bookmarkCount.value
            // Optimistic update
            _isBookmarked.value = !previousBookmarked
            _bookmarkCount.value = if (previousBookmarked) previousCount - 1 else previousCount + 1

            val request = BookmarkActionRequest(targetType = contentType, targetId = objectId)
            bookmarksRepository.createBookmark(request).fold(
                onSuccess = { response ->
                    // Sync with actual response
                    _isBookmarked.value = true
                },
                onFailure = { error ->
                    // Revert
                    _isBookmarked.value = previousBookmarked
                    _bookmarkCount.value = previousCount
                    actionState.value = ActionState.Error(error.message ?: "Failed to update bookmark")
                }
            )
        }
    }

    fun clear() {
        currentTarget = null
        _isBookmarked.value = false
        _bookmarkCount.value = 0
    }
}
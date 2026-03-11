package com.cyberarcenal.huddle.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.repositories.feed.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {
    private val repository = FeedRepository()

    val feedPagingFlow: Flow<PagingData<PostFeed>> = Pager(
        PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            prefetchDistance = 2,
            enablePlaceholders = false
        )
    ) {
        FeedPagingSource(repository)
    }.flow.cachedIn(viewModelScope)

    private val _likeEvents = MutableSharedFlow<LikeResult>()
    val likeEvents = _likeEvents.asSharedFlow()

    // Scroll to Top Event
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    fun requestScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    fun toggleLike(postId: Int, currentLiked: Boolean, currentCount: Int) {
        viewModelScope.launch {
            val result = repository.toggleLike(postId)
            result.onSuccess { response ->
                _likeEvents.emit(
                    LikeResult.Success(
                        postId = postId,
                        liked = response.liked ?: false,
                        likeCount = response.likeCount ?: 0
                    )
                )
            }.onFailure { error ->
                _likeEvents.emit(LikeResult.Error(postId, error.message ?: "Unknown error"))
            }
        }
    }
}

sealed class LikeResult {
    data class Success(val postId: Int, val liked: Boolean, val likeCount: Int) : LikeResult()
    data class Error(val postId: Int, val message: String) : LikeResult()
}

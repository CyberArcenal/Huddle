package com.cyberarcenal.huddle.ui.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Comment
import com.cyberarcenal.huddle.api.models.CommentCreate
import com.cyberarcenal.huddle.api.models.PostDetail
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.data.repositories.feed.FeedRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class PostDetailViewModel(
    private val postId: Int,
    private val feedRepository: FeedRepository
) : ViewModel() {

    // Post detail state
    private val _postState = MutableStateFlow<PostDetail?>(null)
    val postState: StateFlow<PostDetail?> = _postState.asStateFlow()

    private val _postLoading = MutableStateFlow(false)
    val postLoading: StateFlow<Boolean> = _postLoading.asStateFlow()

    private val _postError = MutableStateFlow<String?>(null)
    val postError: StateFlow<String?> = _postError.asStateFlow()

    // Comment input
    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _sendingComment = MutableStateFlow(false)
    val sendingComment: StateFlow<Boolean> = _sendingComment.asStateFlow()

    // Comments paging
    val commentsFlow: Flow<PagingData<Comment>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { CommentsPagingSource(postId, feedRepository) }
    ).flow.cachedIn(viewModelScope)

    init {
        loadPost()
    }

    fun loadPost() {
        viewModelScope.launch {
            _postLoading.value = true
            _postError.value = null
            val result = feedRepository.getPost(postId)
            result.fold(
                onSuccess = { post -> _postState.value = post },
                onFailure = { error -> _postError.value = error.message ?: "Failed to load post" }
            )
            _postLoading.value = false
        }
    }

    fun updateCommentText(text: String) {
        _commentText.value = text
    }

    fun sendComment() {
        val text = _commentText.value.trim()
        if (text.isEmpty() || _sendingComment.value) return

        viewModelScope.launch {
            _sendingComment.value = true
            // Create comment object – user_id is set by server based on auth token
            val comment = CommentCreate(
                postId = postId,
                userId = 0,
                content = text,
                parentCommentId = null
            )
            val result = feedRepository.createComment(comment)
            result.fold(
                onSuccess = {
                    _commentText.value = ""
                    // Refresh comments – easiest is to invalidate paging source
                    // We can trigger a refresh by updating a state or using a different approach.
                    // For now, we'll rely on the user pulling to refresh, or we could emit a refresh event.
                    // A better way: use a `refreshTrigger` state and observe it in the paging source.
                    // We'll implement a simple refresh callback.
                },
                onFailure = { error ->
                    // Show error via snackbar later (we'll pass a callback to screen)
                }
            )
            _sendingComment.value = false
        }
    }

    fun toggleLike() {
        val currentPost = _postState.value ?: return
        // Optimistic update
        val updatedPost = currentPost.copy(
            liked = !currentPost.liked,
            likeCount = if (currentPost.liked) currentPost.likeCount - 1 else currentPost.likeCount + 1
        )
        _postState.value = updatedPost

        viewModelScope.launch {
            val result = feedRepository.toggleLike(postId)
            result.fold(
                onSuccess = { response ->
                    // Server returned new like status, but we already updated optimistically.
                    // Optionally sync with server's response.
                },
                onFailure = { error ->
                    // Revert optimistic update
                    _postState.value = currentPost
                    // Show error via snackbar
                }
            )
        }
    }

    fun refreshPost() {
        loadPost()
    }
}

// Simple PagingSource for comments
class CommentsPagingSource(
    private val postId: Int,
    private val feedRepository: FeedRepository
) : androidx.paging.PagingSource<Int, Comment>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        return try {
            val page = params.key ?: 1
            val result = feedRepository.getComments(
                postId = postId,
                page = page,
                pageSize = params.loadSize
            )
            result.fold(
                onSuccess = { data ->
                    LoadResult.Page(
                        data = data.results,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (data.next == null) null else page + 1
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
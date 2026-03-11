package com.cyberarcenal.huddle.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Comment
import com.cyberarcenal.huddle.api.models.CommentCreateRequest
import com.cyberarcenal.huddle.api.models.LikeContentTypeEnum
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.repositories.feed.FeedRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {
    private val repository = FeedRepository()

    // Current user ID (set from outside)
    private var _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Int?) {
        _currentUserId.value = userId
    }

    // Feed posts paging
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

    // Like events
    private val _likeEvents = MutableSharedFlow<LikeResult>()
    val likeEvents = _likeEvents.asSharedFlow()

    // Scroll to top event
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    fun requestScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    fun toggleLike(postId: Int?, currentLiked: Boolean?, currentCount: Int?) {
        if (postId == null) return
        viewModelScope.launch {
            val result = repository.toggleLike(LikeContentTypeEnum.POST, postId)
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

    // ========== BOTTOM SHEET STATES ==========
    private val _commentSheetState = MutableStateFlow<CommentSheetState?>(null)
    val commentSheetState: StateFlow<CommentSheetState?> = _commentSheetState.asStateFlow()

    private val _optionsSheetState = MutableStateFlow<OptionsSheetState?>(null)
    val optionsSheetState: StateFlow<OptionsSheetState?> = _optionsSheetState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Top-level comments (no parent)
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError.asStateFlow()

    // Replies map: parentId -> list of replies
    private val _replies = MutableStateFlow<Map<Int, List<Comment>>>(emptyMap())
    val replies: StateFlow<Map<Int, List<Comment>>> = _replies.asStateFlow()

    private val _expandedReplies = MutableStateFlow<Set<Int>>(emptySet())
    val expandedReplies: StateFlow<Set<Int>> = _expandedReplies.asStateFlow()

    // Pagination for comments
    private var _commentPage = MutableStateFlow(1)
    private var _hasMoreComments = MutableStateFlow(true)
    private var _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPostId: Int? = null

    fun openCommentSheet(postId: Int?) {
        if (postId == null) return
        currentPostId = postId
        _commentSheetState.value = CommentSheetState(postId)
        // Reset pagination states
        _commentPage.value = 1
        _hasMoreComments.value = true
        _comments.value = emptyList()
        _replies.value = emptyMap()
        loadComments(postId, page = 1, replace = true)
    }

    fun openOptionsSheet(post: PostFeed) {
        _optionsSheetState.value = OptionsSheetState(post)
    }

    fun dismissCommentSheet() {
        _commentSheetState.value = null
        _comments.value = emptyList()
        _commentsError.value = null
        _replies.value = emptyMap()
        _expandedReplies.value = emptySet()
        currentPostId = null
        _commentPage.value = 1
        _hasMoreComments.value = true
        _isLoadingMore.value = false
    }

    fun dismissOptionsSheet() {
        _optionsSheetState.value = null
    }

    private fun loadComments(postId: Int, page: Int, replace: Boolean) {
        viewModelScope.launch {
            if (page == 1) {
                _actionState.value = ActionState.Loading()
                _commentsError.value = null
            } else {
                _isLoadingMore.value = true
            }

            repository.getComments(postId = postId, page = page, pageSize = 20).fold(
                onSuccess = { paginated ->
                    val allComments = paginated.results
                    val topLevel = mutableListOf<Comment>()
                    val repliesMap = mutableMapOf<Int, MutableList<Comment>>()

                    allComments.forEach { comment ->
                        if (comment.parentComment == null) {
                            topLevel.add(comment)
                        } else {
                            repliesMap.getOrPut(comment.parentComment) { mutableListOf() }.add(comment)
                        }
                    }

                    if (replace) {
                        _comments.value = topLevel.reversed() // newest first
                        _replies.value = repliesMap
                    } else {
                        // Append new top-level comments and merge replies
                        _comments.value = (_comments.value + topLevel.reversed())
                        _replies.value = _replies.value.toMutableMap().apply {
                            repliesMap.forEach { (parentId, newReplies) ->
                                val existing = this[parentId] ?: emptyList()
                                this[parentId] = existing + newReplies
                            }
                        }
                    }

                    _hasMoreComments.value = paginated.hasNext
                    _commentPage.value = page + 1
                    _actionState.value = ActionState.Idle
                    _isLoadingMore.value = false
                },
                onFailure = { error ->
                    if (page == 1) {
                        _commentsError.value = error.message ?: "Failed to load comments"
                        _actionState.value = ActionState.Error(_commentsError.value!!)
                    } else {
                        _actionState.value = ActionState.Error(error.message ?: "Failed to load more comments")
                    }
                    _isLoadingMore.value = false
                }
            )
        }
    }

    fun loadMoreComments() {
        val postId = currentPostId ?: return
        if (!_hasMoreComments.value || _isLoadingMore.value) return
        loadComments(postId, page = _commentPage.value, replace = false)
    }

    fun addComment(content: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Posting comment...")
            val request = CommentCreateRequest(
                postId = postId,
                content = content,
                parentCommentId = null
            )
            repository.createComment(request).fold(
                onSuccess = { newComment ->
                    _comments.value = listOf(newComment) + _comments.value
                    _actionState.value = ActionState.Success("Comment added")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to post comment")
                }
            )
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            repository.deleteComment(commentId).fold(
                onSuccess = {
                    _comments.value = _comments.value.filter { it.id != commentId }
                    _replies.value = _replies.value.filterKeys { it != commentId }
                    _actionState.value = ActionState.Success("Comment deleted")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete comment")
                }
            )
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Deleting post...")
            repository.deletePost(postId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Post deleted")
                    dismissOptionsSheet()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete post")
                }
            )
        }
    }

    fun reportPost(postId: Int, reason: String) {
        // TODO: Implement report API call when available
        _actionState.value = ActionState.Success("Reported (not implemented)")
        dismissOptionsSheet()
    }

    fun toggleReplyExpansion(commentId: Int?) {
        if (commentId == null) return
        val current = _expandedReplies.value
        _expandedReplies.value = if (commentId in current) {
            current.minus(commentId)
        } else {
            current.plus(commentId)
        }
    }

    fun loadReplies(commentId: Int?) {
        if (commentId == null) return
        if (_replies.value.containsKey(commentId)) return
        viewModelScope.launch {
            repository.getReplies(commentId, page = 1, pageSize = 20).fold(
                onSuccess = { paginated ->
                    _replies.value = _replies.value + (commentId to paginated.results)
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to load replies")
                }
            )
        }
    }

    fun likeComment(commentId: Int?) {
        if (commentId == null) return
        viewModelScope.launch {
            repository.toggleLike(LikeContentTypeEnum.COMMENT, commentId).fold(
                onSuccess = { response ->
                    // Update in top-level comments
                    _comments.value = _comments.value.map { comment ->
                        if (comment.id == commentId) {
                            comment.copy(
                                likeCount = response.likeCount ?: comment.likeCount,
                                hasLiked = response.liked ?: comment.hasLiked
                            )
                        } else comment
                    }
                    // Update in replies map
                    _replies.value = _replies.value.mapValues { (_, repliesList) ->
                        repliesList.map { reply ->
                            if (reply.id == commentId) {
                                reply.copy(
                                    likeCount = response.likeCount ?: reply.likeCount,
                                    hasLiked = response.liked ?: reply.hasLiked
                                )
                            } else reply
                        }
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to like comment")
                }
            )
        }
    }

    fun addReply(parentCommentId: Int?, content: String) {
        if (parentCommentId == null) return
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Posting reply...")
            val request = CommentCreateRequest(
                postId = postId,
                content = content,
                parentCommentId = parentCommentId
            )
            repository.createComment(request).fold(
                onSuccess = { newReply ->
                    _replies.value = _replies.value.toMutableMap().apply {
                        val currentReplies = this[parentCommentId] ?: emptyList()
                        this[parentCommentId] = listOf(newReply) + currentReplies
                    }
                    _expandedReplies.value = _expandedReplies.value + parentCommentId
                    _actionState.value = ActionState.Success("Reply added")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to post reply")
                }
            )
        }
    }
}

// ========== SEALED CLASSES ==========
sealed class LikeResult {
    data class Success(val postId: Int, val liked: Boolean, val likeCount: Int) : LikeResult()
    data class Error(val postId: Int, val message: String) : LikeResult()
}

data class CommentSheetState(val postId: Int)
data class OptionsSheetState(val post: PostFeed)

sealed class ActionState {
    object Idle : ActionState()
    data class Loading(val message: String? = null) : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}
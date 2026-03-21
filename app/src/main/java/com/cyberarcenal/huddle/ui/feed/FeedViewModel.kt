package com.cyberarcenal.huddle.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.data.repositories.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedType: FeedType,
    val postRepository: UserPostsRepository,          // changed
    val feedRepository: FeedRepository,
    private val commentRepository: CommentsRepository,        // changed
    private val reactionsRepository: UserReactionsRepository, // new
    private val storyFeedRepository: StoriesRepository
) : ViewModel() {

    // Current user ID (set from outside)
    private var _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Int?) {
        _currentUserId.value = userId
    }

    // Stories state
    private val _stories = MutableStateFlow<List<StoryFeed>>(emptyList())
    val stories: StateFlow<List<StoryFeed>> = _stories.asStateFlow()

    private val _storiesLoading = MutableStateFlow(false)
    val storiesLoading: StateFlow<Boolean> = _storiesLoading.asStateFlow()

    fun loadStories() {
        viewModelScope.launch {
            _storiesLoading.value = true
            storyFeedRepository.getStoryFeed(includeOwn = true)
                .onSuccess { _stories.value = it }
                .onFailure { error ->
                    _actionState.value = ActionState.Error("Failed to load stories: ${error.message}")
                }
            _storiesLoading.value = false
        }
    }

    // Feed posts paging
    val feedPagingFlow: Flow<PagingData<FeedRow>> = Pager(
        PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            prefetchDistance = 2,
            enablePlaceholders = false
        )
    ) {
        FeedPagingSource(feedRepository, feedType)   // will need to update FeedPagingSource to use
    // UserPostsRepository
    }.flow.cachedIn(viewModelScope)

    // Reaction events (for both posts and comments)
    private val _reactionEvents = MutableSharedFlow<ReactionResult>()
    val reactionEvents = _reactionEvents.asSharedFlow()

    // Scroll to top event
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    fun requestScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    // Send a reaction (like, love, etc.) to a post
    fun sendPostReaction(objectId: Int,  reactionType:
    ReactionType?, contentType: String = "feed.post") {
        viewModelScope.launch {
            val request = ReactionCreateRequest(
                contentType = contentType,
                objectId = objectId,
                reactionType = reactionType
            )
            val result = reactionsRepository.createReaction(request)
            result.onSuccess { response ->
                _reactionEvents.emit(
                    ReactionResult.PostSuccess(
                        postId = objectId,
                        liked = response.reacted,
                        reactionType = response.reactionType as ReactionCreateRequest.ReactionType,
                        counts = response.counts
                    )
                )
            }.onFailure { error ->
                _reactionEvents.emit(
                    ReactionResult.Error(objectId, error.message ?: "Unknown error")
                )
            }
        }
    }


    // FeedViewModel.kt (add inside class FeedViewModel)

    init {
        viewModelScope.launch {
            reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.CommentSuccess -> {
                        updateCommentReaction(
                            commentId = result.commentId,
                            liked = result.liked,
                            reactionType = result.reactionType,
                            likeCount = result.likeCount
                        )
                    }
                    is ReactionResult.PostSuccess -> {
                        // Opsiyonal: i‑update ang post state kung mayroon kang flow para sa posts
                        // Halimbawa, kung may _postsStateFlow, dito mo i‑update
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateCommentReaction(
        commentId: Int,
        liked: Boolean,
        reactionType: ReactionType?,
        likeCount: Int
    ) {
        // 1. Update top-level comments
        _comments.update { currentComments ->
            currentComments.map { comment ->
                if (comment.id == commentId) {
                    comment.copy(
                        liked = liked,
                        userReaction = reactionType as UserReactionA51Enum,
                        likeCount = likeCount
                    )
                } else comment
            }
        }

        // 2. Update replies inside _replies map
        _replies.update { currentReplies ->
            currentReplies.mapValues { (_, repliesList) ->
                repliesList.map { reply ->
                    if (reply.id == commentId) {
                        reply.copy(
                            liked = liked,
                            userReaction = reactionType as UserReactionA51Enum,
                            likeCount = likeCount
                        )
                    } else reply
                }
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
    private val _comments = MutableStateFlow<List<CommentDisplay>>(emptyList())
    val comments: StateFlow<List<CommentDisplay>> = _comments.asStateFlow()

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError.asStateFlow()

    // Replies map: parentId -> list of replies
    private val _replies = MutableStateFlow<Map<Int, List<CommentDisplay>>>(emptyMap())
    val replies: StateFlow<Map<Int, List<CommentDisplay>>> = _replies.asStateFlow()

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

            // Note: getComments expects postId, not necessarily a path param; the repository uses the correct endpoint.
            commentRepository.getComments(postId = postId, page = page, pageSize = 20).fold(
                onSuccess = { paginated ->
                    val allComments = paginated.results
                    val topLevel = mutableListOf<CommentDisplay>()
                    val repliesMap = mutableMapOf<Int, MutableList<CommentDisplay>>()

                    allComments.forEach { comment ->
                        if (comment.parentComment == null) {
                            topLevel.add(comment)
                        } else {
                            repliesMap.getOrPut(comment.parentComment) { mutableListOf() }.add(comment)
                        }
                    }

                    if (replace) {
                        _comments.value = topLevel.reversed()
                        _replies.value = repliesMap
                    } else {
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
                targetId = postId,
                content = content,
                parentCommentId = null,
                targetType = "feed.post"
            )
            commentRepository.createComment(request).fold(
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
            commentRepository.deleteComment(commentId).fold(
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
            postRepository.deletePost(postId).fold(
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
            commentRepository.getReplies(commentId, page = 1, pageSize = 20).fold(
                onSuccess = { paginated ->
                    _replies.value = _replies.value + (commentId to paginated.results)
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to load replies")
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
                targetId = postId,
                content = content,
                parentCommentId = parentCommentId,
                targetType = "feed.post"
            )
            commentRepository.createComment(request).fold(
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

// ========== FACTORY ==========
class FeedViewModelFactory(
    private val feedType: FeedType,
    private val postRepository: UserPostsRepository,
    private val feedRepository: FeedRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val storyFeedRepository: StoriesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(
                feedType,
                postRepository,
                feedRepository,
                commentRepository,
                reactionsRepository,
                storyFeedRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ========== SEALED CLASSES ==========
sealed class ReactionResult {
    data class PostSuccess(
        val postId: Int,
        val liked: Boolean,
        val reactionType: ReactionType?,
        val counts: ReactionCount
    ) : ReactionResult()

    data class CommentSuccess(
        val commentId: Int,
        val liked: Boolean,
        val likeCount: Int,
        val reactionType: ReactionType?
    ) : ReactionResult()

    data class Error(val id: Int, val message: String) : ReactionResult()
}

data class CommentSheetState(val postId: Int)
data class OptionsSheetState(val post: PostFeed)

sealed class ActionState {
    object Idle : ActionState()
    data class Loading(val message: String? = null) : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}
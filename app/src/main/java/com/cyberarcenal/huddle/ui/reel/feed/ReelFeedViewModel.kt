package com.cyberarcenal.huddle.ui.reel.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.CommentManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReelFeedViewModel(
    private val reelsRepository: ReelsRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val sharePostsRepository: SharePostsRepository
) : ViewModel() {

    // Action state
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Managers
    val commentManager = CommentManager(
        commentRepository = commentRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val reactionManager = ReactionManager(
        reactionRepository = reactionsRepository,
        viewModelScope = viewModelScope
    )

    // Expose comment manager states
    val commentSheetState = commentManager.commentSheetState
    val comments = commentManager.comments
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore

    // Reel-specific local state (optimistic updates)
    private val _localReelStates = MutableStateFlow<Map<Int, ReelState>>(emptyMap())
    val localReelStates = _localReelStates.asStateFlow()

    data class ReelState(
        val hasLiked: Boolean,
        val likeCount: Int
    )

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Int?) {
        _currentUserId.value = userId
    }

    // Paging flow
    val reelsPagingFlow: Flow<PagingData<ReelDisplay>> = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        ReelPagingSource(reelsRepository)
    }.flow.cachedIn(viewModelScope)

    // Share reel
    fun shareReel(reel: ReelDisplay) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Sharing...")
            val request = ShareCreateRequest(
                contentType = "reel",
                objectId = reel.id ?: return@launch,
                caption = null,
                privacy = PrivacyB23Enum.PUBLIC,
                group = null
            )
            sharePostsRepository.createShare(request)
                .onSuccess { _actionState.value = ActionState.Success("Shared successfully") }
                .onFailure { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to share")
                }
        }
    }

    // Reaction handling (optimistic update for reels, then let reactionManager do the API call)
    fun sendReaction(request: ReactionCreateRequest) {
        // Optimistic update for reel
        if (request.contentType == "reel") {
            updateLocalReelState(request.objectId, request.reactionType == ReactionTypeEnum.LIKE)
        }
        // Delegate to reactionManager
        reactionManager.sendReaction(request)
    }

    private fun updateLocalReelState(reelId: Int, isLiked: Boolean) {
        _localReelStates.update { current ->
            val currentState = current[reelId]
            val newCount = if (isLiked) {
                (currentState?.likeCount ?: 0) + 1
            } else {
                maxOf(0, (currentState?.likeCount ?: 1) - 1)
            }
            current + (reelId to ReelState(hasLiked = isLiked, likeCount = newCount))
        }
    }

    // Comment operations – delegate to manager
    fun openCommentSheet(reelId: Int) = commentManager.openCommentSheet("reel", reelId)
    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun addReply(parentCommentId: Int?, content: String) = commentManager.addReply(parentCommentId, content)
    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)

    fun clearActionState() {
        _actionState.value = ActionState.Idle
    }

    init {
        // Listen for reaction events to update comments and reel states
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        when (result.contentType) {
                            "comment" -> {
                                commentManager.updateCommentReaction(
                                    commentId = result.objectId,
                                    reacted = result.reacted,
                                    reactionType = result.reactionType as ReactionTypeEnum?,
                                    reactionCount = result.reactionCount,
                                    counts = result.counts
                                )
                            }
                            "reel" -> {
                                // Update local reel state with server response
                                _localReelStates.update { current ->
                                    current + (result.objectId to ReelState(
                                        hasLiked = result.reacted,
                                        likeCount = result.counts.like ?: 0
                                    ))
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

class ReelPagingSource(
    private val reelsRepository: ReelsRepository
) : PagingSource<Int, ReelDisplay>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReelDisplay> {
        return try {
            val page = params.key ?: 1
            val result = reelsRepository.getReels(page = page, pageSize = params.loadSize)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                LoadResult.Page(
                    data = data.results,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (data.hasNext) page + 1 else null
                )
            } else {
                LoadResult.Error(Exception("Failed to load reels"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ReelDisplay>): Int? {
        return state.anchorPosition?.let { state.closestPageToPosition(it)?.prevKey?.plus(1) }
    }
}

class ReelFeedViewModelFactory(
    private val reelsRepository: ReelsRepository,
    private val commentsRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val sharePostsRepository: SharePostsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReelFeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReelFeedViewModel(
                reelsRepository,
                commentsRepository,
                reactionsRepository,
                sharePostsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
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
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.CommentManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionResult
import com.cyberarcenal.huddle.ui.common.managers.ShareManager
import com.cyberarcenal.huddle.ui.common.managers.ShareResult
import com.cyberarcenal.huddle.ui.common.managers.FollowManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReelFeedViewModel(
    private val reelsRepository: ReelsRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
    private val targetUserId: Int? = null
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

    val shareManager = ShareManager(
        shareRepository = sharePostsRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val followManager = FollowManager(
        followRepository = followRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    // Expose manager states
    val commentSheetState = commentManager.commentSheetState
    val comments = commentManager.comments
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Int?) {
        _currentUserId.value = userId
    }

    // Paging flow
    val reelsPagingFlow: Flow<PagingData<ReelDisplay>> = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        ReelPagingSource(reelsRepository, targetUserId)
    }.flow.cachedIn(viewModelScope)

    // Share reel
    fun shareReel(shareData: ShareRequestData) = shareManager.sharePost(shareData)

    // Follow
    fun toggleFollow(userId: Int, currentIsFollowing: Boolean, username: String) =
        followManager.toggleFollow(userId, currentIsFollowing, username)

    // Reel operations
    fun deleteReel(reelId: Int) {
        viewModelScope.launch {
            reelsRepository.deleteReel(reelId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Reel deleted successfully")
                },
                onFailure = {
                    _actionState.value = ActionState.Error(it.message ?: "Failed to delete reel")
                }
            )
        }
    }

    // Reaction handling
    fun sendReaction(request: ReactionCreateRequest) = reactionManager.sendReaction(request)

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
        // Listen for reaction events
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        if (result.contentType == "comment") {
                            commentManager.updateCommentReaction(
                                commentId = result.objectId,
                                reacted = result.reacted,
                                reactionType = result.reactionType as ReactionTypeEnum?,
                                reactionCount = result.reactionCount,
                                counts = result.counts
                            )
                        }
                    }
                    is ReactionResult.Error -> {
                        _actionState.value = ActionState.Error(result.message)
                    }
                }
            }
        }

        // Listen for share events
        viewModelScope.launch {
            shareManager.shareEvents.collect { result ->
                when (result) {
                    is ShareResult.Success -> {
                        _actionState.value = ActionState.Success("Shared successfully")
                    }
                    is ShareResult.Error -> {
                        _actionState.value = ActionState.Error(result.message)
                    }
                }
            }
        }
    }
}

class ReelPagingSource(
    private val reelsRepository: ReelsRepository,
    private val userId: Int? = null
) : PagingSource<Int, ReelDisplay>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReelDisplay> {
        return try {
            val page = params.key ?: 1
            reelsRepository.getReels(page = page, pageSize = params.loadSize, userId = userId).fold(
                onSuccess = { response ->
                    if (response.status){
                        LoadResult.Page(
                            data = response.data?.pagination?.results!!,
                            prevKey = if (page == 1) null else page - 1,
                            nextKey = if (response.data.pagination.hasNext) page + 1 else null
                        )
                    }else{
                        LoadResult.Page(
                            data = emptyList(),
                            prevKey = if (page == 1) null else page - 1,
                            nextKey = if (response.data?.pagination?.hasNext!!) page + 1 else null
                        )
                    }
                },
                onFailure = { return LoadResult.Error(it) }
            )

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
    private val reactionsRepository: ReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
    private val targetUserId: Int? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReelFeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReelFeedViewModel(
                reelsRepository,
                commentsRepository,
                reactionsRepository,
                sharePostsRepository,
                followRepository,
                targetUserId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

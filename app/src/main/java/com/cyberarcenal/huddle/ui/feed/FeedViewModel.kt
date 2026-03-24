package com.cyberarcenal.huddle.ui.feed

import android.util.Log
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
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.CommentManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedType: FeedType,
    val postRepository: UserPostsRepository,
    val feedRepository: FeedRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val storyFeedRepository: StoriesRepository,
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

    // Expose manager states for easy UI access (optional)
    val commentSheetState = commentManager.commentSheetState
    val optionsSheetState = commentManager.optionsSheetState
    val comments = commentManager.comments
    val commentsError = commentManager.commentsError
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore

    // Current user
    private var _currentUserId = MutableStateFlow<Int?>(null)
    private var _currentUser = MutableStateFlow<UserProfile?>(null)

    fun setCurrentUserId(userId: Int?) { _currentUserId.value = userId }
    fun setCurrentUserData(currentUserData: UserProfile?) { _currentUser.value = currentUserData }

    // Stories
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

    // Feed paging
    val feedPagingFlow: Flow<PagingData<UnifiedContentItem>> = Pager(
        PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            prefetchDistance = 2,
            enablePlaceholders = false
        )
    ) {
        FeedPagingSource(feedRepository, feedType)
    }.flow.cachedIn(viewModelScope)

    // Scroll to top
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    fun requestScrollToTop() {
        viewModelScope.launch { _scrollToTopEvent.emit(Unit) }
    }

    // Share post
    fun sharePost(shareData: ShareRequestData) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Sharing...")
            val request = ShareCreateRequest(
                contentType = shareData.contentType,
                objectId = shareData.contentId,
                caption = shareData.caption,
                privacy = shareData.privacy,
                group = shareData.groupId
            )
            sharePostsRepository.createShare(request).fold(
                onSuccess = { _actionState.value = ActionState.Success("Shared successfully") },
                onFailure = { error -> _actionState.value = ActionState.Error(error.message ?: "Failed to share") }
            )
        }
    }

    // Reactions
    fun sendReaction(data: ReactionCreateRequest) = reactionManager.sendReaction(data)

    // Comments – delegate to manager
    fun openCommentSheet(contentType: String, objectId: Int) = commentManager.openCommentSheet(contentType, objectId)
    fun openOptionsSheet(post: PostFeed) = commentManager.openOptionsSheet(post)
    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun dismissOptionsSheet() = commentManager.dismissOptionsSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun deleteComment(commentId: Int) = commentManager.deleteComment(commentId)
    fun addReply(parentCommentId: Int?, content: String) = commentManager.addReply(parentCommentId, content)
    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)

    // Delete post
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
        _actionState.value = ActionState.Success("Reported (not implemented)")
        dismissOptionsSheet()
    }

    init {
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        when (result.contentType) {
                            "comment" -> {
                                commentManager.updateCommentReaction(
                                    commentId = result.objectId,
                                    reacted = result.reacted,
                                    reactionType = result.reactionType as ReactionType?,
                                    reactionCount = result.reactionCount,
                                    counts = result.counts
                                )
                            }
                            "post", "reel" -> { /* update post/reel if needed */ }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// Factory remains unchanged
class FeedViewModelFactory(
    private val feedType: FeedType,
    private val postRepository: UserPostsRepository,
    private val feedRepository: FeedRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val storyFeedRepository: StoriesRepository,
    private val sharePostsRepository: SharePostsRepository
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
                storyFeedRepository,
                sharePostsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
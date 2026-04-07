package com.cyberarcenal.huddle.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.*
import com.cyberarcenal.huddle.ui.feed.dataclass.FeedType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedType: FeedType,
    val postRepository: UserPostsRepository,
    val feedRepository: FeedRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val storyFeedRepository: StoriesRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
    private  val userMediaRepository: UserMediaRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger: SharedFlow<Unit> = _refreshTrigger.asSharedFlow()

    // Action state
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    private val _currentUser = MutableStateFlow<UserProfile?>(null)

    private val _groupMembershipStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())




    // Managers
    val commentManager = CommentManager(
        commentRepository = commentRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val followManager = FollowManager(
        followRepository = followRepository,
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

    val userMediaManager = UserMediaManager(
        userMediaRepository = userMediaRepository,
        viewModelScope = viewModelScope,
        currentUser = _currentUser,
        currentUserId = _currentUserId
    )

    val postManager = PostManager(
        postRepository = postRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val groupManager = GroupManager(
        groupRepository = groupRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    // Expose manager states for easy UI access
    val commentSheetState = commentManager.commentSheetState
    val optionsSheetState = postManager.optionsSheetState
    val comments = commentManager.comments
    val commentsError = commentManager.commentsError
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore


    val groupMembershipStatuses: StateFlow<Map<Int, Boolean>> = _groupMembershipStatuses.asStateFlow()
    val joiningGroupIds: StateFlow<Map<Int, Boolean>> = groupManager.joiningGroupIds


    val followStatuses: StateFlow<Map<Int, Boolean>> = followManager.followStatuses
    val loadingUserIds: StateFlow<Map<Int, Boolean>> = followManager.loadingUserIds

    // Current user
    fun setCurrentUserId(userId: Int?) { _currentUserId.value = userId }
    fun setCurrentUserData(currentUserData: UserProfile?) { _currentUser.value = currentUserData }
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
    // Stories
    val stories: StateFlow<List<StoryFeed>> = storyFeedRepository.observeStories("FEED")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _storiesLoading = MutableStateFlow(false)
    val storiesLoading: StateFlow<Boolean> = _storiesLoading.asStateFlow()

    fun refreshFeed() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
            loadStories()
            loadUserImage()
        }
    }

    fun loadUserImage() = userMediaManager.loadUserImage()

    fun loadStories() {
        viewModelScope.launch {
            _storiesLoading.value = true
            storyFeedRepository.fetchAndCacheStories("FEED")
                .onFailure { error ->
                    _actionState.value = ActionState.Error("Failed to load stories: ${error.message}")
                }
            _storiesLoading.value = false
        }
    }

    // Feed paging
    val feedPagingFlow: Flow<PagingData<UnifiedContentItem>> =
        feedRepository.getPagedFeed(feedType.name)
            .cachedIn(viewModelScope)

    // Scroll to top
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    fun requestScrollToTop() {
        viewModelScope.launch { _scrollToTopEvent.emit(Unit) }
    }

    // Share post
    fun sharePost(shareData: ShareRequestData) = shareManager.sharePost(shareData)

    fun toggleFollow(userId: Int, currentIsFollowing: Boolean, username: String) {
        followManager.toggleFollow(userId, currentIsFollowing, username)
    }

    fun joinGroup(groupId: Int) {
        viewModelScope.launch {
            // Optimistic update
            _groupMembershipStatuses.update { current ->
                current + (groupId to true)
            }
            // Call the actual join operation
            groupManager.joinGroup(groupId)
            // If the join fails, revert the optimistic update.
            // We can listen to actionState, but actionState is global.
            // A more robust way is to modify GroupManager to return a result,
            // but for simplicity we'll revert if an error appears soon.
            // We'll also use a delay to check if error occurred.
            delay(500)
            if (actionState.value is ActionState.Error) {
                _groupMembershipStatuses.update { current ->
                    current - groupId
                }
            }
        }
    }

    // Reactions
    fun sendReaction(data: ReactionCreateRequest) = reactionManager.sendReaction(data)

    // Comments – delegate to manager
    fun openCommentSheet(contentType: String, objectId: Int, stats: PostStatsSerializers?) =
        commentManager
        .openCommentSheet(contentType, objectId, stats)
    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun deleteComment(commentId: Int) = commentManager.deleteComment(commentId)
    fun addReply(parentCommentId: Int?, content: String) = commentManager.addReply(parentCommentId, content)
    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)

    // Posts – delegate to manager
    fun openOptionsSheet(post: PostFeed) = postManager.openOptionsSheet(post)
    fun dismissOptionsSheet() = postManager.dismissOptionsSheet()
    fun deletePost(postId: Int) = postManager.deletePost(postId)
    fun reportPost(postId: Int, reason: String) = postManager.reportPost(postId, reason)

    init {
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
//                        _actionState.value = ActionState.Success("Reacted to ${result.contentType}")
                        when (result.contentType) {
                            "comment" -> {
                                result.reactionType?.let {
                                    commentManager.updateCommentReaction(
                                        commentId = result.objectId,
                                        reacted = result.reacted,
                                        reactionType = safeConvertReactionTypeToRequest(result.reactionType),
                                        reactionCount = result.reactionCount,
                                        counts = result.counts
                                    )
                                }
                            }
                            "post", "reel", "share" -> {}
                        }
                    }
                    is ReactionResult.Error -> {
                        _actionState.value = ActionState.Error(result.message)
                    }
                }
            }
        }
        // Share listener sa FeedViewModel.kt
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

fun safeConvertRequestToResponseReactionType(reactionType: ReactionTypeEnum):
        ReactionTypeEnum {
    return when (reactionType) {
        ReactionTypeEnum.LIKE -> ReactionTypeEnum.LIKE
        ReactionTypeEnum.LOVE -> ReactionTypeEnum.LOVE
        ReactionTypeEnum.CARE -> ReactionTypeEnum.CARE
        ReactionTypeEnum.HAHA -> ReactionTypeEnum.HAHA
        ReactionTypeEnum.WOW -> ReactionTypeEnum.WOW
        ReactionTypeEnum.SAD -> ReactionTypeEnum.SAD
        ReactionTypeEnum.ANGRY -> ReactionTypeEnum.ANGRY
        else -> ReactionTypeEnum.LIKE
    }
}

fun safeConvertReactionTypeToRequest(reactionType: ReactionTypeEnum):
        ReactionTypeEnum {
    return when (reactionType) {
        ReactionTypeEnum.LIKE -> ReactionTypeEnum.LIKE
        ReactionTypeEnum.LOVE -> ReactionTypeEnum.LOVE
        ReactionTypeEnum.CARE -> ReactionTypeEnum.CARE
        ReactionTypeEnum.HAHA -> ReactionTypeEnum.HAHA
        ReactionTypeEnum.WOW -> ReactionTypeEnum.WOW
        ReactionTypeEnum.SAD -> ReactionTypeEnum.SAD
        ReactionTypeEnum.ANGRY -> ReactionTypeEnum.ANGRY
        else -> ReactionTypeEnum.LIKE
    }
}

class FeedViewModelFactory(
    private val feedType: FeedType,
    private val postRepository: UserPostsRepository,
    private val feedRepository: FeedRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val storyFeedRepository: StoriesRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
    private val userMediaRepository: UserMediaRepository,
    private val groupRepository: GroupRepository,
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
                sharePostsRepository,
                followRepository,
                userMediaRepository,
                groupRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

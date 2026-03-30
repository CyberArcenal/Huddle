package com.cyberarcenal.huddle.ui.groups

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
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.*
import com.cyberarcenal.huddle.ui.feed.FeedPagingSource
import com.cyberarcenal.huddle.ui.feed.safeConvertReactionTypeToRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupMainViewModel(
    private val groupRepository: GroupRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
    private  val userMediaRepository: UserMediaRepository,
    private val postRepository: UserPostsRepository
) : ViewModel() {

    // Groups list

    private val _currentUserId = MutableStateFlow<Int?>(null)
    private val _currentUser = MutableStateFlow<UserProfile?>(null)

    private val _groups = MutableStateFlow<List<GroupMinimal>>(emptyList())
    val groups: StateFlow<List<GroupMinimal>> = _groups.asStateFlow()

    private val _isLoadingGroups = MutableStateFlow(true)
    val isLoadingGroups: StateFlow<Boolean> = _isLoadingGroups.asStateFlow()

    fun setCurrentUserId(userId: Int?) { _currentUserId.value = userId }
    fun setCurrentUserData(currentUserData: UserProfile?) { _currentUser.value = currentUserData }

    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Selected group: null = All Groups
    private val _selectedGroupId = MutableStateFlow<Int?>(null)
    val selectedGroupId: StateFlow<Int?> = _selectedGroupId.asStateFlow()

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
    val followManager = FollowManager(
        followRepository = followRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val userMediaManager = UserMediaManager(
        userMediaRepository = userMediaRepository,
        viewModelScope = viewModelScope,
        currentUser = _currentUser,
        currentUserId = _currentUserId
    )

    val shareManager = ShareManager(
        shareRepository = sharePostsRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val postManager = PostManager(
        postRepository = postRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    // Expose manager states
    val commentSheetState = commentManager.commentSheetState
    val optionsSheetState = commentManager.optionsSheetState
    val comments = commentManager.comments
    val commentsError = commentManager.commentsError
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore
    val followStatuses: StateFlow<Map<Int, Boolean>> = followManager.followStatuses
    val loadingUserIds: StateFlow<Map<Int, Boolean>> = followManager.loadingUserIds

    // Feed paging flow based on selected group
    val feedPagingFlow: Flow<PagingData<UnifiedContentItem>> = selectedGroupId.flatMapLatest { groupId ->
        if (groupId == null) {
            // All Groups feed
            Pager(PagingConfig(10)) { GroupFeedPagingSource(groupRepository) }.flow
        } else {
            // Specific group posts
            Pager(PagingConfig(10)) { GroupPostsPagingSource(groupRepository, groupId) }.flow
        }
    }.cachedIn(viewModelScope)

    init {
        loadMyGroups()
        setupReactionListener()
    }

    private fun setupReactionListener() {
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
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
                            else -> {}
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadMyGroups() {
        viewModelScope.launch {
            _isLoadingGroups.value = true
            groupRepository.getMyGroups(page = 1, pageSize = 50).fold(
                onSuccess = { paginated ->
                    _groups.value = paginated.results
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error("Failed to load groups: ${error.message}")
                }
            )
            _isLoadingGroups.value = false
        }
    }

    fun selectGroup(groupId: Int?) {
        _selectedGroupId.value = groupId
    }

    fun refresh() {
        loadMyGroups()
        // The paging flow will automatically refresh when selectedGroupId changes
    }

    fun sendReaction(data: ReactionCreateRequest) = reactionManager.sendReaction(data)

    fun openCommentSheet(contentType: String, objectId: Int) = commentManager.openCommentSheet(contentType, objectId)
    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun deleteComment(commentId: Int) = commentManager.deleteComment(commentId)
    fun addReply(parentCommentId: Int?, content: String) = commentManager.addReply(parentCommentId, content)
    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)

    fun toggleFollow(userId: Int, currentIsFollowing: Boolean, username: String) {
        followManager.toggleFollow(userId, currentIsFollowing, username)
    }

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










    fun openOptionsSheet(post: PostFeed) = postManager.openOptionsSheet(post)
    fun dismissOptionsSheet() = postManager.dismissOptionsSheet()
    fun deletePost(postId: Int) = postManager.deletePost(postId)
    fun reportPost(postId: Int, reason: String) = postManager.reportPost(postId, reason)











    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger: SharedFlow<Unit> = _refreshTrigger.asSharedFlow()








    private val _stories = MutableStateFlow<List<StoryFeed>>(emptyList())
    val stories: StateFlow<List<StoryFeed>> = _stories.asStateFlow()

    private val _storiesLoading = MutableStateFlow(false)
    val storiesLoading: StateFlow<Boolean> = _storiesLoading.asStateFlow()

    fun refreshFeed() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
            loadUserImage()
        }
    }

    fun loadUserImage() = userMediaManager.loadUserImage()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    fun requestScrollToTop() {
        viewModelScope.launch { _scrollToTopEvent.emit(Unit) }
    }



    init {
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        _actionState.value = ActionState.Success("Reacted to ${result.contentType}")
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

// Paging Sources
class GroupFeedPagingSource(
    private val groupRepository: GroupRepository
) : PagingSource<Int, UnifiedContentItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnifiedContentItem> {
        return try {
            val page = params.key ?: 1
            val response = groupRepository.getGroupFeed(page, params.loadSize)
            val feed = response.getOrNull()
            val items = feed?.results ?: emptyList()
            val hasNext = feed?.hasNext ?: false
            LoadResult.Page(
                data = items,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (hasNext) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UnifiedContentItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

class GroupPostsPagingSource(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : PagingSource<Int, UnifiedContentItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnifiedContentItem> {
        return try {
            val page = params.key ?: 1
            val response = groupRepository.getGroupPosts(groupId, page, params.loadSize)
            val feed = response.getOrNull()
            val items = feed?.results ?: emptyList()
            val hasNext = feed?.hasNext ?: false
            LoadResult.Page(
                data = items,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (hasNext) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UnifiedContentItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

class GroupMainViewModelFactory(
    private val groupRepository: GroupRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: UserReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
    private val userMediaRepository: UserMediaRepository,
    private val postRepository: UserPostsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupMainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupMainViewModel(
                groupRepository,
                commentRepository,
                reactionsRepository,
                sharePostsRepository,
                followRepository,
                userMediaRepository,
                postRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
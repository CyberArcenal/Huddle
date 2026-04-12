package com.cyberarcenal.huddle.ui.groups.groupdetail

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
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.CommentManager
import com.cyberarcenal.huddle.ui.common.managers.FollowManager
import com.cyberarcenal.huddle.ui.common.managers.GroupManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionResult
import com.cyberarcenal.huddle.ui.feed.safeConvertReactionTypeToRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: Int,
    private val groupRepository: GroupRepository,
    private val eventRepository: EventRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    // Group data
    private val _groupMembershipStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())

    private val _group = MutableStateFlow<GroupDisplay?>(null)
    val group: StateFlow<GroupDisplay?> = _group.asStateFlow()

    private val _isMember = MutableStateFlow(false)
    val isMember: StateFlow<Boolean> = _isMember.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Managers
    val groupManager = GroupManager(
        groupRepository = groupRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

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

    // Expose manager states
    val commentSheetState = commentManager.commentSheetState
    val optionsSheetState = commentManager.optionsSheetState
    val comments = commentManager.comments
    val commentsError = commentManager.commentsError
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore
    val followStatuses: StateFlow<Map<Int, Boolean>> = followManager.followStatuses
    val groupMembershipStatuses: StateFlow<Map<Int, Boolean>> = _groupMembershipStatuses.asStateFlow()
    val joiningGroupIds: StateFlow<Map<Int, Boolean>> = groupManager.joiningGroupIds


    // Paging flows
    val postsPagingFlow: Flow<PagingData<UnifiedContentItem>> = Pager(PagingConfig(10)) {
        GroupPostsPagingSource(groupRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    val eventsPagingFlow: Flow<PagingData<EventList>> = Pager(PagingConfig(10)) {
        GroupEventsPagingSource(eventRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    val membersPagingFlow: Flow<PagingData<GroupMemberMinimal>> = Pager(PagingConfig(20)) {
        GroupMembersPagingSource(groupRepository, groupId)
    }.flow.cachedIn(viewModelScope)




    init {
        loadGroup()
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
                            "post" -> { /* optionally refresh posts */ }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadGroup() {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.getGroup(groupId).fold(
                onSuccess = { response ->
                    if (response.status){
                        val group = response.data.group
                        _group.value = group
                        _isMember.value = group.isMember ?: false
                    }else{
                        _actionState.value = ActionState.Error(response.message)
                    }

                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error("Failed to load group: ${error.message}")
                }
            )
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadGroup()
        // Paging flows refresh when recomposed
    }

    fun joinGroup() {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Joining group...")
            groupRepository.joinGroup(groupId).fold(
                onSuccess = {
                    _isMember.value = true
                    _actionState.value = ActionState.Success("Joined group!")
                    loadGroup()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to join group")
                }
            )
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Leaving group...")
            groupRepository.leaveGroup(groupId).fold(
                onSuccess = {
                    _isMember.value = false
                    _actionState.value = ActionState.Success("Left group")
                    loadGroup()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to leave group")
                }
            )
        }
    }

    fun inviteFriends() {
        _actionState.value = ActionState.Success("Invite feature coming soon")
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

    fun sendReaction(data: ReactionCreateRequest) = reactionManager.sendReaction(data)

    // Comment actions
    fun openCommentSheet(contentType: String, objectId: Int, stats: PostStatsSerializers?) = commentManager.openCommentSheet(contentType, objectId, stats)
    fun openOptionsSheet(post: PostFeed) = commentManager.openOptionsSheet(post)
    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun dismissOptionsSheet() = commentManager.dismissOptionsSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun deleteComment(commentId: Int) = commentManager.deleteComment(commentId)
    fun addReply(parentCommentId: Int?, content: String) = commentManager.addReply(parentCommentId, content)
    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)

    fun toggleFollow(userId: Int, currentIsFollowing: Boolean, username: String) {
        followManager.toggleFollow(userId, currentIsFollowing, username)
    }

    fun rsvpToEvent(eventId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("RSVPing...")
            // TODO: implement RSVP
            _actionState.value = ActionState.Success("RSVP updated")
        }
    }

    fun changeMemberRole(userId: Int, newRole: RoleBf6Enum) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating role...")
            groupRepository.updateMemberRole(groupId, userId, PatchedGroupMemberUpdateRequest(role = newRole)).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Role updated")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to update role")
                }
            )
        }
    }

    fun removeMember(userId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Removing member...")
            groupRepository.removeMemberById(groupId, userId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Member removed")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to remove member")
                }
            )
        }
    }
}

// Paging sources remain unchanged
class GroupPostsPagingSource(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : PagingSource<Int, UnifiedContentItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnifiedContentItem> {
        return try {
            val page = params.key ?: 1
            val response = groupRepository.getGroupPosts(groupId, page, params.loadSize)
            val feedResponse = response.getOrNull()
            val items = feedResponse?.data?.results ?: emptyList()
            val hasNext = feedResponse?.data?.hasNext ?: false
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

class GroupEventsPagingSource(
    private val eventRepository: EventRepository,
    private val groupId: Int
) : PagingSource<Int, EventList>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventList> {
        return try {
            val page = params.key ?: 1
            val response = eventRepository.getGroupEvents(groupId, page, params.loadSize, upcomingOnly = true)
            val events = response.getOrNull()?.data?.results ?: emptyList()
            val hasNext = response.getOrNull()?.data?.hasNext ?: false
            LoadResult.Page(
                data = events,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (hasNext) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, EventList>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

class GroupMembersPagingSource(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : PagingSource<Int, GroupMemberMinimal>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupMemberMinimal> {
        return try {
            val page = params.key ?: 1
            val response = groupRepository.getMembers(groupId, page, params.loadSize)
            val members = response.getOrNull()?.data?.results ?: emptyList()
            val hasNext = response.getOrNull()?.data?.hasNext ?: false
            LoadResult.Page(
                data = members,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (hasNext) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GroupMemberMinimal>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

// Factory
class GroupDetailViewModelFactory(
    private val groupId: Int,
    private val groupRepository: GroupRepository,
    private val eventRepository: EventRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(
                groupId,
                groupRepository,
                eventRepository,
                commentRepository,
                reactionsRepository,
                sharePostsRepository,
                followRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
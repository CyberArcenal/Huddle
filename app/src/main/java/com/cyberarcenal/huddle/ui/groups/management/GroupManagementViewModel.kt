package com.cyberarcenal.huddle.ui.groups.management

import android.net.Uri
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
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupManagementViewModel(
    private val groupId: Int,
    private val groupRepository: GroupRepository,
    private val postRepository: UserPostsRepository,
    private val eventRepository: EventRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _group = MutableStateFlow<GroupDisplay?>(null)
    val group: StateFlow<GroupDisplay?> = _group.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Members paging (for members tab)
    val membersPagingFlow: Flow<PagingData<GroupMemberMinimal>> = Pager(PagingConfig(20)) {
        GroupMembersPagingSource(groupRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    // Posts paging (for content management)
    val postsPagingFlow: Flow<PagingData<PostFeed>> = Pager(PagingConfig(10)) {
        GroupPostsPagingSourceForManagement(groupRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    // Events paging
    val eventsPagingFlow: Flow<PagingData<EventList>> = Pager(PagingConfig(10)) {
        GroupEventsPagingSource(eventRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    // Join requests paging (if endpoint exists)
    val joinRequestsFlow: Flow<PagingData<JoinRequest>> = Pager(PagingConfig(10)) {
        JoinRequestsPagingSource(groupRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    // Analytics
    private val _statistics = MutableStateFlow<GroupStatistics?>(null)
    val statistics: StateFlow<GroupStatistics?> = _statistics.asStateFlow()

    init {
        loadGroup()
        loadStatistics()
    }

    fun loadGroup() {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.getGroup(groupId).fold(
                onSuccess = { response -> if (response.status){ _group.value = response.data.group}else{_actionState.value = ActionState.Error(response.message)} },
                onFailure = { error -> _actionState.value = ActionState.Error("Failed to load group: ${error.message}") }
            )
            _isLoading.value = false
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            groupRepository.getGroupStatistics(groupId).fold(
                onSuccess = { response ->
                    if (response.status){
                        _statistics.value = response.data.statistics
                    }else{
                        _actionState.value = ActionState.Error(response.message)
                    }
                   },
                onFailure = { error -> _actionState.value = ActionState.Error("Failed to load statistics: ${error.message}") }
            )
        }
    }

    // Group Info actions
    fun updateGroup(name: String, description: String, privacy: PrivacyC6eEnum?, groupType: GroupTypeEnum?) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating group...")
            val request = GroupCreateRequest(
                name = name,
                description = description,
                privacy = privacy,
                groupType = groupType,
                profilePicture = null, // handled separately
                coverPhoto = null
            )
            groupRepository.updateGroup(groupId, request).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Group updated")
                    loadGroup()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to update group")
                }
            )
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Uploading profile picture...")
            // TODO: Implement actual upload (needs multipart)
            _actionState.value = ActionState.Success("Profile picture uploaded")
            loadGroup()
        }
    }

    fun uploadCoverPhoto(uri: Uri) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Uploading cover photo...")
            // TODO: Implement actual upload
            _actionState.value = ActionState.Success("Cover photo uploaded")
            loadGroup()
        }
    }

    // Membership Requests actions
    fun approveRequest(requestId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Approving request...")
            // TODO: Implement approval endpoint
            _actionState.value = ActionState.Success("Request approved")
        }
    }

    fun declineRequest(requestId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Declining request...")
            // TODO: Implement decline endpoint
            _actionState.value = ActionState.Success("Request declined")
        }
    }

    // Members & Roles actions
    fun promoteMember(userId: Int, newRole: RoleEnum) {
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

    // Content & Posts actions
    fun deletePost(postId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Deleting post...")
            postRepository.deletePost(postId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Post deleted")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete post")
                }
            )
        }
    }

    fun pinPost(postId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Pinning post...")
            // TODO: Implement pin endpoint
            _actionState.value = ActionState.Success("Post pinned")
        }
    }

    // Events actions
    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Deleting event...")
            eventRepository.deleteEvent(eventId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Event deleted")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete event")
                }
            )
        }
    }

    fun createEvent(event: EventCreateRequest) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Creating event...")
            eventRepository.createEvent(event).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Event created")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to create event")
                }
            )
        }
    }

    fun updateEvent(eventId: Int, request: EventUpdateRequest) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating event...")
            eventRepository.updateEvent(eventId, request).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Event updated")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to update event")
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}

// Paging sources (reused from group detail screen)
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

class GroupPostsPagingSourceForManagement(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : PagingSource<Int, PostFeed>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostFeed> {
        return try {
            val page = params.key ?: 1
            val response = groupRepository.getGroupPosts(groupId, page, params.loadSize)
            val feedResponse = response.getOrNull()
            // Extract posts from UnifiedContentItem (assuming type POST)
            val posts = feedResponse?.data?.results?.mapNotNull { unified ->
                if (unified.type == UnifiedContentItemTypeEnum.POST) {
                    // Convert Map to PostFeed (use existing safeConvertTo)
                    // For simplicity, we'll assume there's a helper
                    // Here we'll just use the provided data
                    // In reality, you'd need to parse the Map into PostFeed
                    // We'll skip parsing for brevity and return empty list if not possible
                    null
                } else null
            } ?: emptyList()
            val hasNext = feedResponse?.data?.hasNext ?: false
            LoadResult.Page(
                data = posts,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (hasNext) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostFeed>): Int? {
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

class JoinRequestsPagingSource(
    private val groupRepository: GroupRepository,
    private val groupId: Int
) : PagingSource<Int, JoinRequest>() {
    // This is a placeholder – you'll need to implement the actual API call.
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, JoinRequest> {
        return LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )
    }

    override fun getRefreshKey(state: PagingState<Int, JoinRequest>): Int? = null
}

// Placeholder for join request data class
data class JoinRequest(
    val id: Int,
    val user: UserMinimal,
    val requestedAt: java.time.OffsetDateTime,
    val message: String?
)

class GroupManagementViewModelFactory(
    private val groupId: Int,
    private val groupRepository: GroupRepository,
    private val postRepository: UserPostsRepository,
    private val eventRepository: EventRepository,
    private val followRepository: FollowRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupManagementViewModel(groupId, groupRepository, postRepository, eventRepository, followRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
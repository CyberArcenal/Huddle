package com.cyberarcenal.huddle.ui.groups.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.groups.GroupsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class MemberActionState {
    object Idle : MemberActionState()
    object Loading : MemberActionState()
    data class Success(val message: String) : MemberActionState()
    data class Error(val message: String) : MemberActionState()
}

class GroupDetailViewModel(
    private val groupId: Int,
    private val groupsRepository: GroupsRepository
) : ViewModel() {

    private val _groupState = MutableStateFlow<Group?>(null)
    val groupState: StateFlow<Group?> = _groupState.asStateFlow()

    private val _groupLoading = MutableStateFlow(false)
    val groupLoading: StateFlow<Boolean> = _groupLoading.asStateFlow()

    private val _groupError = MutableStateFlow<String?>(null)
    val groupError: StateFlow<String?> = _groupError.asStateFlow()

    private val _memberActionState = MutableStateFlow<MemberActionState>(MemberActionState.Idle)
    val memberActionState: StateFlow<MemberActionState> = _memberActionState.asStateFlow()

    private val _isCurrentUserMember = MutableStateFlow(false)
    val isCurrentUserMember: StateFlow<Boolean> = _isCurrentUserMember.asStateFlow()

    private val _currentUserRole = MutableStateFlow<RoleEnum?>(null)
    val currentUserRole: StateFlow<RoleEnum?> = _currentUserRole.asStateFlow()

    private val _isCreator = MutableStateFlow(false)
    val isCreator: StateFlow<Boolean> = _isCreator.asStateFlow()

    val membersFlow: Flow<PagingData<GroupMember>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { GroupMembersPagingSource(groupId, groupsRepository) }
    ).flow.cachedIn(viewModelScope)

    init {
        loadGroupDetails()
    }

    fun loadGroupDetails() {
        viewModelScope.launch {
            _groupLoading.value = true
            _groupError.value = null
            val result = groupsRepository.getGroup(groupId)
            result.fold(
                onSuccess = { group ->
                    _groupState.value = group
                    _isCurrentUserMember.value = group.isMember == "true"
                    _currentUserRole.value = RoleEnum.decode(group.memberRole)
                    _isCreator.value = group.creatorUsername == null // We'll compare with current user later – need current user ID
                    // For now, we'll trust the `isMember` and `memberRole` flags
                },
                onFailure = { error ->
                    _groupError.value = error.message ?: "Failed to load group"
                }
            )
            _groupLoading.value = false
        }
    }

    fun joinGroup() {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val result = groupsRepository.joinGroup(groupId)
            result.fold(
                onSuccess = { member ->
                    _isCurrentUserMember.value = true
                    _currentUserRole.value = RoleEnum.MEMBER
                    _memberActionState.value = MemberActionState.Success("Joined group successfully")
                    loadGroupDetails() // Refresh group to update member count
                },
                onFailure = { error ->
                    _memberActionState.value = MemberActionState.Error(error.message ?: "Failed to join group")
                }
            )
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val result = groupsRepository.leaveGroup(groupId)
            result.fold(
                onSuccess = {
                    _isCurrentUserMember.value = false
                    _currentUserRole.value = null
                    _memberActionState.value = MemberActionState.Success("Left group successfully")
                    loadGroupDetails()
                },
                onFailure = { error ->
                    _memberActionState.value = MemberActionState.Error(error.message ?: "Failed to leave group")
                }
            )
        }
    }

    fun changeMemberRole(userId: Int, newRole: RoleEnum) {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val update = PatchedGroupMemberUpdate(role = newRole)
            val result = groupsRepository.updateMemberRole(groupId, userId, update)
            result.fold(
                onSuccess = { member ->
                    _memberActionState.value = MemberActionState.Success("Role updated")
                    // Refresh members list by invalidating paging source (will trigger refresh)
                    // We can emit a refresh signal or just reload details
                },
                onFailure = { error ->
                    _memberActionState.value = MemberActionState.Error(error.message ?: "Failed to update role")
                }
            )
        }
    }

    fun removeMember(userId: Int) {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            // Note: The API for removing a member requires groupId only in path and body? In generated GroupsRepository, removeGroupMember takes only groupId.
            // But in the actual API, you need to specify which user to remove. The generated method v1GroupsMembersDestroy takes only groupId – that's likely wrong.
            // Let's assume we have a method that takes both. We'll adjust based on actual implementation.
            // For now, we'll use a placeholder – you'll need to implement the correct call.
            // Since we don't have a proper method, we'll skip and show error.
            _memberActionState.value = MemberActionState.Error("Remove member not implemented yet")
        }
    }

    fun refresh() {
        loadGroupDetails()
    }

    fun clearActionState() {
        _memberActionState.value = MemberActionState.Idle
    }
}

class GroupMembersPagingSource(
    private val groupId: Int,
    private val repository: GroupsRepository
) : androidx.paging.PagingSource<Int, GroupMember>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupMember> {
        return try {
            val page = params.key ?: 1
            val result = repository.getGroupMembers(
                groupId = groupId,
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

    override fun getRefreshKey(state: PagingState<Int, GroupMember>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
// GroupManager.kt
package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupManager(
    private val groupRepository: GroupRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _group = MutableStateFlow<GroupDisplay?>(null)
    val group: StateFlow<GroupDisplay?> = _group.asStateFlow()

    private val _isMember = MutableStateFlow(false)
    val isMember: StateFlow<Boolean> = _isMember.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMemberMinimal>>(emptyList())
    val members: StateFlow<List<GroupMemberMinimal>> = _members.asStateFlow()

    private var _membersPage = 1
    private var _hasMoreMembers = true
    private var currentGroupId: Int? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _joiningGroupIds = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val joiningGroupIds: StateFlow<Map<Int, Boolean>> = _joiningGroupIds.asStateFlow()

    fun setGroup(groupId: Int) {
        if (currentGroupId == groupId) return
        currentGroupId = groupId
        reset()
        loadGroup(groupId)
        loadMembers(groupId)
    }

    private fun reset() {
        _group.value = null
        _isMember.value = false
        _members.value = emptyList()
        _membersPage = 1
        _hasMoreMembers = true
    }

    private fun loadGroup(groupId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.getGroup(groupId).fold(
                onSuccess = { group -> _group.value = group },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load group")
                }
            )
            _isLoading.value = false
        }
    }

    private fun loadMembers(groupId: Int) {
        viewModelScope.launch {
            groupRepository.getMembers(groupId, page = _membersPage).fold(
                onSuccess = { paginated ->
                    _members.value = paginated.results
                    _hasMoreMembers = paginated.hasNext
                    _membersPage++
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load members")
                }
            )
        }
    }

    fun joinGroup() {
        val groupId = currentGroupId ?: return
        joinGroup(groupId)
    }

    fun joinGroup(groupId: Int) {
        viewModelScope.launch {
            _joiningGroupIds.update { it + (groupId to true) }
            actionState.value = ActionState.Loading("Joining group...")
            groupRepository.joinGroup(groupId).fold(
                onSuccess = {
                    if (currentGroupId == groupId) {
                        _isMember.value = true
                    }
                    actionState.value = ActionState.Success("Joined group")
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to join")
                }
            )
            _joiningGroupIds.update { it + (groupId to false) }
        }
    }

    fun leaveGroup() {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Leaving group...")
            groupRepository.leaveGroup(groupId).fold(
                onSuccess = {
                    _isMember.value = false
                    actionState.value = ActionState.Success("Left group")
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to leave")
                }
            )
        }
    }

    fun loadMoreMembers() {
        val groupId = currentGroupId ?: return
        if (!_hasMoreMembers) return
        viewModelScope.launch {
            groupRepository.getMembers(groupId, page = _membersPage).fold(
                onSuccess = { paginated ->
                    _members.update { it + paginated.results }
                    _hasMoreMembers = paginated.hasNext
                    _membersPage++
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load more members")
                }
            )
        }
    }

    fun clear() {
        currentGroupId = null
        reset()
    }
}

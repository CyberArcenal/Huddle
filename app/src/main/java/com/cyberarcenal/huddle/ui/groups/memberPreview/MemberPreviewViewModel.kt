package com.cyberarcenal.huddle.ui.groups.memberPreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.GroupMemberMinimal
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.FollowManager
import kotlinx.coroutines.flow.*

class MemberPreviewViewModel(
    private val groupId: Int,
    private val groupRepository: GroupRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    val followManager = FollowManager(
        followRepository = followRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )
    val followStatuses: StateFlow<Map<Int, Boolean>> = followManager.followStatuses

    val membersPagingFlow: Flow<PagingData<GroupMemberMinimal>> = Pager(PagingConfig(20)) {
        MemberPreviewPagingSource(groupRepository, groupId)
    }.flow.cachedIn(viewModelScope)

    fun toggleFollow(userId: Int, currentIsFollowing: Boolean, username: String) {
        followManager.toggleFollow(userId, currentIsFollowing, username)
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}

class MemberPreviewPagingSource(
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

class MemberPreviewViewModelFactory(
    private val groupId: Int,
    private val groupRepository: GroupRepository,
    private val followRepository: FollowRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemberPreviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemberPreviewViewModel(groupId, groupRepository, followRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
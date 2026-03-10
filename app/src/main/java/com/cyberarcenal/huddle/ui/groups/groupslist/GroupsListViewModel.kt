package com.cyberarcenal.huddle.ui.groups.groupslist

import androidx.paging.PagingState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Group
import com.cyberarcenal.huddle.data.repositories.groups.GroupsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class PrivacyFilter {
    object All : PrivacyFilter()
    data class Specific(val privacy: String) : PrivacyFilter()
}

class GroupsListViewModel(
    private val groupsRepository: GroupsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _privacyFilter = MutableStateFlow<PrivacyFilter>(PrivacyFilter.All)
    val privacyFilter: StateFlow<PrivacyFilter> = _privacyFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val groupsFlow: Flow<PagingData<Group>> = combine(
        _searchQuery.debounce(300),
        _privacyFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            GroupsPagingSource(
                repository = groupsRepository,
                query = query.takeIf { it.length >= 2 },
                privacy = if (filter is PrivacyFilter.Specific) filter.privacy else null
            )
        }.flow
    }.cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        clearError()
    }

    fun setPrivacyFilter(privacy: String?) {
        _privacyFilter.value = if (privacy == null) PrivacyFilter.All else PrivacyFilter.Specific(privacy)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Invalidate paging source by emitting a new query (same value triggers refresh)
            _searchQuery.value = _searchQuery.value
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun navigateToGroupDetail(navController: androidx.navigation.NavController, groupId: Int) {
        navController.navigate("groupdetail/$groupId")
    }
}

class GroupsPagingSource(
    private val repository: GroupsRepository,
    private val query: String?,
    private val privacy: String?
) : androidx.paging.PagingSource<Int, Group>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Group> {
        return try {
            val page = params.key ?: 1
            val result = repository.getGroups(
                page = page,
                pageSize = params.loadSize,
                privacy = privacy,
                query = query
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

    override fun getRefreshKey(state: PagingState<Int, Group>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
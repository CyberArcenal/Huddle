package com.cyberarcenal.huddle.ui.events.eventslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.data.repositories.events.EventsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class EventsFilter {
    object All : EventsFilter()
    data class Type(val type: String) : EventsFilter()
    object Upcoming : EventsFilter()
    object Past : EventsFilter()
}

class EventsListViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<EventsFilter>(EventsFilter.Upcoming)
    val filter: StateFlow<EventsFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val eventsFlow: Flow<PagingData<EventList>> = combine(
        _filter,
        _searchQuery.debounce(300)
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            EventsPagingSource(
                repository = eventsRepository,
                filter = filter,
                searchQuery = query.takeIf { it.length >= 2 }
            )
        }.flow
    }.cachedIn(viewModelScope)

    fun setFilter(filter: EventsFilter) {
        _filter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        clearError()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Trigger paging refresh by emitting same filter
            _filter.value = _filter.value
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun navigateToEventDetail(navController: androidx.navigation.NavController, eventId: Int) {
        navController.navigate("eventdetail/$eventId")
    }
}

class EventsPagingSource(
    private val repository: EventsRepository,
    private val filter: EventsFilter,
    private val searchQuery: String?
) : androidx.paging.PagingSource<Int, EventList>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventList> {
        return try {
            val page = params.key ?: 1
            val result = when (filter) {
                is EventsFilter.All -> repository.getEvents(
                    page = page,
                    pageSize = params.loadSize,
                    upcoming = null,
                    type = null
                )
                is EventsFilter.Type -> repository.getEvents(
                    page = page,
                    pageSize = params.loadSize,
                    type = filter.type
                )
                EventsFilter.Upcoming -> repository.getUpcomingEvents(
                    page = page,
                    pageSize = params.loadSize
                )
                EventsFilter.Past -> repository.getPastEvents(
                    page = page,
                    pageSize = params.loadSize
                )
            }

            if (searchQuery != null) {
                // Search is handled by the API via `getEvents` with query param? Actually the repository method supports it.
                // We'll just call the search method instead.
                val searchResult = repository.searchEvents(
                    q = searchQuery,
                    page = page,
                    pageSize = params.loadSize
                )
                searchResult.fold(
                    onSuccess = { data ->
                        return LoadResult.Page(
                            data = data.results,
                            prevKey = if (page == 1) null else page - 1,
                            nextKey = if (data.next == null) null else page + 1
                        )
                    },
                    onFailure = { error -> return LoadResult.Error(error) }
                )
            } else {
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
            }
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
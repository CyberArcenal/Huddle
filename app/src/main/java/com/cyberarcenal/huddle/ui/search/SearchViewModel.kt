package com.cyberarcenal.huddle.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.SearchResult
import com.cyberarcenal.huddle.data.repositories.search.SearchRepository
import com.cyberarcenal.huddle.data.repositories.users.UsersRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchType = MutableStateFlow(SearchType.ALL)
    val searchType: StateFlow<SearchType> = _searchType.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Paging data for search results
    val searchResultsFlow: Flow<PagingData<SearchResult>> = _searchQuery
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { query ->
            Pager(PagingConfig(pageSize = 20)) {
                SearchPagingSource(usersRepository, query)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        _error.value = null // clear any previous error
        if (query.length >= 2) {
            fetchSuggestions(query)
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun onSearchTypeChange(type: SearchType) {
        _searchType.value = type
        // Refresh search results by updating query (same query triggers refresh)
        _searchQuery.value = _searchQuery.value
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            val result = searchRepository.getSearchSuggestions(query)
            result.fold(
                onSuccess = { response ->
                    _suggestions.value = response.suggestions ?: emptyList()
                },
                onFailure = { error ->
                    Log.e("SearchViewModel", "Failed to fetch suggestions for query: $query", error)
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}

enum class SearchType {
    ALL, USERS, POSTS, GROUPS
}
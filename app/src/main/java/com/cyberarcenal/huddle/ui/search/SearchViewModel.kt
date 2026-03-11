package com.cyberarcenal.huddle.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.data.repositories.search.SearchRepository
import com.cyberarcenal.huddle.data.repositories.users.UsersRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow(SearchCategory.USERS)
    val searchCategory: StateFlow<SearchCategory> = _searchCategory.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsFlow: Flow<PagingData<Any>> = combine(_searchQuery, _searchCategory) { query, category ->
        query to category
    }
    .debounce(400)
    .flatMapLatest { (query, category) ->
        if (query.length < 2) {
            flowOf(PagingData.empty())
        } else {
            Pager(
                config = PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false)
            ) {
                UniversalSearchPagingSource(searchRepository, query, category)
            }.flow
        }
    }
    .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) fetchSuggestions(query) else _suggestions.value = emptyList()
    }

    fun onCategoryChange(category: SearchCategory) {
        _searchCategory.value = category
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            searchRepository.getSuggestions(query).onSuccess { response ->
                _suggestions.value = response.suggestions ?: emptyList()
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _suggestions.value = emptyList()
    }
}

enum class SearchCategory {
    USERS, POSTS, GROUPS, EVENTS
}

class SearchViewModelFactory(
    private val searchRepository: SearchRepository,
    private val usersRepository: UsersRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(searchRepository, usersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

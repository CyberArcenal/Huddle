package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.UserSearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchManager(
    private val repository: UserSearchRepository,
    private val scope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _searchResults = MutableStateFlow<List<UserMinimal>>(emptyList())
    val searchResults: StateFlow<List<UserMinimal>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        scope.launch {
            _isSearching.value = true
            repository.searchUsers(query).fold(
                onSuccess = { _searchResults.value = it.results ?: emptyList() },
                onFailure = { actionState.value = ActionState.Error(it.message ?: "Search failed") }
            )
            _isSearching.value = false
        }
    }
}

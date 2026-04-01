package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.UserMatchingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchingManager(
    private val repository: UserMatchingRepository,
    private val scope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _matches = MutableStateFlow<List<UserMatchScore>>(emptyList())
    val matches: StateFlow<List<UserMatchScore>> = _matches.asStateFlow()

    private val _suggestions = MutableStateFlow<FriendSuggestionsResponseData?>(null)
    val suggestions: StateFlow<FriendSuggestionsResponseData?> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadMatches(limit: Int? = null) {
        scope.launch {
            _isLoading.value = true
            repository.getMatches(limit = limit).fold(
                onSuccess = { if (it.status){ _matches.value = it.data.results}else{actionState.value = ActionState.Error(it.message)} },
                onFailure = { actionState.value = ActionState.Error(it.message ?: "Failed to load matches") }
            )
            _isLoading.value = false
        }
    }

    fun loadSuggestions() {
        scope.launch {
            _isLoading.value = true
            repository.getFriendSuggestions().fold(
                onSuccess = { if (it.status){ _suggestions.value = it.data}else{actionState.value =
                    ActionState.Error(it.message)} },
                onFailure = { actionState.value = ActionState.Error(it.message ?: "Failed to load suggestions") }
            )
            _isLoading.value = false
        }
    }
}

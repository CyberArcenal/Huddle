package com.cyberarcenal.huddle.ui.chat.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.ConversationCreateRequest
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.ConversationRepository
import com.cyberarcenal.huddle.data.repositories.UserSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StartChatUiState {
    object Idle : StartChatUiState()
    object Loading : StartChatUiState()
    data class Success(val users: List<UserMinimal>) : StartChatUiState()
    data class Error(val message: String) : StartChatUiState()
}

sealed class StartChatEvent {
    data class ConversationCreated(val conversationId: Int) : StartChatEvent()
    data class Error(val message: String) : StartChatEvent()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class StartChatViewModel @Inject constructor(
    private val userSearchRepository: UserSearchRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _uiState = MutableStateFlow<StartChatUiState>(StartChatUiState.Idle)
    val uiState: StateFlow<StartChatUiState> = _uiState

    private val _events = MutableSharedFlow<StartChatEvent>()
    val events: SharedFlow<StartChatEvent> = _events

    init {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .onEach { query ->
                searchUsers(query)
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _uiState.value = StartChatUiState.Idle
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.value = StartChatUiState.Loading
            userSearchRepository.searchUsers(query)
                .onSuccess { response ->
                    _uiState.value = StartChatUiState.Success(response.data.results)
                }
                .onFailure { error ->
                    _uiState.value = StartChatUiState.Error(error.message ?: "Failed to search users")
                }
        }
    }

    fun startConversation(userId: Int) {
        viewModelScope.launch {
            val request = ConversationCreateRequest(
                participantIds = listOf(userId)
            )
            conversationRepository.createConversation(request)
                .onSuccess { response ->
                    response.data.conversation.id?.let {
                        _events.emit(StartChatEvent.ConversationCreated(it))
                    } ?: run {
                        _events.emit(StartChatEvent.Error("Invalid response from server"))
                    }
                }
                .onFailure { error ->
                    _events.emit(StartChatEvent.Error(error.message ?: "Failed to create conversation"))
                }
        }
    }
}

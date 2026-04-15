package com.cyberarcenal.huddle.ui.chat.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.Conversation
import com.cyberarcenal.huddle.data.repositories.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val conversationRepository: ConversationRepository = ConversationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationListUiState>(ConversationListUiState.Loading)
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = ConversationListUiState.Loading
            conversationRepository.getConversations().onSuccess { response ->
                _uiState.value = ConversationListUiState.Success(response.data.results)
            }.onFailure { error ->
                _uiState.value = ConversationListUiState.Error(error.message ?: "Unknown error")
            }
        }
    }
}

sealed class ConversationListUiState {
    object Loading : ConversationListUiState()
    data class Success(val conversations: List<Conversation>) : ConversationListUiState()
    data class Error(val message: String) : ConversationListUiState()
}

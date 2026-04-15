package com.cyberarcenal.huddle.ui.chat.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.Message
import com.cyberarcenal.huddle.data.repositories.ChatRepository
import com.cyberarcenal.huddle.data.repositories.ConversationRepository
import com.cyberarcenal.huddle.ui.chat.websocket.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val conversationId: Int = checkNotNull(savedStateHandle["conversationId"])

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    init {
        loadMessages()
        observeWebSocket()
        markAsRead()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getMessages(conversationId).onSuccess { response ->
                _messages.value = response.data.results
            }.onFailure { e ->
                _error.emit(e.message ?: "Failed to load messages")
            }
            _isLoading.value = false
        }
    }

    private fun observeWebSocket() {
        WebSocketManager.connect(conversationId)
        viewModelScope.launch {
            WebSocketManager.events.collect { event ->
                if (event is WebSocketManager.WebSocketEvent.NewMessage) {
                    val newMessage = Message(
                        id = event.messageId,
                        conversation = conversationId,
                        sender = event.senderId,
                        content = event.content ?: "",
                        mediaUrl = event.mediaUrl,
                        mediaType = event.mediaType,
                        createdAt = java.time.OffsetDateTime.parse(event.timestamp)
                    )
                    _messages.update { it + newMessage }
                    markAsRead()
                }
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            conversationRepository.markConversationRead(conversationId)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationPk = conversationId,
                conversation = conversationId,
                content = content
            ).onSuccess {
                // message will come via WebSocket
            }.onFailure { e ->
                _error.emit(e.message ?: "Failed to send message")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        WebSocketManager.disconnect()
    }
}

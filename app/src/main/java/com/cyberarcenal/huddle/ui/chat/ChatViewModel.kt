package com.cyberarcenal.huddle.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Message
import com.cyberarcenal.huddle.data.repositories.messaging.MessagingRepository
import com.cyberarcenal.huddle.ui.chat.websocket.WebSocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val conversationId: Int,
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Messages paging
    val messagesFlow: Flow<PagingData<Message>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        MessagesPagingSource(conversationId, messagingRepository)
    }.flow.cachedIn(viewModelScope)

    // WebSocket events
    private val _wsEvents = MutableSharedFlow<WebSocketEvent>()
    val wsEvents = _wsEvents.asSharedFlow()

    init {
        // Connect to WebSocket when ViewModel is created
        WebSocketManager.connect(conversationId)

        // Collect WebSocket events and forward
        viewModelScope.launch {
            WebSocketManager.events.collect { event ->
                when (event) {
                    is WebSocketManager.WebSocketEvent.NewMessage -> {
                        _wsEvents.emit(WebSocketEvent.MessageReceived)
                    }
                    is WebSocketManager.WebSocketEvent.Error -> {
                        _error.value = "WebSocket error: ${event.message}"
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _sending.value) return

        viewModelScope.launch {
            _sending.value = true
            _error.value = null
            val result = messagingRepository.sendTextMessage(conversationId, text)
            result.fold(
                onSuccess = {
                    _inputText.value = ""
                },
                onFailure = { error ->
                    _error.value = error.message ?: "Failed to send message"
                }
            )
            _sending.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        WebSocketManager.disconnect()
        super.onCleared()
    }

    sealed class WebSocketEvent {
        object MessageReceived : WebSocketEvent()
    }
}

// Factory class para sa manual initialization ng ViewModel
class ChatViewModelFactory(
    private val conversationId: Int,
    private val messagingRepository: MessagingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(conversationId, messagingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MessagesPagingSource(
    private val conversationId: Int,
    private val repository: MessagingRepository
) : androidx.paging.PagingSource<Int, Message>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
        return try {
            val page = params.key ?: 1
            val result = repository.getMessages(conversationId, page, params.loadSize)
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

    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Message>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

// DatingViewModel.kt
package com.cyberarcenal.huddle.ui.dating

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.DatingMessagesRepository
import com.cyberarcenal.huddle.data.repositories.DatingPreferencesRepository
import com.cyberarcenal.huddle.data.repositories.MatchesRepository
import com.cyberarcenal.huddle.data.repositories.UserMatchingRepository
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONObject
import java.time.OffsetDateTime

sealed class DatingUiState {
    object Loading : DatingUiState()
    data class Success(val data: Any) : DatingUiState()
    data class Error(val message: String) : DatingUiState()
}

data class ConversationPartner(
    val userId: Int,
    val username: String,
    val profilePictureUrl: String?,
    val lastMessage: String,
    val lastMessageTime: String?,
    val unreadCount: Int
)

class DatingViewModel(
    private val preferencesRepo: DatingPreferencesRepository,
    private val messagesRepo: DatingMessagesRepository,
    private val matchingRepo: UserMatchingRepository,
    private val matchesRepo: MatchesRepository
) : ViewModel() {

    // Discover users
    private val _discoverState = MutableStateFlow<DatingUiState>(DatingUiState.Loading)
    val discoverState: StateFlow<DatingUiState> = _discoverState.asStateFlow()

    // Preferences
    private val _preferencesState = MutableStateFlow<DatingUiState>(DatingUiState.Loading)
    val preferencesState: StateFlow<DatingUiState> = _preferencesState.asStateFlow()

    // Inbox (grouped conversations)
    private val _inboxState = MutableStateFlow<DatingUiState>(DatingUiState.Loading)
    val inboxState: StateFlow<DatingUiState> = _inboxState.asStateFlow()

    // Sent messages
    private val _sentState = MutableStateFlow<DatingUiState>(DatingUiState.Loading)
    val sentState: StateFlow<DatingUiState> = _sentState.asStateFlow()

    // Conversation with specific user
    private val _conversationState = MutableStateFlow<DatingUiState>(DatingUiState.Loading)
    val conversationState: StateFlow<DatingUiState> = _conversationState.asStateFlow()

    private val _sendMessageState = MutableStateFlow<DatingUiState?>(null)
    val sendMessageState: StateFlow<DatingUiState?> = _sendMessageState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    // WebSocket state
    private var webSocket: WebSocket? = null
    private var currentReceiverId: Int? = null
    private val client = OkHttpClient()
    private val _messages = MutableStateFlow<List<DatingMessageDetail>>(emptyList())
    val messages: StateFlow<List<DatingMessageDetail>> = _messages.asStateFlow()

    init {
        loadPreferences()
        loadInbox()
        loadSentMessages()
        loadDiscoverUsers()
    }

    fun connectToChat(receiverId: Int, retryCount: Int = 0) {
        if (retryCount > 5) {
            Log.e("DatingViewModel", "Max WebSocket reconnection attempts reached")
            return
        }
        disconnectFromChat()
        currentReceiverId = receiverId
        val token = TokenManager.accessToken ?: run {
            Log.e("DatingViewModel", "No access token for WebSocket")
            return
        }
        // TODO: Move WebSocket URL to config
        val url = "ws://127.0.0.1:8000/ws/dating/chat/$receiverId/?token=$token"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "new_message" -> {
                            val newMessage = DatingMessageDetail(
                                id = json.getInt("message_id"),
                                sender = UserMinimal(id = json.getInt("sender_id"), username = json.getString("sender_username")),
                                receiver = UserMinimal(id = json.getInt("receiver_id")),
                                content = json.getString("content"),
                                createdAt = OffsetDateTime.parse(json.getString("timestamp")),
                                isRead = json.getBoolean("is_read")
                            )
                            viewModelScope.launch {
                                _messages.update { current ->
                                    if (current.any { it.id == newMessage.id }) current else listOf(newMessage) + current
                                }
                                // Also update conversation state
                                _conversationState.update { state ->
                                    if (state is DatingUiState.Success) {
                                        @Suppress("UNCHECKED_CAST")
                                        val list = state.data as List<DatingMessageDetail>
                                        if (list.any { it.id == newMessage.id }) state else DatingUiState.Success(listOf(newMessage) + list)
                                    } else state
                                }
                                // Refresh inbox because unread count may change
                                loadInbox()
                            }
                        }
                        "message_read" -> {
                            val messageId = json.getInt("message_id")
                            viewModelScope.launch {
                                _messages.update { messages ->
                                    messages.map { msg ->
                                        if (msg.id == messageId) msg.copy(isRead = true) else msg
                                    }
                                }
                                loadInbox()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DatingViewModel", "Error parsing socket message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("DatingViewModel", "WebSocket failure", t)
                viewModelScope.launch {
                    delay(2000L * (retryCount + 1))
                    connectToChat(receiverId, retryCount + 1)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("DatingViewModel", "WebSocket closed: $reason")
            }
        })
    }

    fun disconnectFromChat() {
        webSocket?.close(1000, "Closing connection")
        webSocket = null
        currentReceiverId = null
    }

    fun sendSocketMessage(content: String) {
        if (webSocket == null) {
            Log.e("DatingViewModel", "WebSocket not connected, falling back to REST")
            currentReceiverId?.let { receiverId ->
                sendMessage(receiverId, content) {}
            }
            return
        }
        val data = JSONObject().apply {
            put("content", content)
        }
        webSocket?.send(data.toString())
    }

    fun loadDiscoverUsers() {
        viewModelScope.launch {
            _discoverState.value = DatingUiState.Loading
            matchingRepo.getMatches(limit = 20).fold(
                onSuccess = { response ->
                    val users = (response.data.results ?: emptyList<UserMatchScore>()).map { userScore ->
                        userScore.copy(score = userScore.score ?: 0)
                    }
                    _discoverState.value = DatingUiState.Success(users)
                },
                onFailure = { error ->
                    _discoverState.value = DatingUiState.Error(error.message ?: "Failed to load discoveries")
                }
            )
        }
    }

    fun likeUser(userId: Int, onMatch: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val request = MatchCreateRequest(user2 = userId)
            matchesRepo.createMatch(request).fold(
                onSuccess = { response ->
                    onMatch(response.status)
                    // Remove current user and move to next
                    popDiscoverUser()
                },
                onFailure = { error ->
                    _discoverState.value = DatingUiState.Error(error.message ?: "Failed to like user")
                }
            )
        }
    }

    fun skipUser() {
        // TODO: Call API to record skip if needed
        popDiscoverUser()
    }

    private fun popDiscoverUser() {
        val state = _discoverState.value
        if (state is DatingUiState.Success) {
            val users = (state.data as List<UserMatchScore>).toMutableList()
            if (users.isNotEmpty()) {
                users.removeAt(0)
                _discoverState.value = DatingUiState.Success(users)
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _preferencesState.value = DatingUiState.Loading
            preferencesRepo.getPreferences().fold(
                onSuccess = { response ->
                    _preferencesState.value = DatingUiState.Success(response.data.preferences)
                },
                onFailure = { error ->
                    _preferencesState.value = DatingUiState.Error(error.message ?: "Failed to load preferences")
                }
            )
        }
    }

    fun setCurrentUserId(id: Int?) {
        _currentUserId.value = id
    }

    fun updatePreferences(request: DatingPreferenceCreateUpdateRequest) {
        viewModelScope.launch {
            _preferencesState.value = DatingUiState.Loading
            preferencesRepo.updatePreferences(request).fold(
                onSuccess = { response ->
                    _preferencesState.value = DatingUiState.Success(response.data.preferences)
                },
                onFailure = { error ->
                    _preferencesState.value = DatingUiState.Error(error.message ?: "Failed to update preferences")
                }
            )
        }
    }

    fun loadInbox() {
        viewModelScope.launch {
            _inboxState.value = DatingUiState.Loading
            messagesRepo.getInbox(limit = 100).fold(
                onSuccess = { response ->
                    val messages = response.data.results ?: emptyList()
                    val conversations = groupMessagesIntoConversations(messages)
                    _inboxState.value = DatingUiState.Success(conversations)
                },
                onFailure = { error ->
                    _inboxState.value = DatingUiState.Error(error.message ?: "Failed to load inbox")
                }
            )
        }
    }

    fun loadSentMessages() {
        viewModelScope.launch {
            _sentState.value = DatingUiState.Loading
            messagesRepo.getSentMessages(limit = 100).fold(
                onSuccess = { response ->
                    _sentState.value = DatingUiState.Success(response.data.results ?: emptyList<DatingMessageDetail>())
                },
                onFailure = { error ->
                    _sentState.value = DatingUiState.Error(error.message ?: "Failed to load sent messages")
                }
            )
        }
    }

    fun loadConversationWith(userId: Int) {
        viewModelScope.launch {
            _conversationState.value = DatingUiState.Loading
            messagesRepo.getConversation(userId, limit = 100).fold(
                onSuccess = { response ->
                    val messages = response.data.results ?: emptyList()
                    _messages.value = messages
                    _conversationState.value = DatingUiState.Success(messages)
                    // Mark only incoming unread messages as read, but avoid spamming API
                    val unreadMessages = messages.filter {
                        it.isRead == false && it.receiver?.id == currentUserId.value
                    }
                    if (unreadMessages.isNotEmpty()) {
                        // Mark first unread message as read; backend might mark conversation as read
                        markMessageRead(unreadMessages.first().id!!)
                    }
                },
                onFailure = { error ->
                    _conversationState.value = DatingUiState.Error(error.message ?: "Failed to load conversation")
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectFromChat()
    }

    fun sendMessage(receiverId: Int, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _sendMessageState.value = DatingUiState.Loading
            val request = DatingMessageCreateRequest(receiver = receiverId, content = content)
            messagesRepo.sendMessage(request).fold(
                onSuccess = { response ->
                    _sendMessageState.value = DatingUiState.Success(response.data.message)
                    onSuccess()
                    // Refresh conversation after sending
                    loadConversationWith(receiverId)
                    loadInbox()
                },
                onFailure = { error ->
                    _sendMessageState.value = DatingUiState.Error(error.message ?: "Failed to send message")
                }
            )
        }
    }

    fun markMessageRead(messageId: Int) {
        viewModelScope.launch {
            messagesRepo.markMessageRead(messageId).fold(
                onSuccess = { /* ignore */ },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun clearSendMessageState() {
        _sendMessageState.value = null
    }

    // Helper: group messages by the other participant
    private fun groupMessagesIntoConversations(messages: List<DatingMessageDetail>): List<ConversationPartner> {
        val currentUserId = currentUserId.value
        val conversationMap = mutableMapOf<Int, MutableList<DatingMessageDetail>>()
        for (msg in messages) {
            val otherId = when {
                msg.sender?.id == currentUserId -> msg.receiver?.id
                msg.receiver?.id == currentUserId -> msg.sender?.id
                else -> null
            } ?: continue
            conversationMap.getOrPut(otherId) { mutableListOf() }.add(msg)
        }
        return conversationMap.map { (userId, userMessages) ->
            val latest = userMessages.maxByOrNull { it.createdAt ?: OffsetDateTime.MIN }
            val otherUser = if (latest?.sender?.id == currentUserId) latest?.receiver else latest?.sender
            ConversationPartner(
                userId = userId,
                username = otherUser?.username ?: "User $userId",
                profilePictureUrl = otherUser?.profilePictureUrl,
                lastMessage = latest?.content ?: "",
                lastMessageTime = latest?.createdAt?.toString(),
                unreadCount = userMessages.count { it.isRead == false && it.receiver?.id == currentUserId }
            )
        }.sortedByDescending { it.lastMessageTime }
    }
}
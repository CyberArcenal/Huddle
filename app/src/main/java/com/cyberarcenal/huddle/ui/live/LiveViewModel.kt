package com.cyberarcenal.huddle.ui.live

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.LiveRepository
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class LiveUiState {
    object Idle : LiveUiState()
    object Loading : LiveUiState()
    data class Success(val data: Any) : LiveUiState()
    data class Error(val message: String) : LiveUiState()
}

enum class JoinRequestStatus {
    NONE,       // wala pang request
    PENDING,    // naghihintay ng approval
    APPROVED,   // approved na (makaka-join na)
    REJECTED    // ni-reject
}


data class LiveComment(
    val id: Int, val userId: Int, val username: String, val content: String, val createdAt: String
)

class LiveViewModel(private val repository: LiveRepository) : ViewModel() {

    private val _activeStreams = MutableStateFlow<LiveUiState>(LiveUiState.Idle)
    val activeStreams: StateFlow<LiveUiState> = _activeStreams.asStateFlow()

    private val _currentLiveStream = MutableStateFlow<LiveStream?>(null)
    val currentLiveStream: StateFlow<LiveStream?> = _currentLiveStream.asStateFlow()

    private val _comments = MutableStateFlow<List<LiveComment>>(emptyList())
    val comments: StateFlow<List<LiveComment>> = _comments.asStateFlow()

    private val _participants = MutableStateFlow<List<LiveParticipant>>(emptyList())
    val participants: StateFlow<List<LiveParticipant>> = _participants.asStateFlow()

    private val _joinRequests = MutableStateFlow<List<LiveJoinRequest>>(emptyList())
    val joinRequests: StateFlow<List<LiveJoinRequest>> = _joinRequests.asStateFlow()

    private val _liveKitToken = MutableStateFlow<TokenData?>(null)
    val liveKitToken: StateFlow<TokenData?> = _liveKitToken.asStateFlow()

    private var webSocketManager: LiveWebSocketManager? = null

    private val _joinRequestStatus = MutableStateFlow(JoinRequestStatus.NONE)
    val joinRequestStatus: StateFlow<JoinRequestStatus> = _joinRequestStatus.asStateFlow()

    fun loadActiveStreams() {
        viewModelScope.launch {
            _activeStreams.value = LiveUiState.Loading
            repository.getActiveStreams().fold(
                onSuccess = { streams -> _activeStreams.value = LiveUiState.Success(streams) },
                onFailure = { error ->
                    _activeStreams.value =
                        LiveUiState.Error(error.message ?: "Failed to load streams")
                })
        }
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun initUser(context: android.content.Context) {
        viewModelScope.launch {
            _currentUser.value = TokenManager.getUser(context)
        }
    }

    fun startLive(title: String, description: String) {
        viewModelScope.launch {
            val request = LiveCreateRequest(title = title, description = description)
            repository.startLive(request).fold(onSuccess = { response ->
                if (response.status) {
                    val stream = response.data.liveData
                    if (stream.id !== null) {
                        _currentLiveStream.value = stream
                        connectToWebSocket(stream.id)
                        fetchLiveKitToken(stream.id)
                    }
                }


            }, onFailure = { error ->
                Log.e("LiveViewModel", "Failed to start live", error)
            })
        }
    }

    // Baguhin ang requestToJoin()


    fun joinStream(liveId: Int) {
        viewModelScope.launch {
            repository.getLiveDetails(liveId).fold(onSuccess = { response ->
                if (response.status) {
                    val stream = response.data
                    if (stream.id !== null) {
                        _currentLiveStream.value = stream
                        connectToWebSocket(stream.id)
                        // If host, fetch token immediately. If viewer, might need to request join first.
                        // For now, let's assume we fetch token if allowed or after approval.
                        fetchLiveKitToken(stream.id)
                        fetchParticipants(stream.id)
                    }
                }


            }, onFailure = { error ->
                Log.e("LiveViewModel", "Failed to join stream", error)
            })
        }
    }

    private fun connectToWebSocket(liveId: Int) {
        val token = TokenManager.accessToken ?: return
        webSocketManager?.disconnect()
        webSocketManager = LiveWebSocketManager(liveId, token).apply {
            addListener(object : LiveWebSocketManager.LiveStreamListener {
                override fun onNewRequest(request: LiveJoinRequest) {
                    _joinRequests.update { it + request }
                }

                override fun onRequestResponded(requestId: Int, status: String) {
                    // Hanapin kung ang request na ito ay galing sa current user
                    val currentUserId = _currentUser.value?.id
                    val request = _joinRequests.value.find { it.id == requestId }
                    if (request?.user?.id == currentUserId) {
                        when (status.lowercase()) {
                            "approved" -> _joinRequestStatus.value = JoinRequestStatus.APPROVED
                            "rejected" -> _joinRequestStatus.value = JoinRequestStatus.REJECTED
                        }
                    }
                    // I-update pa rin ang listahan ng requests para sa host
                    _joinRequests.update { it.filter { req -> req.id != requestId } }
                    if (status == "approved" && request?.user?.id == currentUserId) {
                        fetchLiveKitToken(liveId)
                    }
                }


                override fun onParticipantUpdate(action: String, userId: Int, username: String) {
                    fetchParticipants(liveId)
                }

                override fun onNewComment(
                    commentId: Int,
                    userId: Int,
                    username: String,
                    content: String,
                    createdAt: String
                ) {
                    val newComment = LiveComment(commentId, userId, username, content, createdAt)
                    _comments.update { (it + newComment).takeLast(100) }
                }

                override fun onStreamEnded() {
                    _currentLiveStream.value = null
                    webSocketManager?.disconnect()
                }
            })
            connect()
        }
    }

    fun sendComment(content: String) {
        webSocketManager?.sendComment(content)
    }

    private fun fetchLiveKitToken(liveId: Int) {
        viewModelScope.launch {
            repository.getLiveKitToken(liveId).fold(onSuccess = { response ->
                _liveKitToken.value = response.data?.let {
                    TokenData(token = it.token, url = it.url, roomName = it.roomName)
                }
            }, onFailure = { error ->
                Log.e("LiveViewModel", "Failed to get LiveKit token", error)
            })
        }
    }

    private fun fetchParticipants(liveId: Int) {
        viewModelScope.launch {
            repository.getParticipants(liveId).fold(onSuccess = { response ->
                if (response.status) {
                    _participants.value = response.data
                }
            }, onFailure = { /* ignore */ })
        }
    }

    fun endLive() {
        val streamId = _currentLiveStream.value?.id ?: return
        viewModelScope.launch {
            repository.endLive(streamId).fold(onSuccess = {
                _currentLiveStream.value = null
                webSocketManager?.disconnect()
            }, onFailure = { /* ignore */ })
        }
    }

    fun leaveStream() {
        val streamId = _currentLiveStream.value?.id ?: return
        viewModelScope.launch {
            repository.leaveStream(streamId).fold(onSuccess = {
                _currentLiveStream.value = null
                webSocketManager?.disconnect()
                resetJoinRequestStatus()
            }, onFailure = { /* ignore */ })
        }
    }

    fun requestToJoin(message: String) {
        val streamId = _currentLiveStream.value?.id ?: return
        if (_joinRequestStatus.value != JoinRequestStatus.NONE) return // bawal kung may pending/approved na
        viewModelScope.launch {
            repository.requestJoin(streamId, message).onFailure { error ->
                Log.e("LiveViewModel", "Failed to request join", error)
            }
        }
    }

    // Idagdag ang function para ma-reset ang status (halimbawa kapag umalis sa stream)
    fun resetJoinRequestStatus() {
        _joinRequestStatus.value = JoinRequestStatus.NONE
    }

    fun respondToJoinRequest(requestId: Int, approve: Boolean) {
        viewModelScope.launch {
            repository.respondToRequest(requestId, approve).onSuccess {
                _joinRequestStatus.value = JoinRequestStatus.PENDING
                _joinRequests.update { list -> list.filter { it.id != requestId } }
            }.onFailure { error ->
                Log.e("LiveViewModel", "Failed to respond to request", error)
            }
        }
    }

    fun fetchPendingRequests() {
        val streamId = _currentLiveStream.value?.id ?: return
        viewModelScope.launch {
            repository.getPendingRequests(streamId).onSuccess { response ->
                if (response.status) {
                    _joinRequests.value = response.data.results
                }
            }.onFailure { error ->
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        webSocketManager?.disconnect()
    }
}

// Minimal data class for LiveKit token if not already in models
data class TokenData(val token: String, val url: String, val roomName: String)

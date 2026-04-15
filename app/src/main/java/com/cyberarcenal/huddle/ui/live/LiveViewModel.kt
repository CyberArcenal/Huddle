package com.cyberarcenal.huddle.ui.live

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.data.repositories.LiveRepository
import com.cyberarcenal.huddle.data.repositories.ReactionsRepository
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.common.managers.*
import kotlinx.coroutines.flow.*
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

class LiveViewModel(
    private val repository: LiveRepository,
    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Managers
    val commentManager = CommentManager(
        commentRepository = commentRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val reactionManager = ReactionManager(
        reactionRepository = reactionsRepository,
        viewModelScope = viewModelScope
    )

    // Expose manager states
    val commentSheetState = commentManager.commentSheetState
    val commentComments = commentManager.comments
    val commentsError = commentManager.commentsError
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore

    private val _activeStreams = MutableStateFlow<LiveUiState>(LiveUiState.Idle)
    val activeStreams: StateFlow<LiveUiState> = _activeStreams.asStateFlow()

    private val _currentLiveStream = MutableStateFlow<LiveStream?>(null)
    val currentLiveStream: StateFlow<LiveStream?> = _currentLiveStream.asStateFlow()

    private val _currentReaction = MutableStateFlow<ReactionTypeEnum?>(null)
    val currentReaction: StateFlow<ReactionTypeEnum?> = _currentReaction.asStateFlow()

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

    private val _isStartingLive = MutableStateFlow(false)
    val isStartingLive: StateFlow<Boolean> = _isStartingLive.asStateFlow()

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
            _isStartingLive.value = true
            val request = LiveCreateRequest(title = title, description = description)
            repository.startLive(request).fold(onSuccess = { response ->
                _isStartingLive.value = false
                if (response.status) {
                    val stream = response.data.liveData
                    if (stream.id !== null) {
                        _currentLiveStream.value = stream
                        connectToWebSocket(stream.id)
                        fetchLiveKitToken(stream.id)
                    }
                }


            }, onFailure = { error ->
                _isStartingLive.value = false
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
                _liveKitToken.value = response.data?.let { data ->
                    // Fix URL if it points to localhost/127.0.0.1 on a real device
                    var url = data.url
                    val isLocalhost = url.contains("localhost") || url.contains("127.0.0.1")
                    if (isLocalhost && !isEmulator()) {
                        // Extract host from BASE_URL
                        val apiHost = com.cyberarcenal.huddle.network.ApiService.BASE_URL
                            .removePrefix("http://")
                            .removePrefix("https://")
                            .split(":")
                            .first()
                            .removeSuffix("/")
                        
                        url = url.replace("localhost", apiHost).replace("127.0.0.1", apiHost)
                    }

                    TokenData(token = data.token, url = url, roomName = data.roomName)
                }
            }, onFailure = { error ->
                Log.e("LiveViewModel", "Failed to get LiveKit token", error)
            })
        }
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
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

    // Reaction and Comment delegates
    fun sendReaction(data: ReactionCreateRequest) {
        if (data.contentType == "livestream") {
            _currentReaction.value = data.reactionType
        }
        reactionManager.sendReaction(data)
    }

    fun openCommentSheet(contentType: String, objectId: Int, stats: PostStatsSerializers?) =
        commentManager.openCommentSheet(contentType, objectId, stats)

    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun deleteComment(commentId: Int) = commentManager.deleteComment(commentId)
    fun addReply(parentCommentId: Int?, content: String) =
        commentManager.addReply(parentCommentId, content)

    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)

    init {
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        if (result.contentType == "comment") {
                            result.reactionType?.let {
                                commentManager.updateCommentReaction(
                                    commentId = result.objectId,
                                    reacted = result.reacted,
                                    reactionType = result.reactionType,
                                    reactionCount = result.reactionCount,
                                    counts = result.counts
                                )
                            }
                        }
                    }
                    is ReactionResult.Error -> {
                        _actionState.value = ActionState.Error(result.message)
                    }
                }
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

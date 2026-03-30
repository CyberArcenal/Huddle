package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Manager class to handle friendship actions like sending, accepting, 
 * or declining requests and removing friends.
 */
class FriendshipManager(
    private val repository: FriendshipsRepository,
    private val scope: CoroutineScope
) {
    private val _events = MutableSharedFlow<FriendshipResult>()
    val events: SharedFlow<FriendshipResult> = _events.asSharedFlow()

    /**
     * Send a friend request to a user.
     */
    fun sendRequest(toUserId: Int) {
        scope.launch {
            repository.sendRequest(FriendshipCreateRequest(toUser = toUserId)).fold(
                onSuccess = { 
                    _events.emit(FriendshipResult.Success(FriendshipAction.SEND, it)) 
                },
                onFailure = { 
                    _events.emit(FriendshipResult.Error(FriendshipAction.SEND, it.message ?: "Failed to send request")) 
                }
            )
        }
    }

    /**
     * Accept a pending friend request.
     */
    fun acceptRequest(requestId: Int) {
        scope.launch {
            repository.acceptRequest(requestId).fold(
                onSuccess = { 
                    _events.emit(FriendshipResult.Success(FriendshipAction.ACCEPT, it)) 
                },
                onFailure = { 
                    _events.emit(FriendshipResult.Error(FriendshipAction.ACCEPT, it.message ?: "Failed to accept request")) 
                }
            )
        }
    }

    /**
     * Decline a pending friend request.
     */
    fun declineRequest(requestId: Int) {
        scope.launch {
            repository.declineRequest(requestId).fold(
                onSuccess = { 
                    _events.emit(FriendshipResult.Success(FriendshipAction.DECLINE, it)) 
                },
                onFailure = { 
                    _events.emit(FriendshipResult.Error(FriendshipAction.DECLINE, it.message ?: "Failed to decline request")) 
                }
            )
        }
    }

    /**
     * Remove an existing friend.
     */
    fun removeFriend(friendId: Int) {
        scope.launch {
            repository.removeFriend(FriendRemoveRequest(friendId)).fold(
                onSuccess = { 
                    _events.emit(FriendshipResult.Success(FriendshipAction.REMOVE, it)) 
                },
                onFailure = { 
                    _events.emit(FriendshipResult.Error(FriendshipAction.REMOVE, it.message ?: "Failed to remove friend")) 
                }
            )
        }
    }
}

/**
 * Action types for friendship operations.
 */
enum class FriendshipAction {
    SEND, ACCEPT, DECLINE, REMOVE
}

/**
 * Result wrapper for friendship events.
 */
sealed class FriendshipResult {
    data class Success(val action: FriendshipAction, val data: Any) : FriendshipResult()
    data class Error(val action: FriendshipAction, val message: String) : FriendshipResult()
}

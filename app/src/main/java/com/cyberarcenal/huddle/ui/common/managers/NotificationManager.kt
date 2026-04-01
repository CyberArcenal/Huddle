// NotificationManager.kt
package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.Notification
import com.cyberarcenal.huddle.api.models.NotificationMarkReadRequest
import com.cyberarcenal.huddle.data.repositories.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationManager(
    private val notificationsRepository: NotificationsRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var _currentPage = 1
    private var _hasMore = true
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadNotifications(refresh: Boolean = false) {
        if (_isLoading.value) return
        if (refresh) {
            _currentPage = 1
            _hasMore = true
            _notifications.value = emptyList()
        }
        viewModelScope.launch {
            _isLoading.value = true
            notificationsRepository.getNotifications(page = _currentPage).fold(
                onSuccess = { paginated ->
                    if (refresh) {
                        _notifications.value = paginated.data.results
                    } else {
                        _notifications.update { it + paginated.data.results }
                    }
                    _hasMore = paginated.data.hasNext
                    _currentPage++
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load notifications")
                }
            )
            _isLoading.value = false
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            notificationsRepository.getUnreadCount().fold(
                onSuccess = { response ->
                    if (response.status){_unreadCount.value = response.data.unreadCount!!}else{actionState.value =
                        ActionState.Error(response.message)}

                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun markAsRead(id: Int? = null) {
        viewModelScope.launch {
            val request = NotificationMarkReadRequest(id, markAll = false)
            notificationsRepository.markRead(request).fold(
                onSuccess = {
                    _notifications.update { list ->
                        list.map {
                            if (it.id == id) it.copy(isRead = true) else it
                        }
                    }
                    _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error("Failed to mark as read")
                }
            )
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            notificationsRepository.markAllRead().fold(
                onSuccess = {
                    _notifications.update { list ->
                        list.map { it.copy(isRead = true) }
                    }
                    _unreadCount.value = 0
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error("Failed to mark all as read")
                }
            )
        }
    }

    fun loadMore() {
        if (_hasMore && !_isLoading.value) {
            loadNotifications(refresh = false)
        }
    }

    fun clear() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
        _currentPage = 1
        _hasMore = true
        _isLoading.value = false
    }
}
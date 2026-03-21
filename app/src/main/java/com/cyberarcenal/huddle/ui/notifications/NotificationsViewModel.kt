package com.cyberarcenal.huddle.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Notification
import com.cyberarcenal.huddle.api.models.NotificationMarkReadRequest
import com.cyberarcenal.huddle.data.repositories.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _markAllResult = MutableStateFlow<MarkAllResult?>(null)
    val markAllResult: StateFlow<MarkAllResult?> = _markAllResult.asStateFlow()

    val notificationsFlow: Flow<PagingData<Notification>> = Pager(
        PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        NotificationsPagingSource(repository)
    }.flow.cachedIn(viewModelScope)

    init {
        loadUnreadCount()
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount()
                .onSuccess { response ->
                    _unreadCount.value = response.unreadCount ?: 0
                }
                .onFailure {
                    // Handle error silently or show message
                }
        }
    }

    fun markNotificationRead(notificationId: Int?) {
        if (notificationId===null)return;
        viewModelScope.launch {
            val request = NotificationMarkReadRequest(id=notificationId)
            repository.markRead(request)
                .onSuccess {
                    // Update local state: we could refresh the list, but for simplicity, just reload unread count
                    loadUnreadCount()
                }
                .onFailure { error ->
                    // Could expose error state if needed
                }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            _markAllResult.value = MarkAllResult.Loading
            repository.markAllRead()
                .onSuccess {
                    _markAllResult.value = MarkAllResult.Success
                    loadUnreadCount()
                }
                .onFailure { error ->
                    _markAllResult.value = MarkAllResult.Error(error.message ?: "Failed to mark all as read")
                }
        }
    }

    fun clearMarkAllResult() {
        _markAllResult.value = null
    }
}

sealed class MarkAllResult {
    object Loading : MarkAllResult()
    object Success : MarkAllResult()
    data class Error(val message: String) : MarkAllResult()
}
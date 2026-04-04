package com.cyberarcenal.huddle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.common.managers.OnlineStatusManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    init {
        // Start tracking online status when the home view is active
        OnlineStatusManager.connect()
    }

    override fun onCleared() {
        super.onCleared()
        // Stop tracking online status when the ViewModel is cleared
        OnlineStatusManager.disconnect()
    }

    // This ViewModel can hold global home-related state, e.g., unread notifications count.
    private val _feedRefreshRequest = MutableSharedFlow<Unit>()
    val feedRefreshRequest: SharedFlow<Unit> = _feedRefreshRequest.asSharedFlow()

    fun requestFeedRefresh() {
        viewModelScope.launch {
            _feedRefreshRequest.emit(Unit)
        }
    }
    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()


    fun setCurrentUserData(currentUserData: UserProfile?) { _currentUser.value = currentUserData }
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()


    fun refreshUnreadCount() {
        viewModelScope.launch {
            // TODO: Call NotificationsRepository.getUnreadCount() and update state
        }
    }
}

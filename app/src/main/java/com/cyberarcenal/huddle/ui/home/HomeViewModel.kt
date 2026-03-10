package com.cyberarcenal.huddle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    // This ViewModel can hold global home-related state, e.g., unread notifications count.
    // For now, it's empty, but you can add shared data here if needed.

    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    fun refreshUnreadCount() {
        viewModelScope.launch {
            // TODO: Call NotificationsRepository.getUnreadCount() and update state
        }
    }
}
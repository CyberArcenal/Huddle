package com.cyberarcenal.huddle.ui.events.attendies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository

class EventAttendeesViewModelFactory(
    private val eventId: Int,
    private val attendanceRepository: EventAttendanceRepository,
    private val friendshipRepository: FriendshipsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventAttendeesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventAttendeesViewModel(eventId, attendanceRepository, friendshipRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
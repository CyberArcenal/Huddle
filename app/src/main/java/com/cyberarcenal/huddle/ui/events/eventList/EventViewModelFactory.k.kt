package com.cyberarcenal.huddle.ui.events.eventList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository

class EventViewModelFactory(
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(eventRepository, attendanceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
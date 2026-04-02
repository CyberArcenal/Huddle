package com.cyberarcenal.huddle.ui.events.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.EventAnalyticsRepository
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository

class EventManagementViewModelFactory(
    private val eventId: Int,
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository,
    private val analyticsRepository: EventAnalyticsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventManagementViewModel(eventId, eventRepository, attendanceRepository, analyticsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
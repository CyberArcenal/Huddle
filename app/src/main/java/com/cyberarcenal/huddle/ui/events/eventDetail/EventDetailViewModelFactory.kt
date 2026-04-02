package com.cyberarcenal.huddle.ui.events.eventDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository

class EventDetailViewModelFactory(
    private val eventId: Int,
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository,
    private val followRepository: FollowRepository,
    private val groupRepository: GroupRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventDetailViewModel(
                eventId,
                eventRepository,
                attendanceRepository,
                followRepository,
                groupRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
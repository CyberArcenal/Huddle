// EventManager.kt
package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventManager(
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _event = MutableStateFlow<EventDetail?>(null)
    val event: StateFlow<EventDetail?> = _event.asStateFlow()

    private val _attendance = MutableStateFlow<EventAttendance?>(null)
    val attendance: StateFlow<EventAttendance?> = _attendance.asStateFlow()

    private val _attendees = MutableStateFlow<List<EventAttendanceWithUser>>(emptyList())
    val attendees: StateFlow<List<EventAttendanceWithUser>> = _attendees.asStateFlow()

    private var _attendeesPage = 1
    private var _hasMoreAttendees = true
    private var currentEventId: Int? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setEvent(eventId: Int) {
        if (currentEventId == eventId) return
        currentEventId = eventId
        reset()
        loadEvent(eventId)
        loadAttendance(eventId)
        loadAttendees(eventId)
    }

    private fun reset() {
        _event.value = null
        _attendance.value = null
        _attendees.value = emptyList()
        _attendeesPage = 1
        _hasMoreAttendees = true
    }

    private fun loadEvent(eventId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            eventRepository.getEvent(eventId).fold(
                onSuccess = { event -> _event.value = event },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load event")
                }
            )
            _isLoading.value = false
        }
    }

    private fun loadAttendance(eventId: Int) {
        viewModelScope.launch {
            attendanceRepository.getAttendance(eventId).fold(
                onSuccess = { attendance -> _attendance.value = attendance },
                onFailure = { /* ignore, user may not have RSVPed */ }
            )
        }
    }

    private fun loadAttendees(eventId: Int) {
        viewModelScope.launch {
            attendanceRepository.getAttendees(eventId, page = _attendeesPage).fold(
                onSuccess = { paginated ->
                    _attendees.value = paginated.results
                    _hasMoreAttendees = paginated.hasNext
                    _attendeesPage++
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load attendees")
                }
            )
        }
    }

    fun rsvp(status: StatusDecEnum) {
        val eventId = currentEventId ?: return
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Updating RSVP...")
            val request = EventAttendanceCreateRequest(
                status = status,
                event = eventId,
            )
            attendanceRepository.rsvp(eventId, request).fold(
                onSuccess = { attendance ->
                    _attendance.value = attendance
                    actionState.value = ActionState.Success("RSVP updated")
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to update RSVP")
                }
            )
        }
    }

    fun loadMoreAttendees() {
        val eventId = currentEventId ?: return
        if (!_hasMoreAttendees) return
        viewModelScope.launch {
            attendanceRepository.getAttendees(eventId, page = _attendeesPage).fold(
                onSuccess = { paginated ->
                    _attendees.update { it + paginated.results }
                    _hasMoreAttendees = paginated.hasNext
                    _attendeesPage++
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load more attendees")
                }
            )
        }
    }

    fun sendReminders() {
        val eventId = currentEventId ?: return
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Sending reminders...")
            attendanceRepository.sendReminders(eventId).fold(
                onSuccess = { response ->
                    actionState.value = ActionState.Success("Reminders sent")
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to send reminders")
                }
            )
        }
    }

    fun clear() {
        currentEventId = null
        reset()
    }
}
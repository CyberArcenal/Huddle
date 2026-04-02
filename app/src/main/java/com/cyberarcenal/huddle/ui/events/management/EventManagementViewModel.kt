package com.cyberarcenal.huddle.ui.events.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.EventDetail
import com.cyberarcenal.huddle.api.models.EventAttendanceWithUser
import com.cyberarcenal.huddle.api.models.EventAnalyticsSummary
import com.cyberarcenal.huddle.api.models.PatchedEventUpdateRequest
import com.cyberarcenal.huddle.data.repositories.EventAnalyticsRepository
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventManagementViewModel(
    private val eventId: Int,
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository,
    private val analyticsRepository: EventAnalyticsRepository
) : ViewModel() {

    private val _event = MutableStateFlow<EventDetail?>(null)
    val event: StateFlow<EventDetail?> = _event.asStateFlow()

    private val _attendees = MutableStateFlow<List<EventAttendanceWithUser>>(emptyList())
    val attendees: StateFlow<List<EventAttendanceWithUser>> = _attendees.asStateFlow()

    private val _pendingAttendees = MutableStateFlow<List<EventAttendanceWithUser>>(emptyList())
    val pendingAttendees: StateFlow<List<EventAttendanceWithUser>> = _pendingAttendees.asStateFlow()

    private val _analytics = MutableStateFlow<EventAnalyticsSummary?>(null)
    val analytics: StateFlow<EventAnalyticsSummary?> = _analytics.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadEvent()
            loadAttendees()
            loadPendingAttendees()
            loadAnalytics()
            _isLoading.value = false
        }
    }

    private suspend fun loadEvent() {
        eventRepository.getEvent(eventId).fold(
            onSuccess = { response ->
                if (response.status) _event.value = response.data.event
                else _actionState.value = ActionState.Error(response.message)
            },
            onFailure = { error ->
                _actionState.value = ActionState.Error(error.message ?: "Failed to load event")
            }
        )
    }

    private suspend fun loadAttendees() {
        attendanceRepository.getAttendees(eventId, page = 1, pageSize = 200).fold(
            onSuccess = { response ->
                _attendees.value = response.data.results ?: emptyList()
            },
            onFailure = { error ->
                _actionState.value = ActionState.Error(error.message ?: "Failed to load attendees")
            }
        )
    }

    private suspend fun loadPendingAttendees() {
        // For private events, we need a separate endpoint for pending requests
        // Assuming there's a filter param "status=pending"
        attendanceRepository.getAttendees(eventId, page = 1, pageSize = 200, status = "pending").fold(
            onSuccess = { response ->
                _pendingAttendees.value = response.data.results ?: emptyList()
            },
            onFailure = { /* ignore, may not exist */ }
        )
    }

    private suspend fun loadAnalytics() {
        analyticsRepository.getEventAnalyticsSummary(eventId, days = 30).fold(
            onSuccess = { response ->
                if (response.status) _analytics.value = response.data.summary
            },
            onFailure = { /* ignore */ }
        )
    }

    fun updateEvent(updateRequest: PatchedEventUpdateRequest) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating event...")
            eventRepository.partialUpdateEvent(eventId, updateRequest).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _event.value = response.data.event
                        _actionState.value = ActionState.Success("Event updated")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Update failed")
                }
            )
        }
    }

    fun approveAttendee(userId: Int) {
        viewModelScope.launch {
            attendanceRepository.approveAttendee(eventId, userId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        // Move from pending to approved
                        val approved = _pendingAttendees.value.find { it.user?.id == userId }
                        _pendingAttendees.value = _pendingAttendees.value.filter { it.user?.id != userId }
                        approved?.let {
                            _attendees.value = _attendees.value + it
                        }
                        _actionState.value = ActionState.Success("Attendee approved")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Approval failed")
                }
            )
        }
    }

    fun rejectAttendee(userId: Int) {
        viewModelScope.launch {
            attendanceRepository.rejectAttendee(eventId, userId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _pendingAttendees.value = _pendingAttendees.value.filter { it.user?.id != userId }
                        _actionState.value = ActionState.Success("Attendee rejected")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Rejection failed")
                }
            )
        }
    }

    fun removeAttendee(userId: Int) {
        viewModelScope.launch {
            attendanceRepository.removeAttendanceForUser(eventId, userId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _attendees.value = _attendees.value.filter { it.user?.id != userId }
                        _actionState.value = ActionState.Success("Attendee removed")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Removal failed")
                }
            )
        }
    }

    fun clearActionState() {
        if (_actionState.value !is ActionState.Loading)
            _actionState.value = ActionState.Idle
    }
}

package com.cyberarcenal.huddle.ui.events.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.EventAttendance
import com.cyberarcenal.huddle.api.models.EventDetail
import com.cyberarcenal.huddle.api.models.EventStatistics
import com.cyberarcenal.huddle.api.models.StatusDecEnum
import com.cyberarcenal.huddle.data.repositories.events.EventsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class RsvpState {
    object Idle : RsvpState()
    object Loading : RsvpState()
    data class Success(val message: String) : RsvpState()
    data class Error(val message: String) : RsvpState()
}

class EventDetailViewModel(
    private val eventId: Int,
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _eventState = MutableStateFlow<EventDetail?>(null)
    val eventState: StateFlow<EventDetail?> = _eventState.asStateFlow()

    private val _eventLoading = MutableStateFlow(false)
    val eventLoading: StateFlow<Boolean> = _eventLoading.asStateFlow()

    private val _eventError = MutableStateFlow<String?>(null)
    val eventError: StateFlow<String?> = _eventError.asStateFlow()

    private val _statsState = MutableStateFlow<EventStatistics?>(null)
    val statsState: StateFlow<EventStatistics?> = _statsState.asStateFlow()

    private val _rsvpState = MutableStateFlow<RsvpState>(RsvpState.Idle)
    val rsvpState: StateFlow<RsvpState> = _rsvpState.asStateFlow()

    private val _currentUserAttendance = MutableStateFlow<EventAttendance?>(null)
    val currentUserAttendance: StateFlow<EventAttendance?> = _currentUserAttendance.asStateFlow()

    init {
        loadEventDetails()
    }

    fun loadEventDetails() {
        viewModelScope.launch {
            _eventLoading.value = true
            _eventError.value = null

            val eventResult = eventsRepository.getEvent(eventId)
            eventResult.fold(
                onSuccess = { event ->
                    _eventState.value = event
                    // Parse userStatus from event if available? The model has userStatus as String.
                    // We could parse it to get current user's RSVP status.
                },
                onFailure = { error ->
                    _eventError.value = error.message ?: "Failed to load event"
                }
            )

            val statsResult = eventsRepository.getEventStatistics(eventId)
            statsResult.fold(
                onSuccess = { stats -> _statsState.value = stats },
                onFailure = { /* ignore stats error */ }
            )

            // Try to get current user's attendance – need current user ID. For now, skip.
            _eventLoading.value = false
        }
    }

    fun rsvp(status: StatusDecEnum) {
        viewModelScope.launch {
            _rsvpState.value = RsvpState.Loading
            val result = eventsRepository.rsvpToEvent(eventId, status)
            result.fold(
                onSuccess = { attendance ->
                    _currentUserAttendance.value = attendance
                    _rsvpState.value = RsvpState.Success("RSVP updated")
                    // Refresh event details to update counts
                    loadEventDetails()
                },
                onFailure = { error ->
                    _rsvpState.value = RsvpState.Error(error.message ?: "Failed to RSVP")
                }
            )
        }
    }

    fun cancelRsvp() {
        // Need to delete attendance. Requires user ID. We'll need current user ID.
        // For now, skip.
    }

    fun refresh() {
        loadEventDetails()
    }

    fun clearRsvpState() {
        _rsvpState.value = RsvpState.Idle
    }
}
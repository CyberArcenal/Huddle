package com.cyberarcenal.huddle.ui.events.createevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.EventCreate
import com.cyberarcenal.huddle.api.models.EventDetail
import com.cyberarcenal.huddle.api.models.EventType8c2Enum
import com.cyberarcenal.huddle.data.repositories.events.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

sealed class CreateEventState {
    object Idle : CreateEventState()
    object Loading : CreateEventState()
    data class Success(val event: EventDetail) : CreateEventState()
    data class Error(val message: String) : CreateEventState()
}

class CreateEventViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _startDate = MutableStateFlow<OffsetDateTime?>(null)
    val startDate: StateFlow<OffsetDateTime?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<OffsetDateTime?>(null)
    val endDate: StateFlow<OffsetDateTime?> = _endDate.asStateFlow()

    private val _eventType = MutableStateFlow(EventType8c2Enum.PUBLIC)
    val eventType: StateFlow<EventType8c2Enum> = _eventType.asStateFlow()

    private val _groupId = MutableStateFlow<Int?>(null)
    val groupId: StateFlow<Int?> = _groupId.asStateFlow()

    private val _maxAttendees = MutableStateFlow<Long?>(null)
    val maxAttendees: StateFlow<Long?> = _maxAttendees.asStateFlow()

    private val _createState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val createState: StateFlow<CreateEventState> = _createState.asStateFlow()

    private val _titleError = MutableStateFlow<String?>(null)
    val titleError: StateFlow<String?> = _titleError.asStateFlow()

    private val _dateError = MutableStateFlow<String?>(null)
    val dateError: StateFlow<String?> = _dateError.asStateFlow()

    fun updateTitle(title: String) {
        _title.value = title
        if (title.isNotBlank()) _titleError.value = null
    }

    fun updateDescription(desc: String) {
        _description.value = desc
    }

    fun updateLocation(loc: String) {
        _location.value = loc
    }

    fun updateStartDate(date: OffsetDateTime?) {
        _startDate.value = date
        _dateError.value = null
    }

    fun updateEndDate(date: OffsetDateTime?) {
        _endDate.value = date
        _dateError.value = null
    }

    fun updateEventType(type: EventType8c2Enum) {
        _eventType.value = type
    }

    fun updateGroupId(id: Int?) {
        _groupId.value = id
    }

    fun updateMaxAttendees(attendees: String) {
        _maxAttendees.value = attendees.toLongOrNull()
    }

    fun createEvent() {
        val title = _title.value.trim()
        val start = _startDate.value
        val end = _endDate.value

        if (title.isEmpty()) {
            _titleError.value = "Title is required"
            return
        }
        if (start == null || end == null) {
            _dateError.value = "Start and end dates are required"
            return
        }
        if (end.isBefore(start)) {
            _dateError.value = "End time must be after start time"
            return
        }

        viewModelScope.launch {
            _createState.value = CreateEventState.Loading
            val create = EventCreate(
                title = title,
                description = _description.value.trim(),
                location = _location.value.trim(),
                startTime = start,
                endTime = end,
                eventType = _eventType.value,
                group = _groupId.value,
                maxAttendees = _maxAttendees.value
            )
            val result = eventsRepository.createEvent(create)
            result.fold(
                onSuccess = { event ->
                    _createState.value = CreateEventState.Success(event)
                },
                onFailure = { error ->
                    _createState.value = CreateEventState.Error(error.message ?: "Failed to create event")
                }
            )
        }
    }

    fun resetState() {
        _createState.value = CreateEventState.Idle
    }

    fun clearErrors() {
        _titleError.value = null
        _dateError.value = null
    }
}
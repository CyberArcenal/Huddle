package com.cyberarcenal.huddle.ui.events.eventList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.EventAttendanceCreateRequest
import com.cyberarcenal.huddle.api.models.StatusDecEnum
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventViewModel(
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Current selected tab: 0 = Discover, 1 = My Events, 2 = Organized
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(index: Int) { _selectedTab.value = index }

    // Paging source for each tab
    fun getDiscoverEventsPager(): Flow<PagingData<EventList>> = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        EventPagingSource(eventRepository, isUpcoming = true, userId = null)
    }.flow.cachedIn(viewModelScope)

    fun getMyEventsPager(): Flow<PagingData<EventList>> = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        EventPagingSource(eventRepository, isUpcoming = true, userId = null, myEvents = true)
    }.flow.cachedIn(viewModelScope)

    fun getOrganizedEventsPager(): Flow<PagingData<EventList>> = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        EventPagingSource(eventRepository, isUpcoming = true, userId = null, organizedByMe = true)
    }.flow.cachedIn(viewModelScope)

    fun rsvp(eventId: Int, status: StatusDecEnum) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating RSVP...")
            val request = EventAttendanceCreateRequest(event = eventId, status = status)
            attendanceRepository.rsvp(eventId, request).fold(
                onSuccess = { response ->
                    _actionState.value = if (response.status)
                        ActionState.Success("RSVP updated")
                    else ActionState.Error(response.message)
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "RSVP failed")
                }
            )
        }
    }

    fun clearActionState() {
        if (_actionState.value !is ActionState.Loading)
            _actionState.value = ActionState.Idle
    }
}
package com.cyberarcenal.huddle.ui.events.eventDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val eventId: Int,
    private val eventRepository: EventRepository,
    private val attendanceRepository: EventAttendanceRepository,
    private val followRepository: FollowRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _event = MutableStateFlow<EventDetail?>(null)
    val event: StateFlow<EventDetail?> = _event.asStateFlow()

    private val _attendance = MutableStateFlow<EventAttendance?>(null)
    val attendance: StateFlow<EventAttendance?> = _attendance.asStateFlow()

    private val _attendees = MutableStateFlow<List<EventAttendanceWithUser>>(emptyList())
    val attendees: StateFlow<List<EventAttendanceWithUser>> = _attendees.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _isFollowingOrganizer = MutableStateFlow(false)
    val isFollowingOrganizer: StateFlow<Boolean> = _isFollowingOrganizer.asStateFlow()

    private val _isGroupMember = MutableStateFlow(false)
    val isGroupMember: StateFlow<Boolean> = _isGroupMember.asStateFlow()

    private val _isJoiningGroup = MutableStateFlow(false)
    val isJoiningGroup: StateFlow<Boolean> = _isJoiningGroup.asStateFlow()

    init {
        loadEventData()
    }

    private fun loadEventData() {
        viewModelScope.launch {
            _isLoading.value = true
            // Load event details
            eventRepository.getEvent(eventId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _event.value = response.data.event
                        // After event loaded, load attendance, attendees, follow status, group membership
                        loadAttendance()
                        loadAttendees()
                        checkFollowOrganizer()
                        checkGroupMembership()
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to load event")
                }
            )
            _isLoading.value = false
        }
    }

    private fun loadAttendance() {
        viewModelScope.launch {
            attendanceRepository.getAttendance(eventId).fold(
                onSuccess = { response ->
                    if (response.status) _attendance.value = response.data.attendance
                },
                onFailure = { /* ignore, user may not have RSVPed */ }
            )
        }
    }

    private fun loadAttendees() {
        viewModelScope.launch {
            attendanceRepository.getAttendees(eventId, page = 1, pageSize = 10).fold(
                onSuccess = { paginated ->
                    _attendees.value = paginated.data.results ?: emptyList()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to load attendees")
                }
            )
        }
    }

    private fun checkFollowOrganizer() {
        viewModelScope.launch {
            val organizerId = _event.value?.organizer?.id ?: return@launch
            followRepository.getFollowStatus(organizerId).fold(
                onSuccess = { response ->
                    _isFollowingOrganizer.value = response.data.isFollowing
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    private fun checkGroupMembership() {
        viewModelScope.launch {
            val groupId = _event.value?.group?.id ?: return@launch
            groupRepository.getGroup(groupId).fold(
                onSuccess = { response ->
                    _isGroupMember.value = response.data.group.isMember ?: false
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun rsvp(status: StatusDecEnum) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating RSVP...")
            val request = EventAttendanceCreateRequest(event = eventId, status = status)
            attendanceRepository.rsvp(eventId, request).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _attendance.value = response.data.attendance
                        // Refresh attendees list to update counts
                        loadAttendees()
                        _actionState.value = ActionState.Success("RSVP updated")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "RSVP failed")
                }
            )
        }
    }

    fun cancelRsvp() {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Removing RSVP...")
            attendanceRepository.removeAttendance(eventId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _attendance.value = null
                        loadAttendees()
                        _actionState.value = ActionState.Success("RSVP cancelled")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to cancel RSVP")
                }
            )
        }
    }

    fun toggleFollowOrganizer() {
        val organizer = _event.value?.organizer ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Updating follow status...")
            if (_isFollowingOrganizer.value) {
                followRepository.unfollowUser(UnfollowUserRequest(organizer.id!!)).fold(
                    onSuccess = {
                        _isFollowingOrganizer.value = false
                        _actionState.value = ActionState.Success("Unfollowed ${organizer.username}")
                    },
                    onFailure = { error ->
                        _actionState.value = ActionState.Error(error.message ?: "Unfollow failed")
                    }
                )
            } else {
                followRepository.followUser(FollowUserRequest(organizer.id!!)).fold(
                    onSuccess = {
                        _isFollowingOrganizer.value = true
                        _actionState.value = ActionState.Success("Following ${organizer.username}")
                    },
                    onFailure = { error ->
                        _actionState.value = ActionState.Error(error.message ?: "Follow failed")
                    }
                )
            }
        }
    }

    fun joinGroup() {
        val groupId = _event.value?.group?.id ?: return
        viewModelScope.launch {
            _isJoiningGroup.value = true
            groupRepository.joinGroup(groupId).fold(
                onSuccess = { response ->
                    if (response.status) {
                        _isGroupMember.value = true
                        _actionState.value = ActionState.Success("Joined group")
                    } else {
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to join group")
                }
            )
            _isJoiningGroup.value = false
        }
    }

    fun clearActionState() {
        if (_actionState.value !is ActionState.Loading)
            _actionState.value = ActionState.Idle
    }
}
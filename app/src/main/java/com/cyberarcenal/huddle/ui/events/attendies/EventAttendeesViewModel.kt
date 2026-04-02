package com.cyberarcenal.huddle.ui.events.attendies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.EventAttendanceWithUser
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository
import kotlinx.coroutines.flow.*

class EventAttendeesViewModel(
    private val eventId: Int,
    private val attendanceRepository: EventAttendanceRepository,
    private val friendshipsRepository: FriendshipsRepository
) : ViewModel() {

    // Filter & sort states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.JOINED_RECENT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedPersonalityType = MutableStateFlow<String?>(null)
    val selectedPersonalityType: StateFlow<String?> = _selectedPersonalityType.asStateFlow()

    private val _showFriendsOnly = MutableStateFlow(false)
    val showFriendsOnly: StateFlow<Boolean> = _showFriendsOnly.asStateFlow()

    // Paging flow that recomposes when any filter changes
    val attendeesPagingFlow: Flow<PagingData<EventAttendanceWithUser>> = combine(
        _searchQuery,
        _sortOption,
        _selectedPersonalityType,
        _showFriendsOnly
    ) { query, sort, personality, friendsOnly ->
        Triple(query, sort, personality) to friendsOnly
    }.flatMapLatest { (params, friendsOnly) ->
        val (query, sort, personality) = params
        Pager(
            PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            EventAttendeesPagingSource(
                repository = attendanceRepository,
                eventId = eventId,
                searchQuery = query,
                personality = personality,
                sort = sort.toApiString(),
                friendsOnly = friendsOnly
            )
        }.flow
    }.cachedIn(viewModelScope)

    init {
        // No need to load friends list – backend handles friends_only filter
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSortOption(option: SortOption) { _sortOption.value = option }
    fun updatePersonalityFilter(type: String?) { _selectedPersonalityType.value = type }
    fun toggleFriendsOnly() { _showFriendsOnly.value = !_showFriendsOnly.value }

    enum class SortOption(val apiString: String) {
        ALPHABETICAL("name"),
        JOINED_RECENT("joined_at"),
        CAPABILITY_SCORE("capability_score");

        fun toApiString(): String = apiString
    }
}
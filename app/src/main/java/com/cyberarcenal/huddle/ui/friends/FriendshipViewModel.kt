package com.cyberarcenal.huddle.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.managers.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class FriendshipUiState {
    object Loading : FriendshipUiState()
    data class Success(
        val friends: List<FriendshipMinimal> = emptyList(),
        val incomingRequests: List<FriendshipMinimal> = emptyList(),
        val outgoingRequests: List<FriendshipMinimal> = emptyList(),
        val suggestions: List<UserMutualCount> = emptyList(),
        val pinnedFriends: List<FriendshipMinimal> = emptyList()
    ) : FriendshipUiState()

    data class Error(val message: String) : FriendshipUiState()
}

class FriendshipViewModel(
    private val friendshipRepository: FriendshipsRepository,
    private val followRepository: FollowRepository,
    private val matchingRepository: UserMatchingRepository,
    private val searchRepository: UserSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FriendshipUiState>(FriendshipUiState.Loading)
    val uiState: StateFlow<FriendshipUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Connections, 1: Requests, 2: Suggestions
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _requestType = MutableStateFlow("incoming") // incoming or outgoing
    val requestType: StateFlow<String> = _requestType.asStateFlow()

    // Shared action state for Snackbars/Loading feedback
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Integrated Managers
    val friendshipManager = FriendshipManager(friendshipRepository, viewModelScope)
    val followManager = FollowManager(followRepository, viewModelScope, _actionState)
    val matchingManager = MatchingManager(matchingRepository, viewModelScope, _actionState)
    val searchManager = SearchManager(searchRepository, viewModelScope, _actionState)

    init {
        // Observe friendship manager events to refresh UI on success
        viewModelScope.launch {
            friendshipManager.events.collect { result ->
                if (result is FriendshipResult.Success) {
                    loadAllData()
                }
            }
        }

        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = FriendshipUiState.Loading
            try {
                // Fetch basic friendship data
                val friendsRes = friendshipRepository.getFriends()
                val requestsRes = friendshipRepository.getPendingRequests()
                val suggestionsRes = followRepository.getSuggestedUsers()

                if (friendsRes.isSuccess && requestsRes.isSuccess && suggestionsRes.isSuccess) {
                    val allFriends = friendsRes.getOrNull()?.data?.results ?: emptyList()
                    val allRequests = requestsRes.getOrNull()?.data?.results ?: emptyList()

                    _uiState.value = FriendshipUiState.Success(
                        friends = allFriends,
                        incomingRequests = allRequests.filter { it.status == Status7baEnum.PENDING },
                        suggestions = suggestionsRes.getOrNull()?.data?.results ?: emptyList(),
                        pinnedFriends = allFriends.filter { it.tag == TagEnum.PINNED }
                    )

                    // Pre-load matches and social suggestions via MatchingManager
                    matchingManager.loadMatches(limit = 10)
                    matchingManager.loadSuggestions()

                } else {
                    _uiState.value = FriendshipUiState.Error("Failed to load connection data")
                }
            } catch (e: Exception) {
                _uiState.value = FriendshipUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setRequestType(type: String) {
        _requestType.value = type
    }

    fun togglePinFriend(friendshipId: Int, currentTag: TagEnum?) {
        viewModelScope.launch {
            val newTag = if (currentTag == TagEnum.PINNED) null else TagEnum.PINNED
            newTag?.let {
                val result = friendshipRepository.updateFriendTag(
                    friendshipId,
                    PatchedTagUpdateRequest(tag = newTag)
                )
                if (result.isSuccess) {
                    loadAllData()
                }
            }

        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}

package com.cyberarcenal.huddle.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.FollowUserRequest
import com.cyberarcenal.huddle.api.models.UnfollowUserRequest
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import com.cyberarcenal.huddle.data.repositories.UserMatchingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FriendsTab(val displayName: String) {
    FOLLOWERS("Followers"),
    FOLLOWING("Following"),
    MOOTS("Moots"),
    SUGGESTIONS("Suggestions"),
    MATCHES("Matches"),
    POPULAR("Popular")
}

class FriendsViewModel(
    private val userId: Int?,
    private val userFollowRepository: FollowRepository,
    private val userProfileRepository: UsersRepository,
    private val matchingRepository: UserMatchingRepository
) : ViewModel() {

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    // Optimistic follow status overrides
    private val _followingOverrides = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val followingOverrides: StateFlow<Map<Int, Boolean>> = _followingOverrides.asStateFlow()

    // Loading state per user (for follow/unfollow)
    private val _loadingUsers = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val loadingUsers: StateFlow<Map<Int, Boolean>> = _loadingUsers.asStateFlow()

    // Paging flows for each tab
    val followersFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.FOLLOWERS)
    val followingFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.FOLLOWING)
    val mootsFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.MOOTS)
    val suggestionsFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.SUGGESTIONS)
    val matchesFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.MATCHES)
    val popularFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.POPULAR)

    private fun createPager(tab: FriendsTab): Flow<PagingData<UserMinimal>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        FriendsPagingSource(
            followRepository = userFollowRepository,
            matchingRepository = matchingRepository,
            userId = userId,
            tab = tab
        )
    }.flow.cachedIn(viewModelScope)

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    fun toggleFollow(targetUserId: Int, currentlyFollowing: Boolean) {
        // Set loading state for this user
        _loadingUsers.update { it + (targetUserId to true) }

        // Optimistic update of follow status
        val newStatus = !currentlyFollowing
        _followingOverrides.update { it + (targetUserId to newStatus) }

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                userFollowRepository.unfollowUser(UnfollowUserRequest(followingId = targetUserId))
            } else {
                userFollowRepository.followUser(FollowUserRequest(followingId = targetUserId))
            }

            // Clear loading state
            _loadingUsers.update { it - targetUserId }

            if (result.isFailure) {
                // Revert optimistic update on failure
                _followingOverrides.update { it + (targetUserId to currentlyFollowing) }
            }
        }
    }
}

class FriendsViewModelFactory(
    private val userId: Int?,
    private val userFollowRepository: FollowRepository,
    private val userProfileRepository: UsersRepository,
    private val matchingRepository: UserMatchingRepository = UserMatchingRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel(userId, userFollowRepository, userProfileRepository, matchingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
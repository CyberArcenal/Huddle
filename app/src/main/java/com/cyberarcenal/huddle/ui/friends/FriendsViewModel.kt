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
import com.cyberarcenal.huddle.data.repositories.FollowViewsRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FriendsTab(val displayName: String) {
    FOLLOWERS("Followers"),
    FOLLOWING("Following"),
    MOOTS("Moots"),
    SUGGESTIONS("Suggestions"),
    POPULAR("Popular")
}

class FriendsViewModel(
    private val userId: Int?,
    private val userFollowRepository: FollowViewsRepository,
    private val userProfileRepository: UsersRepository
) : ViewModel() {

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    // Local override for following status to provide immediate UI feedback (Optimistic UI)
    private val _followingOverrides = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val followingOverrides: StateFlow<Map<Int, Boolean>> = _followingOverrides.asStateFlow()

    // Paging flows for each tab
    val followersFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.FOLLOWERS)
    val followingFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.FOLLOWING)
    val mootsFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.MOOTS)
    val suggestionsFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.SUGGESTIONS)
    val popularFlow: Flow<PagingData<UserMinimal>> = createPager(FriendsTab.POPULAR)

    private fun createPager(tab: FriendsTab): Flow<PagingData<UserMinimal>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        FriendsPagingSource(
            repository = userFollowRepository,
            userId = userId,
            tab = tab
        )
    }.flow.cachedIn(viewModelScope)

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    fun toggleFollow(targetUserId: Int?, currentlyFollowing: Boolean) {
        if (targetUserId == null) return
        
        val newStatus = !currentlyFollowing
        
        // Optimistic update
        _followingOverrides.update { it + (targetUserId to newStatus) }

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                val request = UnfollowUserRequest(followingId = targetUserId)
                userFollowRepository.unfollowUser(request)
            } else {
                val request = FollowUserRequest(followingId = targetUserId)
                userFollowRepository.followUser(request)
            }
            
            result.onFailure {
                // Revert on failure
                _followingOverrides.update { it + (targetUserId to currentlyFollowing) }
            }
        }
    }
}

class FriendsViewModelFactory(
    private val userId: Int?,
    private val userFollowRepository: FollowViewsRepository,
    private val userProfileRepository: UsersRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel(userId, userFollowRepository, userProfileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
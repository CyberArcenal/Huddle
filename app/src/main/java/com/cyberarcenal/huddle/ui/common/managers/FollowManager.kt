package com.cyberarcenal.huddle.ui.common.managers

import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.FollowStatsResponse
import com.cyberarcenal.huddle.api.models.FollowStatusResponse
import com.cyberarcenal.huddle.api.models.FollowUserRequest
import com.cyberarcenal.huddle.api.models.UnfollowUserRequest
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus

/**
 * Manages follow-related operations and state for a single target user.
 * Use this in ViewModels that need to follow/unfollow a user and display their follow status/stats.
 */
class FollowManager(
    private val followRepository: FollowRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    // State for the currently managed user
    private val _followStatus = MutableStateFlow<FollowStatusResponse?>(null)
    val followStatus: StateFlow<FollowStatusResponse?> = _followStatus.asStateFlow()

    private val _followStats = MutableStateFlow<FollowStatsResponse?>(null)
    val followStats: StateFlow<FollowStatsResponse?> = _followStats.asStateFlow()

    // Loading indicators
    private val _isFollowingLoading = MutableStateFlow(false)
    val isFollowingLoading: StateFlow<Boolean> = _isFollowingLoading.asStateFlow()

    private val _isStatsLoading = MutableStateFlow(false)
    val isStatsLoading: StateFlow<Boolean> = _isStatsLoading.asStateFlow()

    private val _followStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val followStatuses: StateFlow<Map<Int, Boolean>> = _followStatuses.asStateFlow()

    private var currentTargetUserId: Int? = null

    /**
     * Sets the user to manage. Clears old state and loads fresh data.
     */
    fun setTargetUser(userId: Int) {
        if (currentTargetUserId == userId) return
        currentTargetUserId = userId
        _followStatus.value = null
        _followStats.value = null
        loadFollowStatus(userId)
        loadFollowStats(userId)
    }

    /**
     * Follows the given user. Automatically sets them as the target if not already.
     */
    fun followUser(userId: Int) {
        if (currentTargetUserId != userId) setTargetUser(userId)
        viewModelScope.launch {
            _isFollowingLoading.value = true
            actionState.value = ActionState.Loading("Following...")
            val request = FollowUserRequest(followingId = userId)
            followRepository.followUser(request).fold(
                onSuccess = { response ->
                    // Optimistically update status
                    _followStatus.update { old ->
                        old?.copy(isFollowing = true) ?: FollowStatusResponse(
                            isFollowing = true,
                            userId = userId,
                            username = old?.username ?: ""
                        )
                    }
                    // Refresh stats to get updated counts
                    loadFollowStats(userId)
                    actionState.value = ActionState.Success(response.message)
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to follow user")
                }
            )
            _isFollowingLoading.value = false
        }
    }

    /**
     * Unfollows the given user. Automatically sets them as the target if not already.
     */
    fun unfollowUser(userId: Int) {
        if (currentTargetUserId != userId) setTargetUser(userId)
        viewModelScope.launch {
            _isFollowingLoading.value = true
            actionState.value = ActionState.Loading("Unfollowing...")
            val request = UnfollowUserRequest(followingId = userId)
            followRepository.unfollowUser(request).fold(
                onSuccess = { response ->
                    _followStatus.update { old ->
                        old?.copy(isFollowing = false) ?: FollowStatusResponse(
                            isFollowing = false,
                            userId = userId,
                            username = old?.username ?: ""
                        )
                    }
                    loadFollowStats(userId)
                    actionState.value = ActionState.Success(response.message)
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to unfollow user")
                }
            )
            _isFollowingLoading.value = false
        }
    }


    fun toggleFollow(userId: Int, currentIsFollowing: Boolean, username: String) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading(
                if (currentIsFollowing) "Unfollowing..." else "Following..."
            )

            // Optimistic update
            _followStatuses.update { it + (userId to !currentIsFollowing) }

            val result = if (currentIsFollowing) {
                followRepository.unfollowUser(UnfollowUserRequest(followingId = userId))
            } else {
                followRepository.followUser(FollowUserRequest(followingId = userId))
            }

            result.fold(
                onSuccess = { response ->
                    actionState.value = ActionState.Success(
                        if (currentIsFollowing) "Unfollowed $username" else "You are now following $username"
                    )
                },
                onFailure = { error ->
                    // Revert optimistic update
                    _followStatuses.update { it + (userId to currentIsFollowing) }
                    actionState.value = ActionState.Error(
                        error.message ?: "Action failed"
                    )
                }
            )
        }
    }

    /**
     * Convenience to get current follow status (true = following).
     */
    fun isFollowing(): Boolean = _followStatus.value?.isFollowing ?: false

    /**
     * Clears all state when the manager is no longer needed.
     */
    fun clear() {
        currentTargetUserId = null
        _followStatus.value = null
        _followStats.value = null
        _isFollowingLoading.value = false
        _isStatsLoading.value = false
    }

    // Private loaders
    private fun loadFollowStatus(userId: Int) {
        viewModelScope.launch {
            _isFollowingLoading.value = true
            followRepository.getFollowStatus(userId).fold(
                onSuccess = { status ->
                    _followStatus.value = status
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error("Failed to load follow status: ${error.message}")
                }
            )
            _isFollowingLoading.value = false
        }
    }

    private fun loadFollowStats(userId: Int) {
        viewModelScope.launch {
            _isStatsLoading.value = true
            followRepository.getFollowStatsForUser(userId).fold(
                onSuccess = { stats ->
                    _followStats.value = stats
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error("Failed to load follow stats: ${error.message}")
                }
            )
            _isStatsLoading.value = false
        }
    }
}
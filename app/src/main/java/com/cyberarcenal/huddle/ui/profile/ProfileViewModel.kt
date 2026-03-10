package com.cyberarcenal.huddle.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.users.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userId: Int?,
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _followActionState = MutableStateFlow<FollowActionState>(FollowActionState.Idle)
    val followActionState: StateFlow<FollowActionState> = _followActionState.asStateFlow()

    val userPostsFlow: Flow<PagingData<PostFeed>> = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        ProfilePagingSource(userId ?: -1, repository)
    }.flow.cachedIn(viewModelScope)

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = if (userId == null) {
                repository.getCurrentUserProfile()
            } else {
                repository.getUserProfile(userId)
            }
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = ProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value = ProfileState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun followUser() {
        val currentState = _profileState.value
        if (currentState !is ProfileState.Success) return
        val targetUserId = currentState.profile.id
        viewModelScope.launch {
            _followActionState.value = FollowActionState.Loading
            val result = repository.followUser(targetUserId)
            result.fold(
                onSuccess = {
                    _followActionState.value = FollowActionState.Success(true)
                    loadProfile()
                },
                onFailure = { error ->
                    _followActionState.value = FollowActionState.Error(error.message ?: "Failed to follow")
                }
            )
        }
    }

    fun unfollowUser() {
        val currentState = _profileState.value
        if (currentState !is ProfileState.Success) return
        val targetUserId = currentState.profile.id
        viewModelScope.launch {
            _followActionState.value = FollowActionState.Loading
            val result = repository.unfollowUser(targetUserId)
            result.fold(
                onSuccess = {
                    _followActionState.value = FollowActionState.Success(false)
                    loadProfile()
                },
                onFailure = { error ->
                    _followActionState.value = FollowActionState.Error(error.message ?: "Failed to unfollow")
                }
            )
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class FollowActionState {
    object Idle : FollowActionState()
    object Loading : FollowActionState()
    data class Success(val isFollowing: Boolean) : FollowActionState()
    data class Error(val message: String) : FollowActionState()
}

class ProfileViewModelFactory(private val userId: Int?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userId, ProfileRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
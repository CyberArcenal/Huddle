package com.cyberarcenal.huddle.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.feed.FeedRepository
import com.cyberarcenal.huddle.data.repositories.users.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userId: Int?,
    private val repository: ProfileRepository,
    private val feedRepository: FeedRepository // Added for like functionality
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        loadProfile()
    }

    // Flow for posts that reacts to profile loading
    @OptIn(ExperimentalCoroutinesApi::class)
    val userPostsFlow: Flow<PagingData<PostFeed>> = profileState.flatMapLatest { state ->
        val idToLoad = when {
            // If viewing own profile, use ID from the loaded profile
            userId == null && state is ProfileState.Success -> state.profile.id
            // If viewing another profile, use the passed userId
            userId != null -> userId
            else -> null
        }

        if (idToLoad != null) {
            Pager(PagingConfig(pageSize = 10)) {
                ProfilePagingSource(idToLoad, repository)
            }.flow
        } else {
            flowOf(PagingData.empty()) // Return empty if no ID is available yet
        }
    }.cachedIn(viewModelScope)

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = if (userId == null) {
                repository.getCurrentUserProfile()
            } else {
                repository.getUserProfile(userId)
            }
            result.fold(
                onSuccess = { profile -> _profileState.value = ProfileState.Success(profile) },
                onFailure = { error -> _profileState.value = ProfileState.Error(error.message ?: "Failed to load profile") }
            )
        }
    }

    fun onFollowToggle() {
        val profile = (_profileState.value as? ProfileState.Success)?.profile ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading()
            val result = if (profile.isFollowing) repository.unfollowUser(profile.id) else repository.followUser(profile.id)
            result.fold(
                onSuccess = { loadProfile() }, // Refresh profile on success
                onFailure = { _actionState.value = ActionState.Error(it.message ?: "Action failed") }
            )
        }
    }

    fun toggleLike(postId: Int) {
        viewModelScope.launch {
            feedRepository.toggleLike(postId)
            // We just trigger the call; the UI should optimistically update or refresh.
        }
    }
    
    fun changeProfilePicture(uri: Uri) {
        // TODO: Implement file upload logic in ProfileRepository
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Uploading...")
            kotlinx.coroutines.delay(2000) // Simulate upload
            _actionState.value = ActionState.Success("Picture updated!")
            loadProfile()
        }
    }
}

// --- States and Factory ---

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ActionState {
    object Idle : ActionState()
    data class Loading(val message: String? = null) : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}

class ProfileViewModelFactory(private val userId: Int?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                userId = userId, 
                repository = ProfileRepository(),
                feedRepository = FeedRepository() // Provide FeedRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
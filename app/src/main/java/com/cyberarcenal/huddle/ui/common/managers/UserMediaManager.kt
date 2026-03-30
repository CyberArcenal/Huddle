package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.UserMediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class UserMediaManager(
    private val userMediaRepository: UserMediaRepository,
    private val viewModelScope: CoroutineScope,
    private val currentUser: MutableStateFlow<UserProfile?>,
    private val currentUserId: MutableStateFlow<Int?>
) {
    fun loadUserImage() {
        val userId = currentUserId.value ?: return
        val currentProfile = currentUser.value
        
        // Load only if profile picture is missing
        if (currentProfile?.profilePictureUrl.isNullOrBlank()) {
            viewModelScope.launch {
                userMediaRepository.getProfilePicture(userId).onSuccess { mediaResponse ->
                    currentUser.value = currentUser.value?.copy(profilePictureUrl = mediaResponse.imageUrl)
                }
            }
        }
    }
}
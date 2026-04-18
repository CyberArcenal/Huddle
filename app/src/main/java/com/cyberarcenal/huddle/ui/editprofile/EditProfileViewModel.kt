package com.cyberarcenal.huddle.ui.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.UpdateFirstNameUpdateFirstNameInputRequest
import com.cyberarcenal.huddle.api.models.UpdateLastNameUpdateLastNameInputRequest
import com.cyberarcenal.huddle.api.models.UserProfileSchemaUpdateRequest
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userProfileRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userProfileRepository.getProfile().fold(
                onSuccess = { response ->
                    val data = response.data
                    val profile = data.user
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        firstName = profile.firstName ?: "",
                        middleName = profile.middleName ?: "",
                        lastName = profile.lastName ?: "",
                        bio = profile.bio ?: "",
                        phoneNumber = profile.phoneNumber ?: "",
                        location = profile.location ?: ""
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    fun onFirstNameChange(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName)
    }

    fun onLastNameChange(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName)
    }

    fun onBioChange(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio)
    }

    fun onPhoneNumberChange(phone: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone)
    }

    fun onLocationChange(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentState = _uiState.value

            try {
                // Update First Name
                userProfileRepository.updateFirstName(UpdateFirstNameUpdateFirstNameInputRequest(currentState.firstName))
                    .onFailure { throw Exception("Failed to update first name: ${it.message}") }

                // Update Last Name
                userProfileRepository.updateLastName(UpdateLastNameUpdateLastNameInputRequest(currentState.lastName))
                    .onFailure { throw Exception("Failed to update last name: ${it.message}") }

                // Update Other Fields
                val userUpdate = UserProfileSchemaUpdateRequest(
                    bio = currentState.bio,
                    phoneNumber = currentState.phoneNumber,
                    location = currentState.location,
                )

                userProfileRepository.updateProfile(userUpdate)
                    .onFailure { throw Exception("Failed to update profile: ${it.message}") }

                _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val bio: String = "",
    val phoneNumber: String = "",
    val location: String = ""
)

class EditProfileViewModelFactory(
    private val userProfileRepository: UsersRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(userProfileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
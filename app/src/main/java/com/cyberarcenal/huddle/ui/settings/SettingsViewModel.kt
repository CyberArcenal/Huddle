package com.cyberarcenal.huddle.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.LogOutRepository
import com.cyberarcenal.huddle.data.repositories.PasswordResetRepository
import com.cyberarcenal.huddle.data.repositories.UserSecurityRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import com.cyberarcenal.huddle.network.AuthManager
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

// Existing sealed classes
sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    data class Success(val message: String) : LogoutState()
    data class Error(val message: String) : LogoutState()
}

sealed class PasswordChangeState {
    object Idle : PasswordChangeState()
    object Loading : PasswordChangeState()
    data class Success(val message: String) : PasswordChangeState()
    data class Error(val message: String) : PasswordChangeState()
}

sealed class TwoFactorState {
    object Idle : TwoFactorState()
    object Loading : TwoFactorState()
    data class Success(val enabled: Boolean) : TwoFactorState()
    data class Error(val message: String) : TwoFactorState()
}

sealed class SessionActionState {
    object Idle : SessionActionState()
    object Loading : SessionActionState()
    data class Success(val message: String) : SessionActionState()
    data class Error(val message: String) : SessionActionState()
}

sealed class AccountDeactivationState {
    object Idle : AccountDeactivationState()
    object Loading : AccountDeactivationState()
    data class Success(val message: String) : AccountDeactivationState()
    data class Error(val message: String) : AccountDeactivationState()
}

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val message: String) : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

// New sealed class for Snackbar messages
sealed class SnackbarMessage {
    data class Success(val message: String) : SnackbarMessage()
    data class Error(val message: String) : SnackbarMessage()
    data class Info(val message: String) : SnackbarMessage()
}

class SettingsViewModel(
    private val userProfileRepository: UsersRepository,
    private val userSecurityRepository: UserSecurityRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val logOutRepository: LogOutRepository,
) : ViewModel() {

    // User profile
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Security settings
    private val _securitySettings = MutableStateFlow<UpdateSecuritySettingsRequest?>(null)
    val securitySettings: StateFlow<UpdateSecuritySettingsRequest?> = _securitySettings.asStateFlow()

    // 2FA status
    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    // Sessions
    private val _sessions = MutableStateFlow<List<LoginSession>>(emptyList())
    val sessions: StateFlow<List<LoginSession>> = _sessions.asStateFlow()

    // Loading states
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Password change
    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    // 2FA state
    private val _twoFactorState = MutableStateFlow<TwoFactorState>(TwoFactorState.Idle)
    val twoFactorState: StateFlow<TwoFactorState> = _twoFactorState.asStateFlow()

    // Session action state
    private val _sessionActionState = MutableStateFlow<SessionActionState>(SessionActionState.Idle)
    val sessionActionState: StateFlow<SessionActionState> = _sessionActionState.asStateFlow()

    // Account deactivation state
    private val _deactivationState = MutableStateFlow<AccountDeactivationState>(AccountDeactivationState.Idle)
    val deactivationState: StateFlow<AccountDeactivationState> = _deactivationState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    // Profile update state
    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState.asStateFlow()

    // Snackbar state
    private val _snackbarMessage = MutableStateFlow<SnackbarMessage?>(null)
    val snackbarMessage: StateFlow<SnackbarMessage?> = _snackbarMessage.asStateFlow()

    init {
        loadSettings()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun postSnackbar(message: SnackbarMessage) {
        _snackbarMessage.value = message
    }

    fun loadSettings() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // Load user profile
            val profileResult = userProfileRepository.getProfile()
            profileResult.fold(
                onSuccess = { profile ->
                    if (profile.status) {
                        _userProfile.value = profile.data.user
                    } else {
                        _error.value = profile.message
                    }
                },
                onFailure = { error -> _error.value = error.message }
            )

            // Load 2FA status
            val twoFactorResult = userSecurityRepository.check2fa()
            twoFactorResult.fold(
                onSuccess = { response ->
                    _twoFactorEnabled.value = response.status && response.data.twoFactorEnabled
                },
                onFailure = { _twoFactorEnabled.value = false }
            )

            // Load sessions
            val sessionsResult = userSecurityRepository.getSessions()
            sessionsResult.fold(
                onSuccess = { paginated -> _sessions.value = paginated.data.results },
                onFailure = { error -> _error.value = error.message }
            )

            _loading.value = false
        }
    }

    // Helper for profile updates with snackbar
    private suspend fun executeProfileUpdateWithSnackbar(
        action: suspend () -> Result<UserUpdateResponse>,
        successMessage: String,
        errorMessage: String = "Update failed"
    ): Boolean {
        return try {
            val result = action()
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        loadSettings() // refresh profile
                        postSnackbar(SnackbarMessage.Success(successMessage))
                        true
                    } else {
                        postSnackbar(SnackbarMessage.Error(response.message ?: errorMessage))
                        false
                    }
                },
                onFailure = { error ->
                    postSnackbar(SnackbarMessage.Error(error.message ?: errorMessage))
                    false
                }
            )
        } catch (e: Exception) {
            postSnackbar(SnackbarMessage.Error(errorMessage))
            false
        }
    }

    // Specific update methods with snackbar
    fun updateFirstName(firstName: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateFirstName(UpdateFirstNameUpdateFirstNameInputRequest(firstName = firstName)) },
                successMessage = "First name updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("First name updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updateLastName(lastName: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateLastName(UpdateLastNameUpdateLastNameInputRequest(lastName = lastName)) },
                successMessage = "Last name updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Last name updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updateUsername(newUsername: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateUsername(UpdateUsernameInputRequest(username = newUsername)) },
                successMessage = "Username updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Username updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updateEmail(newEmail: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateEmail(UpdateEmailInputRequest(email = newEmail)) },
                successMessage = "Email updated. Please verify your new email."
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Email updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updateBio(bio: String?, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateBio(UpdateBioUpdateBioInputRequest(bio = bio)) },
                successMessage = "Bio updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Bio updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updateLocation(location: String?, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateLocation(UpdateLocationUpdateLocationInputRequest(location = location)) },
                successMessage = "Location updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Location updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updatePhoneNumber(phoneNumber: String?, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updatePhoneNumber(UpdatePhoneNumberUpdatePhoneNumberInputRequest(phoneNumber = phoneNumber)) },
                successMessage = "Phone number updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Phone number updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    fun updateDateOfBirth(dateOfBirth: LocalDate?, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val dateString = dateOfBirth?.toString()
            val success = executeProfileUpdateWithSnackbar(
                action = { userProfileRepository.updateDateOfBirth(UpdateDateOfBirthUpdateDateOfBirthInputRequest(dateOfBirth = dateOfBirth)) },
                successMessage = "Date of birth updated successfully"
            )
            _profileUpdateState.value = if (success) ProfileUpdateState.Success("Date of birth updated") else ProfileUpdateState.Error("Failed")
            callback(success, if (success) null else "Failed")
        }
    }

    // Generic updateProfileField (calls specific methods with snackbar)
    fun updateProfileField(fieldName: String, newValue: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        when (fieldName) {
            "first_name" -> updateFirstName(newValue, callback)
            "last_name" -> updateLastName(newValue, callback)
            "username" -> updateUsername(newValue, callback)
            "email" -> updateEmail(newValue, callback)
            "bio" -> updateBio(newValue, callback)
            "location" -> updateLocation(newValue, callback)
            "phone" -> updatePhoneNumber(newValue, callback)
            "date_of_birth" -> {
                val date = try { LocalDate.parse(newValue) } catch (e: Exception) { null }
                updateDateOfBirth(date, callback)
            }
            else -> {
                postSnackbar(SnackbarMessage.Error("Unknown field: $fieldName"))
                callback(false, "Unknown field")
            }
        }
    }

    // Change password with snackbar
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword != confirmPassword) {
            postSnackbar(SnackbarMessage.Error("Passwords do not match"))
            _passwordChangeState.value = PasswordChangeState.Error("Passwords do not match")
            return
        }
        if (newPassword.length < 8) {
            postSnackbar(SnackbarMessage.Error("Password must be at least 8 characters"))
            _passwordChangeState.value = PasswordChangeState.Error("Password must be at least 8 characters")
            return
        }

        viewModelScope.launch {
            _passwordChangeState.value = PasswordChangeState.Loading
            val request = PasswordChangeRequestRequest(currentPassword, newPassword, confirmPassword)
            val result = passwordResetRepository.changePassword(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        _passwordChangeState.value = PasswordChangeState.Success("Password changed successfully")
                        postSnackbar(SnackbarMessage.Success("Password changed successfully"))
                    } else {
                        _passwordChangeState.value = PasswordChangeState.Error(response.message ?: "Failed")
                        postSnackbar(SnackbarMessage.Error(response.message ?: "Failed to change password"))
                    }
                },
                onFailure = { error ->
                    _passwordChangeState.value = PasswordChangeState.Error(error.message ?: "Network error")
                    postSnackbar(SnackbarMessage.Error(error.message ?: "Failed to change password"))
                }
            )
        }
    }

    // Enable 2FA with snackbar
    fun enable2FA(otpCode: String) {
        viewModelScope.launch {
            _twoFactorState.value = TwoFactorState.Loading
            val request = EnableTwoFactorRequest(otpCode = otpCode)
            val result = userSecurityRepository.enable2fa(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        _twoFactorEnabled.value = response.data.twoFactorEnabled ?: true
                        _twoFactorState.value = TwoFactorState.Success(true)
                        postSnackbar(SnackbarMessage.Success("2FA enabled successfully"))
                    } else {
                        _twoFactorEnabled.value = false
                        _twoFactorState.value = TwoFactorState.Error(response.message)
                        postSnackbar(SnackbarMessage.Error(response.message))
                    }
                },
                onFailure = { error ->
                    _twoFactorState.value = TwoFactorState.Error(error.message ?: "Failed to enable 2FA")
                    postSnackbar(SnackbarMessage.Error(error.message ?: "Failed to enable 2FA"))
                }
            )
        }
    }

    // Disable 2FA with snackbar
    fun disable2FA(currentPassword: String) {
        viewModelScope.launch {
            _twoFactorState.value = TwoFactorState.Loading
            val request = DisableTwoFactorRequest(currentPassword = currentPassword)
            val result = userSecurityRepository.disable2fa(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        _twoFactorEnabled.value = response.data.twoFactorEnabled
                        _twoFactorState.value = TwoFactorState.Success(false)
                        postSnackbar(SnackbarMessage.Success("2FA disabled successfully"))
                    } else {
                        _twoFactorEnabled.value = false
                        _twoFactorState.value = TwoFactorState.Error(response.message)
                        postSnackbar(SnackbarMessage.Error(response.message))
                    }
                },
                onFailure = { error ->
                    _twoFactorState.value = TwoFactorState.Error(error.message ?: "Failed to disable 2FA")
                    postSnackbar(SnackbarMessage.Error(error.message ?: "Failed to disable 2FA"))
                }
            )
        }
    }

    // Terminate session with snackbar
    fun terminateSession(sessionId: UUID?) {
        if (sessionId == null) {
            postSnackbar(SnackbarMessage.Error("Invalid session"))
            return
        }
        viewModelScope.launch {
            _sessionActionState.value = SessionActionState.Loading
            val request = TerminateSessionRequest(sessionId)
            val result = userSecurityRepository.terminateSession(request)
            result.fold(
                onSuccess = {
                    loadSettings()
                    _sessionActionState.value = SessionActionState.Success("Session terminated")
                    postSnackbar(SnackbarMessage.Success("Session terminated"))
                },
                onFailure = { error ->
                    _sessionActionState.value = SessionActionState.Error(error.message ?: "Failed")
                    postSnackbar(SnackbarMessage.Error(error.message ?: "Failed to terminate session"))
                }
            )
        }
    }

    // Terminate all other sessions with snackbar
    fun terminateAllOtherSessions() {
        viewModelScope.launch {
            _sessionActionState.value = SessionActionState.Loading
            val result = userSecurityRepository.terminateAllSessions()
            result.fold(
                onSuccess = {
                    loadSettings()
                    _sessionActionState.value = SessionActionState.Success("All other sessions terminated")
                    postSnackbar(SnackbarMessage.Success("All other sessions terminated"))
                },
                onFailure = { error ->
                    _sessionActionState.value = SessionActionState.Error(error.message ?: "Failed")
                    postSnackbar(SnackbarMessage.Error(error.message ?: "Failed to terminate sessions"))
                }
            )
        }
    }

    // Deactivate account with snackbar
    fun deactivateAccount(password: String, confirm: Boolean) {
        if (!confirm) {
            postSnackbar(SnackbarMessage.Error("Please confirm deactivation"))
            _deactivationState.value = AccountDeactivationState.Error("Please confirm deactivation")
            return
        }
        viewModelScope.launch {
            _deactivationState.value = AccountDeactivationState.Loading
            val request = UserDeactivateInputRequest(password, confirm)
            val result = userProfileRepository.deactivate(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        _deactivationState.value = AccountDeactivationState.Success("Account deactivated")
                        postSnackbar(SnackbarMessage.Success("Account deactivated"))
                    } else {
                        _deactivationState.value = AccountDeactivationState.Error(response.message ?: "Failed")
                        postSnackbar(SnackbarMessage.Error(response.message ?: "Failed to deactivate account"))
                    }
                },
                onFailure = { error ->
                    _deactivationState.value = AccountDeactivationState.Error(error.message ?: "Network error")
                    postSnackbar(SnackbarMessage.Error(error.message ?: "Failed to deactivate account"))
                }
            )
        }
    }

    // Logout with snackbar
    fun logout(context: Context) {
        viewModelScope.launch {
            val refresh = AuthManager.getRefreshToken(context)
            if (refresh != null) {
                _logoutState.value = LogoutState.Loading
                val result = logOutRepository.logout(LogoutRequestRequest(refresh = refresh))
                result.fold(
                    onSuccess = { response ->
                        if (response.status) {
                            TokenManager.clearAll(context)
                            AuthManager.clearTokens(context)
                            _logoutState.value = LogoutState.Success("Logged out successfully")
                            postSnackbar(SnackbarMessage.Success("Logged out successfully"))
                        } else {
                            _logoutState.value = LogoutState.Error(response.message)
                            postSnackbar(SnackbarMessage.Error(response.message))
                        }
                    },
                    onFailure = { error ->
                        _logoutState.value = LogoutState.Error(error.message ?: "Logout failed")
                        postSnackbar(SnackbarMessage.Error(error.message ?: "Logout failed"))
                    }
                )
            } else {
                _logoutState.value = LogoutState.Error("No active session")
                postSnackbar(SnackbarMessage.Error("No active session"))
            }
        }
    }

    // Clear functions
    fun clearLogoutState() {
        _logoutState.value = LogoutState.Idle
    }

    fun clearPasswordState() {
        _passwordChangeState.value = PasswordChangeState.Idle
    }

    fun clearTwoFactorState() {
        _twoFactorState.value = TwoFactorState.Idle
    }

    fun clearSessionActionState() {
        _sessionActionState.value = SessionActionState.Idle
    }

    fun clearDeactivationState() {
        _deactivationState.value = AccountDeactivationState.Idle
    }

    fun clearProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }

    fun clearError() {
        _error.value = null
    }
}
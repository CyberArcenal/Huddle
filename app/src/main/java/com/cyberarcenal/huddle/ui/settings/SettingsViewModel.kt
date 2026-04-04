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
import java.util.UUID

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

class SettingsViewModel(
    private val userProfileRepository: UsersRepository,
    private val userSecurityRepository: UserSecurityRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val logOutRepository: LogOutRepository,             // new
) : ViewModel() {

    // User profile
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Security settings
    private val _securitySettings = MutableStateFlow<UpdateSecuritySettingsRequest?>(null)
    val securitySettings: StateFlow<UpdateSecuritySettingsRequest?> =
        _securitySettings.asStateFlow()

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
    private val _passwordChangeState =
        MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    // 2FA state
    private val _twoFactorState = MutableStateFlow<TwoFactorState>(TwoFactorState.Idle)
    val twoFactorState: StateFlow<TwoFactorState> = _twoFactorState.asStateFlow()

    // Session action state
    private val _sessionActionState = MutableStateFlow<SessionActionState>(SessionActionState.Idle)
    val sessionActionState: StateFlow<SessionActionState> = _sessionActionState.asStateFlow()

    // Account deactivation state
    private val _deactivationState =
        MutableStateFlow<AccountDeactivationState>(AccountDeactivationState.Idle)
    val deactivationState: StateFlow<AccountDeactivationState> = _deactivationState.asStateFlow()
    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    
    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // Load user profile using userProfileRepository
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

            // Load security settings using userSecurityRepository
            val settingsResult = userSecurityRepository.getSecuritySettings()
            settingsResult.fold(
                onSuccess = { settings ->
                    // We need to cast appropriately – for simplicity, we'll ignore for now.
                    // In practice, you'd need a proper model. We'll use placeholder.
                },
                onFailure = { /* ignore */ }
            )

            // Load 2FA status using userSecurityRepository
            val twoFactorResult = userSecurityRepository.check2fa()
            twoFactorResult.fold(
                onSuccess = { response ->
                    if (response.status) {
                        _twoFactorEnabled.value = response.data.twoFactorEnabled
                    } else {
                        _twoFactorEnabled.value = false
                    }
                },
                onFailure = { /* ignore */ }
            )

            // Load sessions using userSecurityRepository
            val sessionsResult = userSecurityRepository.getSessions()
            sessionsResult.fold(
                onSuccess = { paginated -> _sessions.value = paginated.data.results },
                onFailure = { error -> _error.value = error.message }
            )

            _loading.value = false
        }
    }

    fun updateSecuritySettings(
        recoveryEmail: String?,
        recoveryPhone: String?,
        alertOnNewDevice: Boolean?,
        alertOnPasswordChange: Boolean?,
        alertOnFailedLogin: Boolean?
    ) {
        val settings = UpdateSecuritySettingsRequest(
            recoveryEmail = recoveryEmail,
            recoveryPhone = recoveryPhone,
            alertOnNewDevice = alertOnNewDevice,
            alertOnPasswordChange = alertOnPasswordChange,
            alertOnFailedLogin = alertOnFailedLogin
        )
        viewModelScope.launch {
            _loading.value = true
            val result = userSecurityRepository.updateSecuritySettings(settings)
            result.fold(
                onSuccess = { response ->
                    // response.settings is Any, we can ignore
                    _loading.value = false
                },
                onFailure = { error ->
                    _error.value = error.message
                    _loading.value = false
                }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword != confirmPassword) {
            _passwordChangeState.value = PasswordChangeState.Error("Passwords do not match")
            return
        }
        if (newPassword.length < 8) {
            _passwordChangeState.value =
                PasswordChangeState.Error("Password must be at least 8 characters")
            return
        }

        viewModelScope.launch {
            _passwordChangeState.value = PasswordChangeState.Loading
            // Use authRepository for password change
            val request =
                PasswordChangeRequestRequest(currentPassword, newPassword, confirmPassword)
            val result = passwordResetRepository.changePassword(request)
            result.fold(
                onSuccess = { response ->
                    _passwordChangeState.value =
                        PasswordChangeState.Success("Password changed successfully")
                },
                onFailure = { error ->
                    _passwordChangeState.value =
                        PasswordChangeState.Error(error.message ?: "Failed to change password")
                }
            )
        }
    }

    fun enable2FA(otpCode: String) {
        viewModelScope.launch {
            _twoFactorState.value = TwoFactorState.Loading
            val request = EnableTwoFactorRequest(otpCode = otpCode)
            val result = userSecurityRepository.enable2fa(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status){
                        val data = response.data
                        _twoFactorEnabled.value = data.twoFactorEnabled ?: true
                        _twoFactorState.value = TwoFactorState.Success(true)
                    }else{
                        _twoFactorEnabled.value = false;
                        _twoFactorState.value = TwoFactorState.Error(response.message)
                    }

                },
                onFailure = { error ->
                    _twoFactorState.value =
                        TwoFactorState.Error(error.message ?: "Failed to enable 2FA")
                }
            )
        }
    }

    fun disable2FA(currentPassword: String) {
        viewModelScope.launch {
            _twoFactorState.value = TwoFactorState.Loading
            val request = DisableTwoFactorRequest(currentPassword = currentPassword)
            val result = userSecurityRepository.disable2fa(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status){
                        _twoFactorEnabled.value = response.data.twoFactorEnabled
                        _twoFactorState.value = TwoFactorState.Success(false)
                    }else{
                        _twoFactorEnabled.value = false;
                        _twoFactorState.value = TwoFactorState.Error(response.message)
                    }

                },
                onFailure = { error ->
                    _twoFactorState.value =
                        TwoFactorState.Error(error.message ?: "Failed to disable 2FA")
                }
            )
        }
    }

    fun terminateSession(sessionId: UUID?) {
        if (sessionId == null) return
        viewModelScope.launch {
            _sessionActionState.value = SessionActionState.Loading
            val request = TerminateSessionRequest(sessionId)
            val result = userSecurityRepository.terminateSession(request)
            result.fold(
                onSuccess = { response ->
                    // Refresh sessions
                    loadSettings()
                    _sessionActionState.value = SessionActionState.Success("Session terminated")
                },
                onFailure = { error ->
                    _sessionActionState.value =
                        SessionActionState.Error(error.message ?: "Failed to terminate session")
                }
            )
        }
    }

    fun terminateAllOtherSessions() {
        viewModelScope.launch {
            _sessionActionState.value = SessionActionState.Loading
            val result = userSecurityRepository.terminateAllSessions()
            result.fold(
                onSuccess = { response ->
                    loadSettings()
                    _sessionActionState.value =
                        SessionActionState.Success("All other sessions terminated")
                },
                onFailure = { error ->
                    _sessionActionState.value =
                        SessionActionState.Error(error.message ?: "Failed to terminate sessions")
                }
            )
        }
    }

    // Update username
    fun updateUsername(newUsername: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val request = UserProfileSchemaUpdateRequest(username = newUsername)
            val result = userProfileRepository.updateProfile(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        // Refresh profile after update
                        loadSettings()
                        callback(true, null)
                    } else {
                        callback(false, response.message)
                    }
                },
                onFailure = { error ->
                    callback(false, error.message)
                }
            )
        }
    }

    // Update email
    fun updateEmail(newEmail: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val request = UserProfileSchemaUpdateRequest(email = newEmail)
            val result = userProfileRepository.updateProfile(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        loadSettings()
                        callback(true, null)
                    } else {
                        callback(false, response.message)
                    }
                },
                onFailure = { error ->
                    callback(false, error.message)
                }
            )
        }
    }

    // Generic field update
    fun updateProfileField(fieldName: String, newValue: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val request = when (fieldName) {
                "first_name" -> UserProfileSchemaUpdateRequest(firstName = newValue)
                "last_name" -> UserProfileSchemaUpdateRequest(lastName = newValue)
                "phone" -> UserProfileSchemaUpdateRequest(phoneNumber = newValue)
                "bio" -> UserProfileSchemaUpdateRequest(bio = newValue)
                "location" -> UserProfileSchemaUpdateRequest(location = newValue)
                "date_of_birth" -> {
                    // Parse date string to LocalDate
                    val date = try {
                        java.time.LocalDate.parse(newValue)
                    } catch (e: Exception) {
                        null
                    }
                    UserProfileSchemaUpdateRequest(dateOfBirth = date)
                }
                else -> {
                    callback(false, "Unknown field")
                    return@launch
                }
            }
            val result = userProfileRepository.updateProfile(request)
            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        loadSettings()
                        callback(true, null)
                    } else {
                        callback(false, response.message)
                    }
                },
                onFailure = { error ->
                    callback(false, error.message)
                }
            )
        }
    }

    fun deactivateAccount(password: String, confirm: Boolean) {
        if (!confirm) {
            _deactivationState.value = AccountDeactivationState.Error("Please confirm deactivation")
            return
        }
        viewModelScope.launch {
            _deactivationState.value = AccountDeactivationState.Loading
            val request = UserDeactivateInputRequest(password, confirm)
            val result = userProfileRepository.deactivate(request)
            result.fold(
                onSuccess = { response ->
                    _deactivationState.value =
                        AccountDeactivationState.Success("Account deactivated")
                    // Navigate to login after a delay
                },
                onFailure = { error ->
                    _deactivationState.value = AccountDeactivationState.Error(
                        error.message ?: "Failed to deactivate account"
                    )
                }
            )
        }
    }

    fun logout(context: Context) {
        try {
            viewModelScope.launch {
                val refresh = AuthManager.getRefreshToken(context);
                refresh?.let {
                    _logoutState.value = LogoutState.Loading
                    val result = logOutRepository.logout(
                        request = LogoutRequestRequest(
                            refresh = it
                        )
                    )  // kailangan mong i‑implement ito
                    result.fold(
                        onSuccess = { response ->
                            if (response.status){
                                // I‑clear ang local data
                                TokenManager.clearAll(context)
                                AuthManager.clearTokens(context)
                                _logoutState.value = LogoutState.Success("Logged out successfully")
                            }else{
                                _logoutState.value = LogoutState.Error(response.message)
                            }

                        },
                        onFailure = { error ->
                            _logoutState.value = LogoutState.Error(error.message ?: "Logout failed")
                        }
                    )
                }

            }
        }catch (e: Exception){
            _logoutState.value = LogoutState.Error("Logout failed")
        }

    }

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

    fun clearError() {
        _error.value = null
    }
}
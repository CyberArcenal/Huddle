package com.cyberarcenal.huddle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.users.UsersRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    private val usersRepository: UsersRepository
) : ViewModel() {

    // User profile
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Security settings
    private val _securitySettings = MutableStateFlow<UpdateSecuritySettings?>(null)
    val securitySettings: StateFlow<UpdateSecuritySettings?> = _securitySettings.asStateFlow()

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

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // Load user profile
            val profileResult = usersRepository.getCurrentUserProfile()
            profileResult.fold(
                onSuccess = { profile -> _userProfile.value = profile },
                onFailure = { error -> _error.value = error.message }
            )

            // Load security settings (the API returns Any, we need to parse)
            val settingsResult = usersRepository.getSecuritySettings()
            settingsResult.fold(
                onSuccess = { settings ->
                    // We need to cast appropriately – for simplicity, we'll ignore for now.
                    // In practice, you'd need a proper model. We'll use placeholder.
                },
                onFailure = { /* ignore */ }
            )

            // Load 2FA status
            val twoFactorResult = usersRepository.check2FA()
            twoFactorResult.fold(
                onSuccess = { response -> _twoFactorEnabled.value = response.twoFactorEnabled ?: false },
                onFailure = { /* ignore */ }
            )

            // Load sessions
            val sessionsResult = usersRepository.getLoginSessions()
            sessionsResult.fold(
                onSuccess = { paginated -> _sessions.value = paginated.results },
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
        val settings = UpdateSecuritySettings(
            recoveryEmail = recoveryEmail,
            recoveryPhone = recoveryPhone,
            alertOnNewDevice = alertOnNewDevice,
            alertOnPasswordChange = alertOnPasswordChange,
            alertOnFailedLogin = alertOnFailedLogin
        )
        viewModelScope.launch {
            _loading.value = true
            val result = usersRepository.updateSecuritySettings(settings)
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
            _passwordChangeState.value = PasswordChangeState.Error("Password must be at least 8 characters")
            return
        }

        viewModelScope.launch {
            _passwordChangeState.value = PasswordChangeState.Loading
            val request = ChangePassword(
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            val result = usersRepository.changePassword(request)
            result.fold(
                onSuccess = { response ->
                    _passwordChangeState.value = PasswordChangeState.Success("Password changed successfully")
                },
                onFailure = { error ->
                    _passwordChangeState.value = PasswordChangeState.Error(error.message ?: "Failed to change password")
                }
            )
        }
    }

    fun enable2FA(otpCode: String) {
        viewModelScope.launch {
            _twoFactorState.value = TwoFactorState.Loading
            val request = EnableTwoFactor(otpCode = otpCode)
            val result = usersRepository.enable2FA(request)
            result.fold(
                onSuccess = { response ->
                    _twoFactorEnabled.value = response.twoFactorEnabled ?: true
                    _twoFactorState.value = TwoFactorState.Success(true)
                },
                onFailure = { error ->
                    _twoFactorState.value = TwoFactorState.Error(error.message ?: "Failed to enable 2FA")
                }
            )
        }
    }

    fun disable2FA(currentPassword: String) {
        viewModelScope.launch {
            _twoFactorState.value = TwoFactorState.Loading
            val request = DisableTwoFactor(currentPassword = currentPassword)
            val result = usersRepository.disable2FA(request)
            result.fold(
                onSuccess = { response ->
                    _twoFactorEnabled.value = response.twoFactorEnabled ?: false
                    _twoFactorState.value = TwoFactorState.Success(false)
                },
                onFailure = { error ->
                    _twoFactorState.value = TwoFactorState.Error(error.message ?: "Failed to disable 2FA")
                }
            )
        }
    }

    fun terminateSession(sessionId: java.util.UUID) {
        viewModelScope.launch {
            _sessionActionState.value = SessionActionState.Loading
            val result = usersRepository.terminateSession(sessionId)
            result.fold(
                onSuccess = { response ->
                    // Refresh sessions
                    loadSettings()
                    _sessionActionState.value = SessionActionState.Success("Session terminated")
                },
                onFailure = { error ->
                    _sessionActionState.value = SessionActionState.Error(error.message ?: "Failed to terminate session")
                }
            )
        }
    }

    fun terminateAllOtherSessions() {
        viewModelScope.launch {
            _sessionActionState.value = SessionActionState.Loading
            val result = usersRepository.terminateAllSessions()
            result.fold(
                onSuccess = { response ->
                    loadSettings()
                    _sessionActionState.value = SessionActionState.Success("All other sessions terminated")
                },
                onFailure = { error ->
                    _sessionActionState.value = SessionActionState.Error(error.message ?: "Failed to terminate sessions")
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
            val result = usersRepository.deactivateAccount(password, confirm)
            result.fold(
                onSuccess = { response ->
                    _deactivationState.value = AccountDeactivationState.Success("Account deactivated")
                    // Navigate to login after a delay
                },
                onFailure = { error ->
                    _deactivationState.value = AccountDeactivationState.Error(error.message ?: "Failed to deactivate account")
                }
            )
        }
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
package com.cyberarcenal.huddle.ui.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.PasswordResetCompleteRequestRequest
import com.cyberarcenal.huddle.api.models.PasswordResetRequestRequest
import com.cyberarcenal.huddle.api.models.PasswordResetVerifyRequestRequest
import com.cyberarcenal.huddle.data.repositories.PasswordRecoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val repository: PasswordRecoveryRepository = PasswordRecoveryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onOtpChange(otp: String) {
        _uiState.update { it.copy(otp = otp, error = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, error = null) }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, error = null) }
    }

    fun requestOtp() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Mangyaring ilagay ang iyong email.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.requestReset(PasswordResetRequestRequest(email = email))
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, step = ForgotPasswordStep.VerifyOtp) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to send OTP") }
                }
            )
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.otp.length < 4) {
            _uiState.update { it.copy(error = "Invalid OTP code.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.verifyReset(
                PasswordResetVerifyRequestRequest(email = state.email, otpCode = state.otp)
            )
            result.fold(
                onSuccess = { response ->
                    _uiState.update { it.copy(
                        isLoading = false, 
                        step = ForgotPasswordStep.ResetPassword,
                        checkpointToken = response.data.checkpointToken 
                    ) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Invalid OTP") }
                }
            )
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters.") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match.") }
            return
        }

        val token = state.checkpointToken ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.completeReset(
                PasswordResetCompleteRequestRequest(
                    checkpointToken = token,
                    newPassword = state.newPassword,
                    confirmPassword = state.confirmPassword
                )
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to reset password") }
                }
            )
        }
    }

    fun backToEmail() {
        _uiState.update { it.copy(step = ForgotPasswordStep.RequestEmail, error = null) }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val otp: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val checkpointToken: String? = null,
    val step: ForgotPasswordStep = ForgotPasswordStep.RequestEmail,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

enum class ForgotPasswordStep {
    RequestEmail,
    VerifyOtp,
    ResetPassword
}
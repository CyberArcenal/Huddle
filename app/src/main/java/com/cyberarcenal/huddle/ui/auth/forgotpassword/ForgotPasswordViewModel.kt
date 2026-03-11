package com.cyberarcenal.huddle.ui.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.data.repositories.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    // Step 1: Request reset
    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onRequestResetClick() {
        val currentState = _uiState.value
        if (currentState.email.isBlank()) {
            _uiState.value = currentState.copy(error = "Email is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            val result = authRepository.requestPasswordReset(currentState.email)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        step = ForgotPasswordStep.VerifyOtp,
                        error = null,
                        message = response.message ?: "Reset code sent to your email"
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to send reset code"
                    )
                }
            )
        }
    }

    // Step 2: Verify OTP
    fun onOtpChange(otp: String) {
        _uiState.value = _uiState.value.copy(otp = otp)
    }

    fun onVerifyOtpClick() {
        val currentState = _uiState.value
        if (currentState.otp.length != 6) {
            _uiState.value = currentState.copy(error = "OTP must be 6 digits")
            return
        }
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            val result = authRepository.verifyPasswordReset(email = currentState.email, otpCode = currentState.otp)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        step = ForgotPasswordStep.ResetPassword,
                        checkpointToken = response.checkpointToken,
                        error = null,
                        message = response.message
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = error.message ?: "Invalid OTP"
                    )
                }
            )
        }
    }

    // Step 3: Reset password
    fun onNewPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(newPassword = newPassword)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }

    fun onResetPasswordClick() {
        val currentState = _uiState.value
        if (currentState.newPassword.isBlank() || currentState.confirmPassword.isBlank()) {
            _uiState.value = currentState.copy(error = "All fields are required")
            return
        }
        if (currentState.newPassword != currentState.confirmPassword) {
            _uiState.value = currentState.copy(error = "Passwords do not match")
            return
        }
        if (currentState.checkpointToken.isNullOrBlank()) {
            _uiState.value = currentState.copy(error = "Missing checkpoint token")
            return
        }
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            val result = authRepository.completePasswordReset(currentState.checkpointToken, currentState.newPassword)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        resetSuccess = true,
                        message = response.message
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = error.message ?: "Password reset failed"
                    )
                }
            )
        }
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(step = ForgotPasswordStep.Request)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(resetSuccess = false)
    }
}

enum class ForgotPasswordStep {
    Request,
    VerifyOtp,
    ResetPassword
}

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.Request,
    val email: String = "",
    val otp: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val checkpointToken: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val resetSuccess: Boolean = false
)
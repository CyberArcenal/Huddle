package com.cyberarcenal.huddle.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.ResendRequest
import com.cyberarcenal.huddle.api.models.UserRegisterRequest
import com.cyberarcenal.huddle.api.models.VerifyEmailRequest
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val usersRepository: UsersRepository = UsersRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(firstName: String) = _uiState.update { it.copy(firstName = firstName) }
    fun onLastNameChange(lastName: String) = _uiState.update { it.copy(lastName = lastName) }
    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }
    fun onPhoneNumberChange(phoneNumber: String) = _uiState.update { it.copy(phoneNumber = phoneNumber) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password) }
    fun onConfirmPasswordChange(confirmPassword: String) = _uiState.update { it.copy(confirmPassword = confirmPassword) }
    fun onOtpChange(otp: String) = _uiState.update { it.copy(otp = otp) }

    fun nextStep() {
        val currentState = _uiState.value
        when (currentState.currentStep) {
            0 -> {
                if (currentState.firstName.isBlank() || currentState.lastName.isBlank() || currentState.email.isBlank()) {
                    _uiState.update { it.copy(error = "Please fill in all required fields") }
                    return
                }
                _uiState.update { it.copy(currentStep = 1, error = null) }
            }
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1, error = null) }
        }
    }

    fun onRegisterSubmit() {
        val currentState = _uiState.value
        if (currentState.password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Auto-generate username from email for registration requirement
            val username = currentState.email.substringBefore("@")

            val request = UserRegisterRequest(
                username = username,
                email = currentState.email,
                password = currentState.password,
                confirmPassword = currentState.confirmPassword,
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                phoneNumber = currentState.phoneNumber.ifBlank { null }
            )

            val result = usersRepository.register(request)
            result.fold(
                onSuccess = { responseMap ->
                    // Backend returns Map<String, Any> with "user_id"
                    val userId = (responseMap["user_id"] as? Double)?.toInt() 
                        ?: (responseMap["user_id"] as? Int)
                    
                    if (userId != null) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                currentStep = 2, 
                                userId = userId,
                                error = null 
                            ) 
                        }
                    } else {
                        // Handle case where email exists but inactive (Backend returns 200 instead of 201)
                        // If user_id is missing, we might need to handle it differently or request via email
                        _uiState.update { it.copy(isLoading = false, error = "Unexpected server response") }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Registration failed") }
                }
            )
        }
    }

    fun onVerifyOtp() {
        val currentState = _uiState.value
        if (currentState.otp.length != 6) {
            _uiState.update { it.copy(error = "Please enter 6-digit OTP") }
            return
        }

        viewModelScope.launch {
            val userId = currentState.userId ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }

            val verifyRequest = VerifyEmailRequest(
                userId = userId,
                otpCode = currentState.otp
            )

            val result = usersRepository.verifyEmail(verifyRequest)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Verification failed") }
                }
            )
        }
    }

    fun resendOtp() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val resendRequest = ResendRequest(email = currentState.email)
            val result = usersRepository.resendEmailVerification(resendRequest)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = "New OTP sent to your email") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to resend OTP") }
                }
            )
        }
    }

    fun resetSuccess() = _uiState.update { it.copy(registrationSuccess = false) }
}

data class RegisterUiState(
    val currentStep: Int = 0, // 0: Personal, 1: Security, 2: OTP
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otp: String = "",
    val userId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val registrationSuccess: Boolean = false
)

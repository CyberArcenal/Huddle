package com.cyberarcenal.huddle.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.ResendRequest
import com.cyberarcenal.huddle.api.models.UserRegisterRequest
import com.cyberarcenal.huddle.api.models.VerifyEmailRequest
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
                    _uiState.update { it.copy(error = "Please enter a valid email address") }
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

            try {
                val result = usersRepository.register(request)
                result.fold(
                    onSuccess = { responseMap ->
                        // Backend returns Map<String, Any> with "user_id"

                        if (responseMap.status) {
                            val userId = responseMap.data?.userId
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentStep = 2,
                                    userId = userId,
                                    error = null
                                )
                            }
                        } else {
                            // Maybe the account already exists but is inactive? Backend might not return user_id.
                            // Check if there's a message in response.
                            val message = responseMap.message
                            _uiState.update { it.copy(isLoading = false, error = mapRegistrationErrorToUserMessage(message)) }
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = getErrorMessage(error)
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
                )
            } catch (e: Exception) {
                val errorMessage = getErrorMessage(e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    fun onVerifyOtp() {
        val currentState = _uiState.value
        if (currentState.otp.length != 6) {
            _uiState.update { it.copy(error = "Please enter a 6-digit verification code") }
            return
        }

        viewModelScope.launch {
            val userId = currentState.userId ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }

            val verifyRequest = VerifyEmailRequest(
                userId = userId,
                otpCode = currentState.otp
            )

            try {
                val result = usersRepository.verifyEmail(verifyRequest)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
                    },
                    onFailure = { error ->
                        val errorMessage = getErrorMessage(error)
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
                )
            } catch (e: Exception) {
                val errorMessage = getErrorMessage(e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    fun resendOtp() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val resendRequest = ResendRequest(email = currentState.email)
            try {
                val result = usersRepository.resendEmailVerification(resendRequest)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, error = null) }
                        // Show success message as a temporary one? Maybe use snackbar.
                        // For now, we set a success message that will be cleared later.
                        _uiState.update { it.copy(successMessage = "New verification code sent to your email") }
                    },
                    onFailure = { error ->
                        val errorMessage = getErrorMessage(error)
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
                )
            } catch (e: Exception) {
                val errorMessage = getErrorMessage(e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun resetSuccess() = _uiState.update { it.copy(registrationSuccess = false) }

    private fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is ConnectException -> "Unable to connect to server. Check your internet connection."
            is SocketTimeoutException -> "Request timed out. Please try again."
            is UnknownHostException -> "No internet connection."
            is HttpException -> {
                try {
                    val errorBody = throwable.response()?.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        val json = Serializer.gson.fromJson(errorBody, Map::class.java)
                        // Sometimes the error is a single detail string, sometimes it's an object with field-specific errors.
                        // For registration, backend might return "detail" or "email" etc.
                        val detail = json["detail"] as? String
                        if (!detail.isNullOrBlank()) {
                            return mapRegistrationErrorToUserMessage(detail)
                        }
                        // Try to extract field-specific errors
                        val emailError = (json["email"] as? List<*>)?.firstOrNull() as? String
                        if (!emailError.isNullOrBlank()) {
                            return mapRegistrationErrorToUserMessage(emailError)
                        }
                        val passwordError = (json["password"] as? List<*>)?.firstOrNull() as? String
                        if (!passwordError.isNullOrBlank()) {
                            return mapRegistrationErrorToUserMessage(passwordError)
                        }
                        // If we can't parse, return generic message
                        return "Registration failed. Please check your information."
                    }
                } catch (e: Exception) {
                    // fall through
                }
                "Registration failed. Please try again."
            }
            else -> throwable.message?.let { mapRegistrationErrorToUserMessage(it) } ?: "An unexpected error occurred."
        }
    }

    private fun mapRegistrationErrorToUserMessage(backendMessage: String): String {
        return when {
            backendMessage.contains("already exists", ignoreCase = true) ||
                    backendMessage.contains("already taken", ignoreCase = true) ||
                    backendMessage.contains("already registered", ignoreCase = true) ->
                "This email is already registered. Please log in or reset your password."
            backendMessage.contains("email", ignoreCase = true) && backendMessage.contains("valid", ignoreCase = true) ->
                "Please enter a valid email address."
            backendMessage.contains("password", ignoreCase = true) && backendMessage.contains("length", ignoreCase = true) ->
                "Password must be at least 8 characters."
            backendMessage.contains("password", ignoreCase = true) && backendMessage.contains("match", ignoreCase = true) ->
                "Passwords do not match."
            backendMessage.contains("inactive", ignoreCase = true) ->
                "Your account is not yet activated. Please check your email to verify your account."
            backendMessage.contains("expired", ignoreCase = true) ->
                "Verification code expired. Request a new one."
            backendMessage.contains("invalid", ignoreCase = true) && backendMessage.contains("code", ignoreCase = true) ->
                "Invalid verification code. Please try again."
            backendMessage.contains("username", ignoreCase = true) && backendMessage.contains("taken", ignoreCase = true) ->
                "Username already taken."
            else -> backendMessage // fallback to original if no mapping
        }
    }
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
    val successMessage: String? = null,
    val registrationSuccess: Boolean = false
)
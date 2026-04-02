// LoginViewModel.kt
package com.cyberarcenal.huddle.ui.auth.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.LoginRequestRequest
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.LoginRepository
import com.cyberarcenal.huddle.data.repositories.utils.ApiException
import com.cyberarcenal.huddle.ui.feed.safeConvertTo
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.OffsetDateTime

// Data classes for success response
data class LoginResponse(
    val status: Boolean,
    val message: String,
    val requires2fa: Boolean = false,
    val checkpointToken: String? = null,
    val expiresIn: OffsetDateTime? = null,
    val user: UserProfile? = null,
    val refreshToken: String? = null,
    val accessToken: String? = null,
    val accessExpiresIn: Int? = null,
    val repExpiresIn: Int? = null,
)

// Simple error response – we only care about the message
data class LoginErrorResponse(
    val status: Boolean? = null,
    val message: String? = null
)

class LoginViewModel(
    private val authRepository: LoginRepository = LoginRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onLoginClick() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            val request = LoginRequestRequest(email = currentState.email, password = currentState.password)

            authRepository.login(request).fold(
                onSuccess = { responseString ->
                    // Parse successful response (could be 200 with tokens or 200 with 2FA)
                    val responseData = safeConvertTo<LoginResponse>(responseString, tag = "login response convert")
                    Log.d("Login", responseData.toString())
                    responseData?.let {
                        if (it.requires2fa) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                navigateTo2FA = true,
                                checkpointToken = it.checkpointToken,
                                error = null
                            )
                        } else {
                            val accessToken = it.accessToken
                            val refreshToken = it.refreshToken
                            val userProfile = it.user
                            if (accessToken != null && refreshToken != null && userProfile != null) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    navigateToHome = true,
                                    accessToken = accessToken,
                                    refreshToken = refreshToken,
                                    userProfile = userProfile,
                                    error = null
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Invalid response from server"
                                )
                            }
                        }
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Invalid response from server"
                        )
                    }
                },
                onFailure = { error ->
                    val errorMessage = getErrorMessage(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            )
        }
    }

    fun resetNavigation() {
        _uiState.value = _uiState.value.copy(
            navigateToHome = false,
            navigateTo2FA = false,
            checkpointToken = null,
            accessToken = null,
            refreshToken = null,
            userProfile = null
        )
    }

    private fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is ApiException -> {
                val errorBody = throwable.errorBody
                val code = throwable.httpCode

                // Log the raw error for debugging
                android.util.Log.e("LoginViewModel", "API error $code: $errorBody")

                // Try to extract the message from the JSON body
                val serverMessage = if (!errorBody.isNullOrBlank()) {
                    try {
                        val json = JsonParser.parseString(errorBody).asJsonObject
                        json.get("message")?.asString
                    } catch (e: Exception) {
                        null // Not JSON, we'll fallback
                    }
                } else {
                    null
                }

                // If we have a server message, use it directly (the backend already returns user‑friendly messages)
                // Otherwise fallback to a generic message based on status code.
                val rawMessage = serverMessage ?: when (code) {
                    400 -> "Invalid request. Please check your input."
                    401 -> "Invalid email or password."
                    403 -> "Access denied. Please check your credentials."
                    404 -> "No account found with this email/username."
                    500 -> "Server error. Please try again later."
                    else -> "Network error (HTTP $code). Please try again."
                }

                // Map any remaining backend jargon to user‑friendly text
                mapBackendErrorToUserMessage(rawMessage)
            }
            is SocketTimeoutException -> "Connection timeout. Please check your network."
            is UnknownHostException -> "No internet connection. Please check your network."
            is ConnectException -> "Unable to connect to the server. Please try again."
            else -> throwable.message?.let { mapBackendErrorToUserMessage(it) } ?: "An unexpected error occurred."
        }
    }

    private fun mapBackendErrorToUserMessage(backendMessage: String): String {
        return when {
            backendMessage.contains("No Account found", ignoreCase = true) ->
                "No account found with this email/username."
            backendMessage.contains("not yet activated", ignoreCase = true) ->
                "Your account is not yet activated. Please check your email to verify your account."
            backendMessage.contains("Account status", ignoreCase = true) ->
                "Your account is restricted or suspended. Please contact support."
            backendMessage.contains("Invalid credentials", ignoreCase = true) ->
                "Invalid email or password."
            backendMessage.contains("Invalid or expired OTP", ignoreCase = true) ->
                "Invalid or expired verification code."
            backendMessage.contains("email", ignoreCase = true) && backendMessage.contains("already", ignoreCase = true) ->
                "This email is already registered. Please use a different email or log in."
            backendMessage.contains("username", ignoreCase = true) && backendMessage.contains("already", ignoreCase = true) ->
                "This username is already taken. Please choose another."
            else -> backendMessage // Fallback to raw message
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToHome: Boolean = false,
    val navigateTo2FA: Boolean = false,
    val checkpointToken: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userProfile: UserProfile? = null
)
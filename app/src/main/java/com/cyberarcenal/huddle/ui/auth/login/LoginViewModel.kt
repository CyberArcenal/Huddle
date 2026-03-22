package com.cyberarcenal.huddle.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.LoginRequestRequest
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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

            try {
                val result = authRepository.login(request)

                result.fold(
                    onSuccess = { response ->
                        val checkpointToken = response["checkpoint_token"] as? String
                        if (checkpointToken != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                navigateTo2FA = true,
                                checkpointToken = checkpointToken,
                                error = null
                            )
                        } else {
                            val accessToken = response["accessToken"] as? String
                            val refreshToken = response["refreshToken"] as? String

                            val userRaw = response["user"]
                            val userProfile = try {
                                val json = Serializer.gson.toJson(userRaw)
                                Serializer.gson.fromJson(json, UserProfile::class.java)
                            } catch (e: Exception) {
                                null
                            }

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
                    },
                    onFailure = { error ->
                        val errorMessage = getErrorMessage(error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMessage = getErrorMessage(e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
            }
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
            is ConnectException -> "Unable to connect to server. Check your internet connection."
            is SocketTimeoutException -> "Request timed out. Please try again."
            is UnknownHostException -> "No internet connection."
            is HttpException -> {
                // Try to extract the "detail" field from the error response body
                try {
                    val errorBody = throwable.response()?.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        val json = Serializer.gson.fromJson(errorBody, Map::class.java)
                        val detail = json["detail"] as? String
                        if (!detail.isNullOrBlank()) {
                            return mapBackendErrorToUserMessage(detail)
                        }
                    }
                } catch (e: Exception) {
                    // fall through
                }
                "Login failed. Please try again."
            }
            else -> {
                // Fallback: try to get message from throwable
                throwable.message?.let { mapBackendErrorToUserMessage(it) } ?: "An unexpected error occurred."
            }
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
            else -> backendMessage // If no mapping, show original (but could be generic)
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
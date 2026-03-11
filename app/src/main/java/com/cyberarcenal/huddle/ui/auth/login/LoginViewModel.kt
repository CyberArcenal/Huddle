package com.cyberarcenal.huddle.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
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

            try {
                val result = authRepository.login(currentState.email, currentState.password)

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
                            
                            // I-parse ang user profile mula sa response
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
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is ConnectException -> "Server connection failed."
                    is SocketTimeoutException -> "Timeout."
                    is UnknownHostException -> "No internet."
                    else -> "Error: ${e.localizedMessage}"
                }
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

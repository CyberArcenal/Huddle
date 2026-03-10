package com.cyberarcenal.huddle.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            // I-set ang loading state
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                // Network call
                val result = authRepository.login(currentState.email, currentState.password)

                result.fold(
                    onSuccess = { response ->
                        // Ang response ay Map<String, Any>
                        val checkpointToken = response["checkpoint_token"] as? String
                        if (checkpointToken != null) {
                            // Kailangan ng 2FA verification
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                navigateTo2FA = true,
                                checkpointToken = checkpointToken,
                                error = null
                            )
                        } else {
                            // Success: Kunin ang tokens
                            val accessToken = response["accessToken"] as? String
                            val refreshToken = response["refreshToken"] as? String

                            if (accessToken != null && refreshToken != null) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    navigateToHome = true,
                                    accessToken = accessToken,
                                    refreshToken = refreshToken,
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
                        // Dito papasok ang mga API errors (e.g. 401 Unauthorized, 422 Validation)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                // FALLBACK: Dito mahuhuli ang mga Network/Hardware errors
                val errorMessage = when (e) {
                    is ConnectException -> "Hindi makakonekta sa server. Siguraduhing tama ang IP address (10.0.2.2 para sa emulator o PC IP para sa device)."
                    is SocketTimeoutException -> "Masyadong matagal ang response ng server. Pakisubukang muli."
                    is UnknownHostException -> "Walang internet connection o hindi mahanap ang host."
                    else -> "Network Error: ${e.localizedMessage}"
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetNavigation() {
        _uiState.value = _uiState.value.copy(
            navigateToHome = false,
            navigateTo2FA = false,
            checkpointToken = null,
            accessToken = null,
            refreshToken = null
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
    val refreshToken: String? = null
)
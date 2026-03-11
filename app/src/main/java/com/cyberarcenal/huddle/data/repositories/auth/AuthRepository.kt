package com.cyberarcenal.huddle.data.repositories.auth

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import java.time.LocalDate
import java.time.OffsetDateTime

class AuthRepository {
    private val api = ApiService.v1Api

    suspend fun login(email: String, password: String): Result<Map<String, Any>> {
        val request = LoginRequestRequest(email = email, password = password)
        return safeApiCall { api.v1UsersLoginCreate(request) }
    }

    suspend fun verify2FA(checkpointToken: String, otpCode: String): Result<Verify2FAResponse> {
        val request = Verify2FARequestRequest(checkpointToken = checkpointToken, otpCode = otpCode)
        return safeApiCall { api.v1UsersLoginVerify2faCreate(request) }
    }

    suspend fun resend2FA(checkpointToken: String): Result<Resend2FAResponse> {
        val request = Resend2FARequestRequest(checkpointToken = checkpointToken)
        return safeApiCall { api.v1UsersLoginResend2faCreate(request) }
    }

    suspend fun refreshToken(refreshToken: String): Result<TokenRefreshResponse> {
        val request = TokenRefreshRequestRequest(refresh = refreshToken)
        return safeApiCall { api.v1UsersTokenRefreshCreate(request) }
    }

    suspend fun logout(refreshToken: String): Result<LogoutResponse> {
        val request = LogoutRequestRequest(refresh = refreshToken)
        return safeApiCall { api.v1UsersLoginLogoutCreate(request) }
    }

    suspend fun logoutAll(): Result<LogoutResponse> {
        return safeApiCall { api.v1UsersLoginLogoutAllCreate() }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
        dateOfBirth: String? = null,
        phoneNumber: String? = null,
        bio: String? = null
    ): Result<UserProfile> {
        val request = UserCreateRequest(
            username = username,
            password = password,
            confirmPassword = password,
            email = email,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth?.let { LocalDate.parse(it) },
            phoneNumber = phoneNumber,
            bio = bio
        )
        return safeApiCall { api.v1UsersRegisterCreate(request) }
    }

    suspend fun requestPasswordReset(email: String): Result<PasswordResetRequestResponse> {
        val request = PasswordResetRequestRequest(email = email)
        return safeApiCall { api.v1UsersPasswordResetCreate(request) }
    }

    suspend fun verifyPasswordReset(email: String, otpCode: String): Result<PasswordResetVerifyResponse> {
        val request = PasswordResetVerifyRequestRequest(email = email, otpCode = otpCode)
        return safeApiCall { api.v1UsersPasswordResetVerifyCreate(request) }
    }

    suspend fun completePasswordReset(checkpointToken: String, newPassword: String): Result<PasswordResetCompleteResponse> {
        val request = PasswordResetCompleteRequestRequest(
            checkpointToken = checkpointToken,
            newPassword = newPassword,
            confirmPassword = newPassword
        )
        return safeApiCall { api.v1UsersPasswordResetCompleteCreate(request) }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<PasswordChangeResponse> {
        val request = PasswordChangeRequestRequest(
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = newPassword
        )
        return safeApiCall { api.v1UsersPasswordChangeCreate(request) }
    }

    suspend fun verifyAccount(): Result<V1UsersVerifyCreate200Response> {
        return safeApiCall { api.v1UsersVerifyCreate() }
    }

    suspend fun deactivateAccount(password: String, confirm: Boolean = true): Result<V1UsersDeactivateCreate200Response> {
        val request = UserDeactivateInputRequest(
            password = password,
            confirm = confirm
        )
        return safeApiCall { api.v1UsersDeactivateCreate(request) }
    }
}
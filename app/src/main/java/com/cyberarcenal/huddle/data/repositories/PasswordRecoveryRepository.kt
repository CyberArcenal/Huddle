package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class PasswordRecoveryRepository {
    private val api = ApiService.passwordRecoveryApi

    // Step 1: Request OTP
    suspend fun requestReset(request: PasswordResetRequestRequest): Result<PasswordResetRequestResponse> =
        safeApiCall { api.apiV1UsersPasswordResetCreate(request) }

    // Step 2: Verify OTP and get Token
    suspend fun verifyReset(request: PasswordResetVerifyRequestRequest): Result<PasswordResetVerifyResponse> =
        safeApiCall { api.apiV1UsersPasswordResetVerifyCreate(request) }

    // Step 3: Complete Reset with New Password
    suspend fun completeReset(request: PasswordResetCompleteRequestRequest): Result<PasswordResetCompleteResponse> =
        safeApiCall { api.apiV1UsersPasswordResetCompleteCreate(request) }
}
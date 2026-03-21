package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class PasswordRecoveryRepository {
    private val api = ApiService.passwordRecoveryApi

    suspend fun requestReset(request: PasswordResetRequestRequest): Result<PasswordResetRequestResponse> =
        safeApiCall { api.apiV1UsersPasswordResetCreate(request) }

    suspend fun verifyReset(request: PasswordResetVerifyRequestRequest): Result<PasswordResetVerifyResponse> =
        safeApiCall { api.apiV1UsersPasswordResetVerifyCreate(request) }

    suspend fun completeReset(request: PasswordResetCompleteRequestRequest): Result<PasswordResetCompleteResponse> =
        safeApiCall { api.apiV1UsersPasswordResetCompleteCreate(request) }
}
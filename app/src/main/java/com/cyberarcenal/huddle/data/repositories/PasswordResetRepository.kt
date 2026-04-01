package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class PasswordResetRepository {
    private val api = ApiService.passwordResetApi

    suspend fun changePassword(request: PasswordChangeRequestRequest): Result<PasswordChangeResponse> =
        safeApiCall { api.apiV1UsersPasswordChangeCreate(request) }

    suspend fun checkPasswordStrength(request: PasswordStrengthCheckRequestRequest): Result<PasswordStrengthCheckResponse> =
        safeApiCall { api.apiV1UsersPasswordCheckStrengthCreate(request) }

    suspend fun getPasswordHistory(): Result<PasswordHistoryResponse> =
        safeApiCall { api.apiV1UsersPasswordHistoryRetrieve() }
}
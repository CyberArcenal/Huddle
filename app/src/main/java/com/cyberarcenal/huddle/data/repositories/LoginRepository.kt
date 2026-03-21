package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class LoginRepository {
    private val api = ApiService.loginApi

    suspend fun login(request: LoginRequestRequest): Result<Map<String, Any>> =
        safeApiCall { api.apiV1UsersLoginCreate(request) }

    suspend fun resend2fa(request: Resend2FARequestRequest): Result<Resend2FAResponse> =
        safeApiCall { api.apiV1UsersLoginResend2faCreate(request) }

    suspend fun verify2fa(request: Verify2FARequestRequest): Result<Verify2FAResponse> =
        safeApiCall { api.apiV1UsersLoginVerify2faCreate(request) }
}
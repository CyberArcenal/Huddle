package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class TokenRepository {
    private val api = ApiService.tokenApi

    suspend fun refreshToken(request: TokenRefreshRequestRequest): Result<TokenRefreshResponse> =
        safeApiCall { api.refreshCreate(request) }

    suspend fun verifyToken(request: TokenVerifyRequestRequest): Result<TokenVerifyResponse> =
        safeApiCall { api.verifyCreate(request) }
}
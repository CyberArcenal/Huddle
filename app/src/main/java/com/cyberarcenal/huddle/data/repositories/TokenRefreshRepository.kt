// TokenRefreshRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class TokenRefreshRepository {
    private val api = ApiService.tokenRefreshApi

    suspend fun refreshToken(request: TokenRefreshRequestRequest): Result<TokenRefreshResponse> =
        safeApiCall { api.apiV1UsersTokenRefreshCreate(request) }
}
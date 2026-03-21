// LogOutRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class LogOutRepository {
    private val api = ApiService.logOutApi

    suspend fun logoutAll(): Result<LogoutResponse> =
        safeApiCall { api.apiV1UsersLoginLogoutAllCreate() }

    suspend fun logout(request: LogoutRequestRequest): Result<LogoutResponse> =
        safeApiCall { api.apiV1UsersLoginLogoutCreate(request) }
}
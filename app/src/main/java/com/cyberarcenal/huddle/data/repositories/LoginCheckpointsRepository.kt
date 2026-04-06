package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class LoginCheckpointsRepository {
    private val api = ApiService.loginCheckpointsApi

    suspend fun getLoginCheckpoints(
        page: Int? = null,
        pageSize: Int? = null,
        isUsed: Boolean? = null,
        isValid: Boolean? = null,
        search: String? = null
    ): Result<LoginCheckpointListResponse> =
        safeApiCall { api.apiV1UsersAuthCheckpointsRetrieve(isUsed, isValid, page, pageSize, search) }

    suspend fun getLoginCheckpoint(id: Int): Result<LoginCheckpointDetailResponse> =
        safeApiCall { api.apiV1UsersAuthCheckpointsRetrieve2(id) }
}
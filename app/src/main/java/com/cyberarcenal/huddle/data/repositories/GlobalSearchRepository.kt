package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.GlobalSearchApi
import com.cyberarcenal.huddle.api.models.GlobalSearchResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class GlobalSearchRepository {
    private val api: GlobalSearchApi = ApiService.globalSearchApi

    suspend fun performGlobalSearch(query: String): Result<GlobalSearchResponse> =
        safeApiCall { api.apiV1SearchSearchGlobalRetrieve(q = query) }
}
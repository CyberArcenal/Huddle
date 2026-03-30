package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.GlobalSearchApi
import com.cyberarcenal.huddle.api.models.PaginatedGlobalSearchResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalSearchRepository @Inject constructor() {

    // Siguraduhin na ang globalSearchApi ay deklara sa iyong ApiService object
    private val api = ApiService.globalSearchApi

    /**
     * Global search across users, posts, groups, events, etc.
     * @param query Ang keyword na gustong hanapin ng user.
     */
    suspend fun performGlobalSearch(query: String): Result<PaginatedGlobalSearchResponse> = safeApiCall {
        api.apiV1SearchSearchGlobalRetrieve(q = query)
    }
}
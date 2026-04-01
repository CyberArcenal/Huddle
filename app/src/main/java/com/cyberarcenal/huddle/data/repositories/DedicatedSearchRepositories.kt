package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.DedicatedSearchsApi
import com.cyberarcenal.huddle.api.models.SearchResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class DedicatedSearchRepositories {
    private val api: DedicatedSearchsApi = ApiService.dedicatedSearchApi

    suspend fun searchEvents(q: String, page: Int? = null, pageSize: Int? = null): Result<SearchResponse> =
        safeApiCall { api.apiV1SearchEventsRetrieve(q, page, pageSize) }

    suspend fun searchGroups(q: String, page: Int? = null, pageSize: Int? = null): Result<SearchResponse> =
        safeApiCall { api.apiV1SearchGroupsRetrieve(q, page, pageSize) }

    suspend fun searchPosts(q: String, page: Int? = null, pageSize: Int? = null): Result<SearchResponse> =
        safeApiCall { api.apiV1SearchPostsRetrieve(q, page, pageSize) }
}
// MatchesRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class MatchesRepository {
    private val api = ApiService.matchesApi

    suspend fun getActiveMatches(limit: Int? = null, offset: Int? = null): Result<PaginatedMatch> =
        safeApiCall { api.apiV1DatingActiveRetrieve(limit, offset) }

    suspend fun createMatch(request: MatchCreateRequest): Result<MatchDetail> =
        safeApiCall { api.apiV1DatingCreateCreate(request) }

    suspend fun getMatch(id: Int): Result<MatchDetail> =
        safeApiCall { api.apiV1DatingRetrieve2(id) }

    suspend fun unmatch(request: MatchUnmatchRequest): Result<MatchUnmatchResponse> =
        safeApiCall { api.apiV1DatingUnmatchCreate(request) }
}
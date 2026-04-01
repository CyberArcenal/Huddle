package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class TrendScoreRepository {
    private val api = ApiService.trendScoreApi

    suspend fun cleanup(olderThanDays: Int? = null): Result<TrendScoreCleanupResponse> = safeApiCall {
        api.apiV1AnalyticsTrendScoreCleanupCreate(
            if (olderThanDays != null) CleanupTrendScoreInputRequest(olderThanDays) else null
        )
    }

    suspend fun recalculateScore(request: RecalculateScoreInputRequest): Result<TrendScoreObjectPostResponse> =
        safeApiCall { api.apiV1AnalyticsTrendScoreObjectCreate(request) }

    suspend fun deleteScore(targetId: Int, targetType: String): Result<TrendScoreObjectDeleteResponse> =
        safeApiCall { api.apiV1AnalyticsTrendScoreObjectDestroy(targetId, targetType) }

    suspend fun getScore(targetId: Int, targetType: String): Result<TrendScoreObjectGetResponse> =
        safeApiCall { api.apiV1AnalyticsTrendScoreObjectRetrieve(targetId, targetType) }

    suspend fun getStatistics(): Result<TrendScoreStatisticsResponse> =
        safeApiCall { api.apiV1AnalyticsTrendScoreStatisticsRetrieve() }

    suspend fun getTop(
        contentType: String? = null,
        limit: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<TrendScoreTopResponse> =
        safeApiCall { api.apiV1AnalyticsTrendScoreTopRetrieve(contentType, limit, page, pageSize) }
}
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.TrendScoreApi
import com.cyberarcenal.huddle.api.models.ApiV1AdminPannelLogsCleanupCreate200Response
import com.cyberarcenal.huddle.api.models.CleanupTrendScoreInputRequest
import com.cyberarcenal.huddle.api.models.PaginatedTrendScoreMinimal
import com.cyberarcenal.huddle.api.models.RecalculateScoreInputRequest
import com.cyberarcenal.huddle.api.models.TrendScoreDisplay
import com.cyberarcenal.huddle.api.models.TrendScoreStatistics
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class TrendScoreRepository {
    private val api = ApiService.trendScoreApi

    /**
     * Delete trend scores that haven't been updated in more than the specified days. (Admin only)
     */
    suspend fun cleanup(olderThanDays: Int? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.apiV1AnalyticsTrendScoreCleanupCreate(
            if (olderThanDays != null) CleanupTrendScoreInputRequest(olderThanDays) else null
        )
    }

    /**
     * Recalculate the trend score for a content object. (Admin only)
     */
    suspend fun recalculateScore(request: RecalculateScoreInputRequest): Result<TrendScoreDisplay> = safeApiCall {
        api.apiV1AnalyticsTrendScoreObjectCreate(request)
    }

    /**
     * Delete the trend score for a content object. (Admin only)
     */
    suspend fun deleteScore(targetId: Int, targetType: String): Result<Unit> = safeApiCall {
        api.apiV1AnalyticsTrendScoreObjectDestroy(targetId, targetType)
    }

    /**
     * Retrieve the trend score for a specific content object.
     */
    suspend fun getScore(targetId: Int, targetType: String): Result<TrendScoreDisplay> = safeApiCall {
        api.apiV1AnalyticsTrendScoreObjectRetrieve(targetId, targetType)
    }

    /**
     * Get average, highest, and lowest trend scores across all objects.
     */
    suspend fun getStatistics(): Result<TrendScoreStatistics> = safeApiCall {
        api.apiV1AnalyticsTrendScoreStatisticsRetrieve()
    }

    /**
     * Retrieve the top trending objects.
     */
    suspend fun getTop(
        contentType: String? = null,
        limit: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedTrendScoreMinimal> = safeApiCall {
        api.apiV1AnalyticsTrendScoreTopRetrieve(contentType, limit, page, pageSize)
    }
}
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class PlatformAnalyticsRepository {
    private val api = ApiService.platformAnalyticsApi

    suspend fun cleanupPlatformAnalytics(request: CleanupAnalyticsInputRequest? = null): Result<PlatformAnalyticsCleanupResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformCleanupCreate(request) }

    suspend fun getCorrelation(metric1: String, metric2: String, days: Int? = null): Result<PlatformAnalyticsCorrelationResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformCorrelationRetrieve(metric1, metric2, days) }

    suspend fun updateDailyAnalytics(request: UpdatePlatformAnalyticsInputRequest? = null): Result<PlatformAnalyticsDailyResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformDailyCreate(request) }

    suspend fun getDailyAnalytics(date: String? = null): Result<PlatformAnalyticsDailyResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformDailyRetrieve(date) }

    suspend fun getPlatformHealth(days: Int? = null): Result<PlatformAnalyticsHealthResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformHealthRetrieve(days) }

    suspend fun getPlatformAnalyticsRange(
        endDate: String,
        startDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PlatformAnalyticsRangeResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize) }

    suspend fun getDailyReport(date: String? = null): Result<PlatformAnalyticsReportResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformReportRetrieve(date) }

    suspend fun getPlatformAnalyticsSummary(days: Int? = null): Result<PlatformAnalyticsSummaryResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformSummaryRetrieve(days) }

    suspend fun getTopDays(limit: Int? = null, metric: String? = null): Result<PlatformAnalyticsTopDaysResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformTopDaysRetrieve(limit, metric) }

    suspend fun getPlatformTrends(
        metric: String,
        days: Int? = null,
        movingAverage: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PlatformAnalyticsTrendsResponse> =
        safeApiCall { api.apiV1AnalyticsPlatformTrendsRetrieve(metric, days, movingAverage, page, pageSize) }
}
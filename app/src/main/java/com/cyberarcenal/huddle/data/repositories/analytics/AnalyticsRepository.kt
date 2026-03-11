package com.cyberarcenal.huddle.data.repositories.analytics

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class AnalyticsRepository {
    private val api = ApiService.v1Api

    // ========== PLATFORM ANALYTICS ==========

    suspend fun getDailyPlatformAnalytics(date: String? = null): Result<PlatformAnalytics> = safeApiCall {
        api.v1AnalyticsPlatformDailyRetrieve(date)
    }

    suspend fun updateDailyPlatformAnalytics(platformAnalytics: UpdatePlatformAnalyticsInputRequest): Result<PlatformAnalytics> = safeApiCall {
        api.v1AnalyticsPlatformDailyCreate(platformAnalytics)
    }

    suspend fun getPlatformAnalyticsRange(
        startDate: String,
        endDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPlatformAnalytics> = safeApiCall {
        api.v1AnalyticsPlatformRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize)
    }

    suspend fun getPlatformReport(date: String? = null): Result<DailyReport> = safeApiCall {
        api.v1AnalyticsPlatformReportRetrieve(date)
    }

    suspend fun getPlatformSummary(days: Int? = null): Result<PlatformAnalyticsSummary> = safeApiCall {
        api.v1AnalyticsPlatformSummaryRetrieve(days)
    }

    suspend fun getPlatformTrends(
        metric: String,
        days: Int? = null,
        movingAverage: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPlatformTrend> = safeApiCall {
        api.v1AnalyticsPlatformTrendsRetrieve(metric, days, movingAverage, page, pageSize)
    }

    suspend fun getTopDays(
        metric: String? = null,
        limit: Int? = null
    ): Result<List<PlatformTopDay>> = safeApiCall {
        api.v1AnalyticsPlatformTopDaysList(limit, metric)
    }

    suspend fun getCorrelation(
        metric1: String,
        metric2: String,
        days: Int? = null
    ): Result<PlatformCorrelation> = safeApiCall {
        api.v1AnalyticsPlatformCorrelationRetrieve(metric1, metric2, days)
    }

    suspend fun getPlatformHealth(days: Int? = null): Result<PlatformHealth> = safeApiCall {
        api.v1AnalyticsPlatformHealthRetrieve(days)
    }

    suspend fun cleanupPlatformAnalytics(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupAnalyticsInputRequest(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AnalyticsPlatformCleanupCreate(body) }
    }

    // ========== USER ANALYTICS ==========

    suspend fun getUserDailyAnalytics(
        userId: Int? = null,
        date: String? = null
    ): Result<UserAnalytics> = safeApiCall {
        if (userId != null) {
            api.v1AnalyticsUserDailyRetrieve2(userId, date)
        } else {
            api.v1AnalyticsUserDailyRetrieve(date)
        }
    }

    suspend fun getUserAnalyticsRange(
        userId: Int? = null,
        startDate: String,
        endDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserAnalytics> = safeApiCall {
        if (userId != null) {
            api.v1AnalyticsUserRangeRetrieve2(userId, endDate, startDate, includeEmptyDays, page, pageSize)
        } else {
            api.v1AnalyticsUserRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize)
        }
    }

    suspend fun getUserAnalyticsSummary(
        userId: Int? = null,
        days: Int? = null
    ): Result<UserAnalyticsSummary> = safeApiCall {
        if (userId != null) {
            api.v1AnalyticsUserSummaryRetrieve2(userId, days)
        } else {
            api.v1AnalyticsUserSummaryRetrieve(days)
        }
    }

    suspend fun getUserTrends(
        metric: String,
        userId: Int? = null,
        days: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserTrend> = safeApiCall {
        if (userId != null) {
            api.v1AnalyticsUserTrendsRetrieve2(userId, metric, days, page, pageSize)
        } else {
            api.v1AnalyticsUserTrendsRetrieve(metric, days, page, pageSize)
        }
    }

    suspend fun getUserTopDays(
        userId: Int? = null,
        metric: String? = null,
        limit: Int? = null
    ): Result<List<UserTopDay>> = safeApiCall {
        if (userId != null) {
            api.v1AnalyticsUserTopDaysList2(userId, limit, metric)
        } else {
            api.v1AnalyticsUserTopDaysList(limit, metric)
        }
    }

    suspend fun getUserEngagement(
        userId: Int? = null,
        days: Int? = null
    ): Result<UserEngagement> = safeApiCall {
        if (userId != null) {
            api.v1AnalyticsUserEngagementRetrieve2(userId, days)
        } else {
            api.v1AnalyticsUserEngagementRetrieve(days)
        }
    }

    suspend fun compareUsers(
        user1Id: Int,
        user2Id: Int,
        days: Int? = null
    ): Result<UserCompare> = safeApiCall {
        api.v1AnalyticsUserCompareRetrieve(user1Id, user2Id, days)
    }

    suspend fun cleanupUserAnalytics(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupUserAnalyticsInputRequest(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AnalyticsUserCleanupCreate(body) }
    }
}
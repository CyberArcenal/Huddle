package com.cyberarcenal.huddle.data.repositories.analytics

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class AnalyticsRepository {
    private val api = ApiService.v1Api

    // ========== PLATFORM ANALYTICS ==========

    /**
     * Get daily platform analytics for a specific date. If not found, creates a new record with zero values.
     */
    suspend fun getDailyPlatformAnalytics(date: String? = null): Result<PlatformAnalytics> = safeApiCall {
        api.v1AnalyticsPlatformDailyRetrieve(date)
    }

    /**
     * Manually update daily analytics. Only fields provided will be updated.
     */
    suspend fun updateDailyPlatformAnalytics(platformAnalytics:  UpdatePlatformAnalyticsInput): Result<PlatformAnalytics> = safeApiCall {
        api.v1AnalyticsPlatformDailyCreate(platformAnalytics)
    }

    /**
     * Get platform analytics for a date range, with optional pagination.
     */
    suspend fun getPlatformAnalyticsRange(
        startDate: String,
        endDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPlatformAnalytics> = safeApiCall {
        api.v1AnalyticsPlatformRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize)
    }

    /**
     * Generate a detailed daily report with changes from previous day.
     */
    suspend fun getPlatformReport(date: String? = null): Result<DailyReport> = safeApiCall {
        api.v1AnalyticsPlatformReportRetrieve(date)
    }

    /**
     * Get a summary of platform metrics over the last N days.
     */
    suspend fun getPlatformSummary(days: Int? = null): Result<PlatformAnalyticsSummary> = safeApiCall {
        api.v1AnalyticsPlatformSummaryRetrieve(days)
    }

    /**
     * Get trend data for a specific metric.
     */
    suspend fun getPlatformTrends(
        metric: String,
        days: Int? = null,
        movingAverage: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPlatformTrend> = safeApiCall {
        api.v1AnalyticsPlatformTrendsRetrieve(metric, days, movingAverage, page, pageSize)
    }

    /**
     * Get the top performing days for a given metric.
     */
    suspend fun getTopDays(
        metric: String? = null,
        limit: Int? = null
    ): Result<List<PlatformTopDay>> = safeApiCall {
        api.v1AnalyticsPlatformTopDaysList(limit, metric)
    }

    /**
     * Calculate correlation coefficient between two metrics over a period.
     */
    suspend fun getCorrelation(
        metric1: String,
        metric2: String,
        days: Int? = null
    ): Result<PlatformCorrelation> = safeApiCall {
        api.v1AnalyticsPlatformCorrelationRetrieve(metric1, metric2, days)
    }

    /**
     * Calculate overall platform health score.
     */
    suspend fun getPlatformHealth(days: Int? = null): Result<PlatformHealth> = safeApiCall {
        api.v1AnalyticsPlatformHealthRetrieve(days)
    }

    /**
     * Delete analytics records older than specified days.
     */
    suspend fun cleanupPlatformAnalytics(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupAnalyticsInput(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AnalyticsPlatformCleanupCreate(body) }
    }

    // ========== USER ANALYTICS ==========

    /**
     * Get daily analytics for a user. If userId is null, returns for current user.
     */
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

    /**
     * Get user analytics for a date range.
     */
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

    /**
     * Get a summary of a user's activity over the last N days.
     */
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

    /**
     * Get trend data for a specific metric for a user.
     */
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

    /**
     * Get the top N days for a user based on a specific metric.
     */
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

    /**
     * Calculate engagement metrics for a user.
     */
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

    /**
     * Compare activity metrics of two users over a period.
     */
    suspend fun compareUsers(
        user1Id: Int,
        user2Id: Int,
        days: Int? = null
    ): Result<UserCompare> = safeApiCall {
        api.v1AnalyticsUserCompareRetrieve(user1Id, user2Id, days)
    }

    /**
     * Delete user analytics records older than specified days.
     */
    suspend fun cleanupUserAnalytics(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupUserAnalyticsInput(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AnalyticsUserCleanupCreate(body) }
    }
}
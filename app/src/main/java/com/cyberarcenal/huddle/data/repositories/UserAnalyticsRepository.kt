package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserAnalyticsRepository {
    private val api = ApiService.userAnalyticsApi

    // Cleanup user analytics records older than specified days
    suspend fun cleanupUserAnalytics(request: CleanupUserAnalyticsInputRequest? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1AnalyticsUserCleanupCreate(request) }

    // Compare activity metrics of two users
    suspend fun compareUsers(user1Id: Int, user2Id: Int, days: Int? = null): Result<UserCompare> =
        safeApiCall { api.apiV1AnalyticsUserCompareRetrieve(user1Id, user2Id, days) }

    // Get daily analytics for current user (optional date)
    suspend fun getMyDailyAnalytics(date: String? = null): Result<UserAnalyticsDisplay> =
        safeApiCall { api.apiV1AnalyticsUserDailyRetrieve(date) }

    // Get daily analytics for a specific user (optional date)
    suspend fun getUserDailyAnalytics(userId: Int, date: String? = null): Result<UserAnalyticsDisplay> =
        safeApiCall { api.apiV1AnalyticsUserDailyRetrieve2(userId, date) }

    // Get engagement metrics (likes, comments, trend) for current user
    suspend fun getMyEngagement(days: Int? = null): Result<UserEngagement> =
        safeApiCall { api.apiV1AnalyticsUserEngagementRetrieve(days) }

    // Get engagement metrics for a specific user
    suspend fun getUserEngagement(userId: Int, days: Int? = null): Result<UserEngagement> =
        safeApiCall { api.apiV1AnalyticsUserEngagementRetrieve2(userId, days) }

    // Get user analytics for a date range (current user)
    suspend fun getMyAnalyticsRange(
        endDate: String,
        startDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserAnalytics> =
        safeApiCall { api.apiV1AnalyticsUserRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize) }

    // Get user analytics for a date range (specific user)
    suspend fun getUserAnalyticsRange(
        userId: Int,
        endDate: String,
        startDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserAnalytics> =
        safeApiCall { api.apiV1AnalyticsUserRangeRetrieve2(userId, endDate, startDate, includeEmptyDays, page, pageSize) }

    // Get summary for current user over last N days
    suspend fun getMySummary(days: Int? = null): Result<UserAnalyticsSummary> =
        safeApiCall { api.apiV1AnalyticsUserSummaryRetrieve(days) }

    // Get summary for a specific user over last N days
    suspend fun getUserSummary(userId: Int, days: Int? = null): Result<UserAnalyticsSummary> =
        safeApiCall { api.apiV1AnalyticsUserSummaryRetrieve2(userId, days) }

    // Get top days for current user based on a metric
    suspend fun getMyTopDays(limit: Int? = null, metric: String? = null): Result<List<UserTopDay>> =
        safeApiCall { api.apiV1AnalyticsUserTopDaysList(limit, metric) }

    // Get top days for a specific user based on a metric
    suspend fun getUserTopDays(userId: Int, limit: Int? = null, metric: String? = null): Result<List<UserTopDay>> =
        safeApiCall { api.apiV1AnalyticsUserTopDaysList2(userId, limit, metric) }

    // Get daily trend data for current user for a metric
    suspend fun getMyTrends(metric: String, days: Int? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserTrend> =
        safeApiCall { api.apiV1AnalyticsUserTrendsRetrieve(metric, days, page, pageSize) }

    // Get daily trend data for a specific user for a metric
    suspend fun getUserTrends(userId: Int, metric: String, days: Int? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserTrend> =
        safeApiCall { api.apiV1AnalyticsUserTrendsRetrieve2(userId, metric, days, page, pageSize) }
}
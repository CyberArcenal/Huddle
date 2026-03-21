// UserAnalyticsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserAnalyticsRepository {
    private val api = ApiService.userAnalyticsApi

    suspend fun cleanupUserAnalytics(request: CleanupUserAnalyticsInputRequest? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1AnalyticsUserCleanupCreate(request) }

    suspend fun compareUsers(user1Id: Int, user2Id: Int, days: Int? = null): Result<UserCompare> =
        safeApiCall { api.apiV1AnalyticsUserCompareRetrieve(user1Id, user2Id, days) }

    suspend fun getDailyAnalytics(date: String? = null): Result<UserAnalyticsDisplay> =
        safeApiCall { api.apiV1AnalyticsUserDailyRetrieve(date) }

    suspend fun getDailyAnalyticsForUser(userId: Int, date: String? = null): Result<UserAnalyticsDisplay> =
        safeApiCall { api.apiV1AnalyticsUserDailyRetrieve2(userId, date) }

    suspend fun getUserEngagement(days: Int? = null): Result<UserEngagement> =
        safeApiCall { api.apiV1AnalyticsUserEngagementRetrieve(days) }

    suspend fun getUserEngagementForUser(userId: Int, days: Int? = null): Result<UserEngagement> =
        safeApiCall { api.apiV1AnalyticsUserEngagementRetrieve2(userId, days) }

    suspend fun getUserAnalyticsRange(endDate: String, startDate: String, includeEmptyDays: Boolean? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserAnalytics> =
        safeApiCall { api.apiV1AnalyticsUserRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize) }

    suspend fun getUserAnalyticsRangeForUser(userId: Int, endDate: String, startDate: String, includeEmptyDays: Boolean? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserAnalytics> =
        safeApiCall { api.apiV1AnalyticsUserRangeRetrieve2(userId, endDate, startDate, includeEmptyDays, page, pageSize) }

    suspend fun getUserAnalyticsSummary(days: Int? = null): Result<UserAnalyticsSummary> =
        safeApiCall { api.apiV1AnalyticsUserSummaryRetrieve(days) }

    suspend fun getUserAnalyticsSummaryForUser(userId: Int, days: Int? = null): Result<UserAnalyticsSummary> =
        safeApiCall { api.apiV1AnalyticsUserSummaryRetrieve2(userId, days) }

    suspend fun getTopDays(limit: Int? = null, metric: String? = null): Result<List<UserTopDay>> =
        safeApiCall { api.apiV1AnalyticsUserTopDaysList(limit, metric) }

    suspend fun getTopDaysForUser(userId: Int, limit: Int? = null, metric: String? = null): Result<List<UserTopDay>> =
        safeApiCall { api.apiV1AnalyticsUserTopDaysList2(userId, limit, metric) }

    suspend fun getUserTrends(metric: String, days: Int? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserTrend> =
        safeApiCall { api.apiV1AnalyticsUserTrendsRetrieve(metric, days, page, pageSize) }

    suspend fun getUserTrendsForUser(userId: Int, metric: String, days: Int? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserTrend> =
        safeApiCall { api.apiV1AnalyticsUserTrendsRetrieve2(userId, metric, days, page, pageSize) }
}
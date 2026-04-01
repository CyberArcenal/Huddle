package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserAnalyticsRepository {
    private val api = ApiService.userAnalyticsApi

    suspend fun cleanupUserAnalytics(request: CleanupUserAnalyticsInputRequest? = null): Result<UserAnalyticsCleanupResponse> =
        safeApiCall { api.apiV1AnalyticsUserCleanupCreate(request) }

    suspend fun compareUsers(user1Id: Int, user2Id: Int, days: Int? = null): Result<UserAnalyticsCompareResponse> =
        safeApiCall { api.apiV1AnalyticsUserCompareRetrieve(user1Id, user2Id, days) }

    suspend fun getMyDailyAnalytics(date: String? = null): Result<UserAnalyticsDailyResponse> =
        safeApiCall { api.apiV1AnalyticsUserDailyRetrieve(date) }

    suspend fun getUserDailyAnalytics(userId: Int, date: String? = null): Result<UserAnalyticsDailyResponse> =
        safeApiCall { api.apiV1AnalyticsUserDailyRetrieve2(userId, date) }

    suspend fun getMyEngagement(days: Int? = null): Result<UserAnalyticsEngagementResponse> =
        safeApiCall { api.apiV1AnalyticsUserEngagementRetrieve(days) }

    suspend fun getUserEngagement(userId: Int, days: Int? = null): Result<UserAnalyticsEngagementResponse> =
        safeApiCall { api.apiV1AnalyticsUserEngagementRetrieve2(userId, days) }

    suspend fun getMyAnalyticsRange(
        endDate: String,
        startDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<UserAnalyticsRangeResponse> =
        safeApiCall { api.apiV1AnalyticsUserRangeRetrieve(endDate, startDate, includeEmptyDays, page, pageSize) }

    suspend fun getUserAnalyticsRange(
        userId: Int,
        endDate: String,
        startDate: String,
        includeEmptyDays: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<UserAnalyticsRangeResponse> =
        safeApiCall { api.apiV1AnalyticsUserRangeRetrieve2(userId, endDate, startDate, includeEmptyDays, page, pageSize) }

    suspend fun getMySummary(days: Int? = null): Result<UserAnalyticsSummaryResponse> =
        safeApiCall { api.apiV1AnalyticsUserSummaryRetrieve(days) }

    suspend fun getUserSummary(userId: Int, days: Int? = null): Result<UserAnalyticsSummaryResponse> =
        safeApiCall { api.apiV1AnalyticsUserSummaryRetrieve2(userId, days) }

    suspend fun getMyTopDays(limit: Int? = null, metric: String? = null): Result<UserAnalyticsTopDaysResponse> =
        safeApiCall { api.apiV1AnalyticsUserTopDaysRetrieve(limit, metric) }

    suspend fun getUserTopDays(userId: Int, limit: Int? = null, metric: String? = null): Result<UserAnalyticsTopDaysResponse> =
        safeApiCall { api.apiV1AnalyticsUserTopDaysRetrieve2(userId, limit, metric) }

    suspend fun getMyTrends(
        metric: String,
        days: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<UserAnalyticsTrendsResponse> =
        safeApiCall { api.apiV1AnalyticsUserTrendsRetrieve(metric, days, page, pageSize) }

    suspend fun getUserTrends(
        userId: Int,
        metric: String,
        days: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<UserAnalyticsTrendsResponse> =
        safeApiCall { api.apiV1AnalyticsUserTrendsRetrieve2(userId, metric, days, page, pageSize) }
}
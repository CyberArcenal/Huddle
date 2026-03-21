// UserActivityRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserActivityRepository {
    private val api = ApiService.userActivityApi

    suspend fun getFollowingActivities(page: Int? = null, pageSize: Int? = null): Result<PaginatedUserActivity> =
        safeApiCall { api.apiV1UsersActivityFollowingRetrieve(page, pageSize) }

    suspend fun logActivity(request: LogActivityInputRequest): Result<LogActivityResponse> =
        safeApiCall { api.apiV1UsersActivityLogCreate(request) }

    suspend fun getRecentActivities(action: String? = null, page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedUserActivity> =
        safeApiCall { api.apiV1UsersActivityRecentRetrieve(action, page, pageSize, userId) }

    suspend fun getMyActivities(action: String? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserActivity> =
        safeApiCall { api.apiV1UsersActivityRetrieve(action, page, pageSize) }

    suspend fun getActivitySummary(): Result<ActivitySummaryResponse> =
        safeApiCall { api.apiV1UsersActivitySummaryRetrieve() }
}
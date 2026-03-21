// AdminLogRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class AdminLogRepository {
    private val api = ApiService.adminLogApi

    suspend fun banUser(request: BanUserInputRequest): Result<BanUserResponse> =
        safeApiCall { api.apiV1AdminPannelActionsBanUserCreate(request) }

    suspend fun removeContent(request: RemoveContentInputRequest): Result<RemoveContentResponse> =
        safeApiCall { api.apiV1AdminPannelActionsRemoveContentCreate(request) }

    suspend fun warnUser(request: WarnUserInputRequest): Result<WarnUserResponse> =
        safeApiCall { api.apiV1AdminPannelActionsWarnUserCreate(request) }

    suspend fun cleanupLogs(request: CleanupLogsInputRequest? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1AdminPannelLogsCleanupCreate(request) }

    suspend fun exportLogs(endDate: String? = null, format: String? = null, startDate: String? = null): Result<ExportAdminLogsResponse> =
        safeApiCall { api.apiV1AdminPannelLogsExportRetrieve(endDate, format, startDate) }

    suspend fun getRecentLogs(days: Int? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedAdminLog> =
        safeApiCall { api.apiV1AdminPannelLogsRecentRetrieve(days, page, pageSize) }

    suspend fun getLogs(
        action: String? = null,
        adminUserId: Int? = null,
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        startDate: String? = null,
        targetUserId: Int? = null
    ): Result<PaginatedAdminLog> =
        safeApiCall { api.apiV1AdminPannelLogsRetrieve(action, adminUserId, endDate, page, pageSize, startDate, targetUserId) }

    suspend fun getLog(logId: Int): Result<AdminLogDisplay> =
        safeApiCall { api.apiV1AdminPannelLogsRetrieve2(logId) }

    suspend fun searchLogs(query: String, page: Int? = null, pageSize: Int? = null, searchIn: String? = null): Result<PaginatedAdminLog> =
        safeApiCall { api.apiV1AdminPannelLogsSearchRetrieve(query, page, pageSize, searchIn) }

    suspend fun getLogStatistics(adminUserId: Int? = null, days: Int? = null): Result<AdminStatistics> =
        safeApiCall { api.apiV1AdminPannelLogsStatisticsRetrieve(adminUserId, days) }

    suspend fun getUserLogs(
        userId: Int,
        asAdmin: Boolean? = null,
        asTarget: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> =
        safeApiCall { api.apiV1AdminPannelLogsUserRetrieve(userId, asAdmin, asTarget, page, pageSize) }
}
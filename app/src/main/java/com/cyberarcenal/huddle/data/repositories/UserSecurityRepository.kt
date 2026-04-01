package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserSecurityRepository {
    private val api = ApiService.userSecurityApi

    suspend fun bulkTerminateSessions(request: BulkTerminateSessionsRequest): Result<BulkTerminateSessionsResponse> =
        safeApiCall { api.apiV1UsersSecurityBulkTerminateSessionsCreate(request) }

    suspend fun changePassword(request: ChangePasswordRequest): Result<ChangePasswordResponse> =
        safeApiCall { api.apiV1UsersSecurityChangePasswordCreate(request) }

    suspend fun check2fa(): Result<Check2FAStatusResponse> =
        safeApiCall { api.apiV1UsersSecurityCheck2faRetrieve() }

    suspend fun disable2fa(request: DisableTwoFactorRequest): Result<Disable2FAResponse> =
        safeApiCall { api.apiV1UsersSecurityDisable2faCreate(request) }

    suspend fun enable2fa(request: EnableTwoFactorRequest): Result<Enable2FAResponse> =
        safeApiCall { api.apiV1UsersSecurityEnable2faCreate(request) }

    suspend fun getFailedLogins(): Result<FailedLoginAttemptsResponse> =
        safeApiCall { api.apiV1UsersSecurityFailedLoginsRetrieve() }

    suspend fun getSecurityLogs(
        eventType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSecurityLogResponse> =
        safeApiCall { api.apiV1UsersSecurityLogsRetrieve(eventType, page, pageSize) }

    suspend fun getSessions(page: Int? = null, pageSize: Int? = null): Result<PaginatedLoginSessionResponse> =
        safeApiCall { api.apiV1UsersSecuritySessionsRetrieve(page, pageSize) }

    suspend fun getSecuritySettings(): Result<SecuritySettingsGetResponse> =
        safeApiCall { api.apiV1UsersSecuritySettingsRetrieve() }

    suspend fun updateSecuritySettings(request: UpdateSecuritySettingsRequest? = null): Result<SecuritySettingsUpdateResponse> =
        safeApiCall { api.apiV1UsersSecuritySettingsUpdate(request) }

    suspend fun getSuspiciousActivities(limit: Int? = null): Result<SuspiciousActivitiesResponse> =
        safeApiCall { api.apiV1UsersSecuritySuspiciousActivitiesRetrieve(limit) }

    suspend fun terminateAllSessions(): Result<TerminateAllSessionsResponse> =
        safeApiCall { api.apiV1UsersSecurityTerminateAllSessionsCreate() }

    suspend fun terminateSession(request: TerminateSessionRequest): Result<TerminateSessionResponse> =
        safeApiCall { api.apiV1UsersSecurityTerminateSessionCreate(request) }
}
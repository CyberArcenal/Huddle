package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.AdminApi
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class AdminRepository {
    private val api: AdminApi = ApiService.adminApi

    suspend fun bulkUserAction(request: BulkUserActionRequest): Result<AdminBulkUserActionResponse> =
        safeApiCall { api.apiV1UsersAdminBulkActionCreate(request) }

    suspend fun cleanup(request: CleanupActionInputRequest): Result<CleanupActionResponse> =
        safeApiCall { api.apiV1UsersAdminCleanupCreate(request) }

    suspend fun getDashboard(): Result<AdminDashboardResponse> =
        safeApiCall { api.apiV1UsersAdminDashboardRetrieve() }

    suspend fun exportUserData(userId: Int): Result<UserExportResponse> =
        safeApiCall { api.apiV1UsersAdminExportRetrieve(userId) }

    suspend fun createUser(request: AdminUserCreateRequest): Result<AdminCreateUserResponse> =
        safeApiCall { api.apiV1UsersAdminUsersCreateCreate(request) }

    suspend fun listUsers(
        isActive: Boolean? = null,
        isVerified: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null,
        search: String? = null,
        status: String? = null
    ): Result<AdminUserListResponse> =
        safeApiCall {
            api.apiV1UsersAdminUsersRetrieve(isActive, isVerified, page, pageSize, search, status)
        }

    suspend fun getUserDetail(userId: Int): Result<AdminUserDetailResponse> =
        safeApiCall { api.apiV1UsersAdminUsersRetrieve2(userId) }

    suspend fun updateUser(userId: Int, request: AdminUserUpdateRequest): Result<AdminUserUpdateResponse> =
        safeApiCall { api.apiV1UsersAdminUsersUpdate(userId, request) }
}
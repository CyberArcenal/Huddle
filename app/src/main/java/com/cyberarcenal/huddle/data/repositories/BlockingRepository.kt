package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class BlockingRepository {
    private val api = ApiService.blockingApi

    suspend fun blockUser(request: BlockedUserCreateRequest): Result<BlockResponse> =
        safeApiCall { api.apiV1UsersBlocksBlockCreate(request) }

    suspend fun checkBlocked(userId: Int): Result<CheckBlockedResponse> =
        safeApiCall { api.apiV1UsersBlocksCheckRetrieve(userId) }

    suspend fun getBlockedUsers(limit: Int? = null, offset: Int? = null): Result<BlockedUsersListResponse> =
        safeApiCall { api.apiV1UsersBlocksRetrieve(limit, offset) }

    suspend fun unblockUser(request: UnblockUserRequest): Result<UnblockResponse> =
        safeApiCall { api.apiV1UsersBlocksUnblockCreate(request) }
}
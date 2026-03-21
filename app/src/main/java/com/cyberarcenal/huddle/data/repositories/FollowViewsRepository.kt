// FollowViewsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class FollowViewsRepository {
    private val api = ApiService.followViewsApi

    suspend fun followUser(request: FollowUserRequest): Result<FollowUserResponse> =
        safeApiCall { api.apiV1UsersFollowCreate(request) }

    suspend fun getFollowStats(userId: Int? = null): Result<FollowStatsResponse> =
        safeApiCall { api.apiV1UsersFollowStatsRetrieve(userId) }

    suspend fun getFollowStatsForUser(userId: Int, userId2: Int? = null): Result<FollowStatsResponse> =
        safeApiCall { api.apiV1UsersFollowStatsRetrieve2(userId, userId2) }

    suspend fun getFollowStatus(userId: Int): Result<FollowStatusResponse> =
        safeApiCall { api.apiV1UsersFollowStatusRetrieve(userId) }

    suspend fun getFollowers(page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersFollowersRetrieve(page, pageSize, userId) }

    suspend fun getFollowersForUser(userId: Int, page: Int? = null, pageSize: Int? = null, userId2: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersFollowersRetrieve2(userId, page, pageSize, userId2) }

    suspend fun getFollowing(page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersFollowingRetrieve(page, pageSize, userId) }

    suspend fun getFollowingForUser(userId: Int, page: Int? = null, pageSize: Int? = null, userId2: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersFollowingRetrieve2(userId, page, pageSize, userId2) }

    suspend fun getMutualFollows(userId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersMutualFollowsRetrieve(userId, page, pageSize) }

    suspend fun getMutualFriends(page: Int? = null, pageSize: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersMutualFriendsRetrieve(page, pageSize) }

    suspend fun getPopularUsers(page: Int? = null, pageSize: Int? = null): Result<PaginatedUserList> =
        safeApiCall { api.apiV1UsersPopularUsersRetrieve(page, pageSize) }

    suspend fun getSuggestedUsers(minMutual: Int? = null, page: Int?=null, pageSize: Int?=null): Result<PaginatedSuggestedUser> =
        safeApiCall { api.apiV1UsersSuggestedUsersRetrieve(minMutual, page, pageSize) }

    suspend fun unfollowUser(request: UnfollowUserRequest): Result<UnfollowUserResponse> =
        safeApiCall { api.apiV1UsersUnfollowCreate(request) }
}
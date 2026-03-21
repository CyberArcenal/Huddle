// ReelsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class ReelsRepository {
    private val api = ApiService.reelsApi

    suspend fun createReel(request: ReelCreateRequest): Result<ReelDisplay> =
        safeApiCall { api.apiV1FeedReelsCreate(request) }

    suspend fun deleteReel(reelId: Int): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1FeedReelsDestroy(reelId) }

    suspend fun restoreReel(reelId: Int): Result<ReelRestoreResponse> =
        safeApiCall { api.apiV1FeedReelsRestoreCreate(reelId) }

    suspend fun getReels(page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedReel> =
        safeApiCall { api.apiV1FeedReelsRetrieve(page, pageSize, userId) }

    suspend fun getReel(reelId: Int): Result<ReelDisplay> =
        safeApiCall { api.apiV1FeedReelsRetrieve2(reelId) }

    suspend fun searchReels(q: String, page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedReel> =
        safeApiCall { api.apiV1FeedReelsSearchRetrieve(q, page, pageSize, userId) }

    suspend fun getReelStatistics(reelId: Int): Result<ApiV1FeedReelsStatisticsRetrieve200Response> =
        safeApiCall { api.apiV1FeedReelsStatisticsRetrieve(reelId) }

    suspend fun getTrendingReels(hours: Int? = null, limit: Int? = null, minLikes: Int? = null): Result<List<ReelDisplay>> =
        safeApiCall { api.apiV1FeedReelsTrendingList(hours, limit, minLikes) }

    suspend fun updateReel(reelId: Int, request: ReelUpdateRequest? = null): Result<ReelDisplay> =
        safeApiCall { api.apiV1FeedReelsUpdate(reelId, request) }

    suspend fun getMyReelStatistics(): Result<ApiV1FeedUsersReelStatisticsRetrieve200Response> =
        safeApiCall { api.apiV1FeedUsersMeReelStatisticsRetrieve() }

    suspend fun getUserReelStatistics(userId: Int): Result<ApiV1FeedUsersReelStatisticsRetrieve200Response> =
        safeApiCall { api.apiV1FeedUsersReelStatisticsRetrieve(userId) }
}
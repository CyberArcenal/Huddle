// UserReactionsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserReactionsRepository {
    private val api = ApiService.reactionsApi

    suspend fun checkLike(contentType: String, objectId: Int): Result<LikeCheckResponse> =
        safeApiCall { api.apiV1FeedLikesCheckRetrieve(contentType, objectId) }

    suspend fun createLike(request: LikeCreateRequest): Result<LikeDisplay> =
        safeApiCall { api.apiV1FeedLikesCreate(request) }

    suspend fun deleteLike(likeId: Int): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1FeedLikesDestroy(likeId) }

    suspend fun getMostLikedContent(contentType: String, days: Int? = null, limit: Int? = null): Result<MostLikedContentResponse> =
        safeApiCall { api.apiV1FeedLikesMostLikedRetrieve(contentType, days, limit) }

    suspend fun getMutualLikes(userId: Int): Result<MutualLikeResponse> =
        safeApiCall { api.apiV1FeedLikesMutualRetrieve(userId) }

    suspend fun getRecentLikers(contentType: String, objectId: Int, limit: Int? = null): Result<RecentLikersResponse> =
        safeApiCall { api.apiV1FeedLikesRecentRetrieve(contentType, objectId, limit) }

    suspend fun getMyLikes(contentType: String? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedLike> =
        safeApiCall { api.apiV1FeedLikesRetrieve(contentType, page, pageSize) }

    suspend fun getObjectLikes(contentType: String, objectId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedLike> =
        safeApiCall { api.apiV1FeedLikesRetrieve2(contentType, objectId, page, pageSize) }

    suspend fun getLike(likeId: Int): Result<LikeDisplay> =
        safeApiCall { api.apiV1FeedLikesRetrieve3(likeId) }

    suspend fun getLikeStatistics(userId: Int? = null): Result<UserLikeStatistics> =
        safeApiCall { api.apiV1FeedLikesStatisticsRetrieve(userId) }

    suspend fun getUserLikeStatistics(userId: Int, userId2: Int? = null): Result<UserLikeStatistics> =
        safeApiCall { api.apiV1FeedLikesStatisticsRetrieve2(userId, userId2) }

    suspend fun toggleLike(request: LikeToggleRequest): Result<LikeToggleResponse> =
        safeApiCall { api.apiV1FeedLikesToggleCreate(request) }

    suspend fun createReaction(request: ReactionCreateRequest): Result<ReactionResponse> =
        safeApiCall { api.apiV1FeedReactionsCreate(request) }
}
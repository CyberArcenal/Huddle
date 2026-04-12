package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class SharePostsRepository {
    private val api = ApiService.sharePostsApi

    suspend fun createShare(request: ShareCreateRequest): Result<ShareCreateResponse> =
        safeApiCall { api.apiV1FeedSharesCreate(request) }

    suspend fun deleteShare(shareId: Int, hard: Boolean? = null): Result<ShareDeleteResponse> =
        safeApiCall { api.apiV1FeedSharesDestroy(shareId, hard) }

    suspend fun getSharesByObject(
        contentType: String,
        objectId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<ShareObjectSharesResponse> =
        safeApiCall { api.apiV1FeedSharesObjectRetrieve(contentType, objectId, page, pageSize) }

    suspend fun restoreShare(shareId: Int): Result<ShareRestoreResponse> =
        safeApiCall { api.apiV1FeedSharesRestoreCreate(shareId) }

    suspend fun getShares(
        contentType: String? = null,
        objectId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        userId: Int? = null
    ): Result<ShareListResponse> =
        safeApiCall { api.apiV1FeedSharesRetrieve(contentType, objectId, page, pageSize, userId) }

    suspend fun getShare(shareId: Int): Result<ShareDetailResponse> =
        safeApiCall { api.apiV1FeedSharesRetrieve2(shareId) }

    suspend fun getUserShareStatistics(userId: Int? = null): Result<UserShareStatisticsResponse> =
        safeApiCall { api.apiV1FeedSharesStatisticsUserRetrieve(userId) }

    suspend fun updateShare(shareId: Int, request: ShareUpdateRequest): Result<ShareUpdateResponse> =
        safeApiCall { api.apiV1FeedSharesUpdate(shareId, request) }
}
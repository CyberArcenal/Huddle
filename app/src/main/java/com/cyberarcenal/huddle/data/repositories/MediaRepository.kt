package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class MediaRepository {
    private val api = ApiService.mediaApi

    suspend fun deleteMedia(mediaId: Int): Result<MediaDeleteResponse> =
        safeApiCall { api.apiV1FeedMediaDestroy(mediaId) }

    suspend fun getMedia(
        contentType: String? = null,
        groupContentType: String? = null,
        groupId: Int? = null,
        objectId: Int? = null,
        orderBy: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        postId: Int? = null,
        reelId: Int? = null
    ): Result<MediaListResponse> =
        safeApiCall {
            api.apiV1FeedMediaRetrieve(
                contentType, groupContentType, groupId, objectId, orderBy,
                page, pageSize, postId, reelId
            )
        }

    suspend fun getMediaById(mediaId: Int): Result<MediaDetailResponse> =
        safeApiCall { api.apiV1FeedMediaRetrieve2(mediaId) }

    suspend fun updateMedia(mediaId: Int, request: MediaCreateRequest): Result<MediaUpdateResponse> =
        safeApiCall { api.apiV1FeedMediaUpdate(mediaId, request) }
}
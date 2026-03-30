// ReelsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface ReelCreateApi {
    @Multipart
    @POST("api/v1/feed/reels/")
    suspend fun apiV1FeedReelsCreate(
        @Part("caption") caption: RequestBody,
        @Part media: MultipartBody.Part,
        @Part("duration") duration: RequestBody,
        @Part("privacy") privacy: RequestBody,
        @Part thumbnail: MultipartBody.Part? = null,
        @Part audio: MultipartBody.Part? = null
    ): Response<ReelCreateResponse>
}

class ReelsRepository {
    private val api = ApiService.reelsApi
    private val createApi = ApiService.reelCreateApi

    suspend fun createReel(
        media: MultipartBody.Part,
        thumbnail: MultipartBody.Part?,
        audio: MultipartBody.Part?,
        caption: String,
        duration: Int,
        privacy: PrivacyB23Enum
    ): Result<ReelCreateResponse> = safeApiCall {
        createApi.apiV1FeedReelsCreate(
            media = media,
            thumbnail = thumbnail,
            audio = audio,
            caption = caption.toRequestBody("text/plain".toMediaTypeOrNull()),
            duration = duration.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            privacy = privacy.value.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    suspend fun deleteReel(reelId: Int): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1FeedReelsDestroy(reelId) }

    suspend fun restoreReel(reelId: Int): Result<ReelRestoreResponse> =
        safeApiCall { api.apiV1FeedReelsRestoreCreate(reelId) }

    suspend fun getReels(
        page: Int? = null,
        pageSize: Int? = null,
        userId: Int? = null
    ): Result<PaginatedReel> =
        safeApiCall { api.apiV1FeedReelsRetrieve(page, pageSize, userId) }

    suspend fun getReel(reelId: Int): Result<ReelDisplay> =
        safeApiCall { api.apiV1FeedReelsRetrieve2(reelId) }

    suspend fun searchReels(
        q: String,
        page: Int? = null,
        pageSize: Int? = null,
        userId: Int? = null
    ): Result<PaginatedReel> =
        safeApiCall { api.apiV1FeedReelsSearchRetrieve(q, page, pageSize, userId) }

    suspend fun getReelStatistics(reelId: Int): Result<ApiV1FeedReelsStatisticsRetrieve200Response> =
        safeApiCall { api.apiV1FeedReelsStatisticsRetrieve(reelId) }

    suspend fun getTrendingReels(
        hours: Int? = null,
        limit: Int? = null,
        minLikes: Int? = null
    ): Result<List<ReelDisplay>> =
        safeApiCall { api.apiV1FeedReelsTrendingList(hours, limit, minLikes) }

    suspend fun updateReel(reelId: Int, request: ReelUpdateRequest? = null): Result<ReelDisplay> =
        safeApiCall { api.apiV1FeedReelsUpdate(reelId, request) }

    suspend fun getMyReelStatistics(): Result<ApiV1FeedUsersReelStatisticsRetrieve200Response> =
        safeApiCall { api.apiV1FeedUsersMeReelStatisticsRetrieve() }

    suspend fun getUserReelStatistics(userId: Int): Result<ApiV1FeedUsersReelStatisticsRetrieve200Response> =
        safeApiCall { api.apiV1FeedUsersReelStatisticsRetrieve(userId) }
}

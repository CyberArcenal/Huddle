package com.cyberarcenal.huddle.data.repositories

import android.content.Context
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.local.HuddleDatabase
import com.cyberarcenal.huddle.data.local.entities.ReelEntity
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        @Part audio: MultipartBody.Part? = null,
        @Part("client_id") clientId: RequestBody? = null
    ): Response<ReelCreateResponse>
}

class ReelsRepository(context: Context) {
    private val database = HuddleDatabase.getDatabase(context)
    private val reelDao = database.reelDao()
    private val api = ApiService.reelsApi
    private val createApi: ReelCreateApi = ApiService.reelCreateApi

    fun observeReels(userId: Int): Flow<List<ReelDisplay>> =
        reelDao.observeReels(userId).map { entities ->
            entities.map { it.rawData }
        }

    suspend fun fetchAndCacheReels(userId: Int? = null, context: Context? = null): Result<Unit> = safeApiCall {
        val response = api.apiV1FeedReelsRetrieve(userId = userId)
        
        if (response.isSuccessful) {
            val reels = response.body()?.data?.pagination?.results ?: emptyList()
            val targetUid = userId ?: context?.let { TokenManager.getUser(it)?.id }
            
            targetUid?.let { uid ->
                val entities = reels.mapIndexed { index, reel ->
                    ReelEntity(
                        id = reel.id!!,
                        userId = uid,
                        position = index,
                        rawData = reel
                    )
                }
                reelDao.refreshReels(uid, entities)
            }
        }
        response
    }.map { Unit }

    suspend fun createReel(
        media: MultipartBody.Part,
        thumbnail: MultipartBody.Part?,
        audio: MultipartBody.Part?,
        caption: String,
        duration: Int,
        privacy: PrivacyB23Enum,
        clientId: String? = null
    ): Result<ReelCreateResponse> = safeApiCall {
        createApi.apiV1FeedReelsCreate(
            media = media,
            thumbnail = thumbnail,
            audio = audio,
            caption = caption.toRequestBody("text/plain".toMediaTypeOrNull()),
            duration = duration.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            privacy = privacy.value.toRequestBody("text/plain".toMediaTypeOrNull()),
            clientId = clientId?.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    suspend fun checkUploadStatus(uploadId: Int): Result<ReelStatusResponse> = safeApiCall {
        api.apiV1FeedReelsStatusRetrieve(uploadId)
    }

    suspend fun deleteReel(reelId: Int): Result<ReelDeleteResponse> =
        safeApiCall { api.apiV1FeedReelsDestroy(reelId) }

    suspend fun restoreReel(reelId: Int): Result<ReelRestoreResponse> =
        safeApiCall { api.apiV1FeedReelsRestoreCreate(reelId) }

    suspend fun getReels(
        page: Int? = null,
        pageSize: Int? = null,
        userId: Int? = null
    ): Result<ReelListResponse> =
        safeApiCall { api.apiV1FeedReelsRetrieve(page, pageSize, userId) }

    suspend fun getReel(reelId: Int): Result<ReelDetailResponse> =
        safeApiCall { api.apiV1FeedReelsRetrieve2(reelId) }

    suspend fun searchReels(
        q: String,
        page: Int? = null,
        pageSize: Int? = null,
        userId: Int? = null
    ): Result<ReelSearchResponse> =
        safeApiCall { api.apiV1FeedReelsSearchRetrieve(q, page, pageSize, userId) }

    suspend fun getReelStatistics(reelId: Int): Result<ReelStatisticsResponse> =
        safeApiCall { api.apiV1FeedReelsStatisticsRetrieve(reelId) }

    suspend fun getTrendingReels(
        hours: Int? = null,
        limit: Int? = null,
        minLikes: Int? = null
    ): Result<TrendingReelsResponse> =
        safeApiCall { api.apiV1FeedReelsTrendingRetrieve(hours, limit, minLikes) }

    suspend fun updateReel(reelId: Int, request: ReelUpdateRequest? = null): Result<ReelUpdateResponse> =
        safeApiCall { api.apiV1FeedReelsUpdate(reelId, request) }

    suspend fun getMyReelStatistics(): Result<UserReelStatisticsResponse> =
        safeApiCall { api.apiV1FeedUsersMeReelStatisticsRetrieve() }

    suspend fun getUserReelStatistics(userId: Int): Result<UserReelStatisticsResponse> =
        safeApiCall { api.apiV1FeedUsersReelStatisticsRetrieve(userId) }
}
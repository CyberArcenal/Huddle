package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MediaCreateApi {
    @Multipart
    @POST("api/v1/feed/media/create/")
    suspend fun uploadFeedMedia(
        @Part file: MultipartBody.Part, 
        @Part("order") order: RequestBody? = null, 
        @Part("mimeTypes") mimeTypes: RequestBody? = null
    ): Response<MediaCreateResponse>

    @DELETE("api/v1/feed/media/{media_id}/")
    suspend fun deleteMedia(@Path("media_id") mediaId: kotlin.Int): Response<Unit>

    @GET("api/v1/feed/media/")
    suspend fun feedMediaRetrieve(
        @Query("content_type") contentType: kotlin.String? = null, 
        @Query("group_content_type") groupContentType: kotlin.String? = null, 
        @Query("group_id") groupId: kotlin.Int? = null, 
        @Query("object_id") objectId: kotlin.Int? = null, 
        @Query("order_by") orderBy: kotlin.String? = null, 
        @Query("page") page: kotlin.Int? = null, 
        @Query("page_size") pageSize: kotlin.Int? = null, 
        @Query("post_id") postId: kotlin.Int? = null, 
        @Query("reel_id") reelId: kotlin.Int? = null
    ): Response<PaginatedMedia>

    @GET("api/v1/feed/media/{media_id}/")
    suspend fun apiV1FeedMediaRetrieve2(@Path("media_id") mediaId: kotlin.Int): Response<MediaDisplay>

    @PUT("api/v1/feed/media/{media_id}/")
    suspend fun apiV1FeedMediaUpdate(@Path("media_id") mediaId: kotlin.Int, @Body mediaCreateRequest: MediaCreateRequest): Response<MediaDisplay>
}

class MediaRepository {
    private val api = ApiService.mediaCreateApi

    suspend fun createMedia(request: MediaCreateRequest): Result<MediaCreateResponse> =
        safeApiCall { 
            api.uploadFeedMedia(
                file = request.file, 
                order = request.order?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()), 
                mimeTypes = request.mimeTypes?.toRequestBody("text/plain".toMediaTypeOrNull())
            ) 
        }

    suspend fun deleteMedia(mediaId: Int): Result<Unit> =
        safeApiCall { api.deleteMedia(mediaId) }

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
    ): Result<PaginatedMedia> =
        safeApiCall {
            api.feedMediaRetrieve(
                contentType, groupContentType, groupId, objectId, orderBy,
                page, pageSize, postId, reelId
            )
        }

    suspend fun getMediaById(mediaId: Int): Result<MediaDisplay> =
        safeApiCall { api.apiV1FeedMediaRetrieve2(mediaId) }

    suspend fun updateMedia(mediaId: Int, request: MediaCreateRequest): Result<MediaDisplay> =
        safeApiCall { api.apiV1FeedMediaUpdate(mediaId, request) }
}

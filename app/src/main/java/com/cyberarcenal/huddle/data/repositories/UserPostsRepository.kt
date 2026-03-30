// UserPostsRepository.kt
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
import java.time.OffsetDateTime
import kotlin.jvm.JvmSuppressWildcards

class UserPostsRepository {
    private val api = ApiService.userPostsApi
    private val createPostApi = ApiService.createPostApi

    suspend fun createPost(
        content: String?,
        postType: String,
        privacy: String,
        groupId: Int? = null,
        mediaParts: List<MultipartBody.Part>,
        tagUsers: List<Int>? = null,
        mimeTypes: List<String>? = null
    ): Result<PostCreateResponse> = safeApiCall {
        // Convert lists to List<MultipartBody.Part> to avoid Retrofit's "type variable" error
        // and correctly handle multiple parts with the same name.
        val tagParts = tagUsers?.map { 
            MultipartBody.Part.createFormData("tag_users", it.toString()) 
        }
        val mimeParts = mimeTypes?.map {
            MultipartBody.Part.createFormData("mimeTypes", it)
        }

        createPostApi.postsCreate(
            content = content?.toRequestBody("text/plain".toMediaTypeOrNull()),
            postType = postType.toRequestBody("text/plain".toMediaTypeOrNull()),
            privacy = privacy.toRequestBody("text/plain".toMediaTypeOrNull()),
            group = groupId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
            mimeTypes = mimeParts,
            tagUsers = tagParts,
            media = mediaParts
        )
    }

    // Inayos ang error sa caret position (Cleanup Admin name)
    suspend fun deletePost(postId: Int, hard: Boolean? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response?> =
        safeApiCall { api.apiV1FeedPostsDestroy(postId, hard) }

    suspend fun restorePost(postId: Int): Result<PostRestoreResponse> =
        safeApiCall { api.apiV1FeedPostsRestoreCreate(postId) }

    suspend fun getPosts(feed: Boolean? = null, page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsRetrieve(feed, page, pageSize, userId) }

    suspend fun getPost(postId: Int): Result<PostDisplay> =
        safeApiCall { api.apiV1FeedPostsRetrieve2(postId) }

    suspend fun searchPosts(query: String, page: Int? = null, pageSize: Int? = null, postType: String? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsSearchRetrieve(query, page, pageSize, postType) }

    suspend fun shareToGroup(postId: Int, request: ShareToGroupRequestRequest): Result<PostShareResponse> =
        safeApiCall { api.apiV1FeedPostsShareToGroupCreate(postId, request) }

    suspend fun getPostStatistics(postId: Int): Result<PostStatsSerializers> =
        safeApiCall { api.apiV1FeedPostsStatisticsRetrieve(postId) }

    suspend fun getTrendingPosts(hours: Int? = null, limit: Int? = null, minLikes: Int? = null): Result<Any> =
        safeApiCall { api.apiV1FeedPostsTrendingRetrieve(hours, limit, minLikes) }

    suspend fun updatePost(postId: Int, request: PostCreateRequest? = null): Result<PostDisplay> =
        safeApiCall { api.apiV1FeedPostsUpdate(postId, request) }

    suspend fun getMyPostStatistics(userId: Int? = null): Result<UserPostStatistics> =
        safeApiCall { api.apiV1FeedUsersMePostStatisticsRetrieve(userId) }

    suspend fun getUserPostStatistics(userId: Int, userId2: Int? = null): Result<UserPostStatistics> =
        safeApiCall { api.apiV1FeedUsersPostStatisticsRetrieve(userId, userId2) }

    suspend fun getForYouFeed(page: Int? = null, pageSize: Int? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsRetrieve(true, page, pageSize) }
}

// FIX: Ginamit ang @JvmSuppressWildcards sa List parameters para maiwasan ang 
// "Parameter type must not include a type variable or wildcard" error sa Retrofit.
interface UserCreatePostApi {
    @Multipart
    @POST("api/v1/feed/posts/")
    suspend fun postsCreate(
        @Part("content") content: RequestBody? = null,
        @Part("group") group: RequestBody? = null,
        @Part("post_type") postType: RequestBody? = null,
        @Part("privacy") privacy: RequestBody? = null,
        @Part media: List<@JvmSuppressWildcards MultipartBody.Part>? = null,
        @Part tagUsers: List<@JvmSuppressWildcards MultipartBody.Part>? = null,
        @Part mimeTypes: List<@JvmSuppressWildcards MultipartBody.Part>? = null
    ): Response<PostCreateResponse>
}

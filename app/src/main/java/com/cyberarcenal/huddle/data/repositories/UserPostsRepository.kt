// UserPostsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

/**
 * Custom request for creating posts that includes local file and mime type info
 * which are not part of the auto-generated OpenAPI models.
 */
data class PostCreateRequestWithMedia(
    val content: String?,
    val group: Int? = null,
    val postType: PostType52cEnum,
    val privacy: PrivacyB23Enum,
    val mediaFiles: List<File>? = null,
    val mimeTypes: List<String>? = null
)

class UserPostsRepository {
    private val api = ApiService.userPostsApi
    private val createPostApi = ApiService.createPostApi

    suspend fun createPost(request: PostCreateRequestWithMedia): Result<PostDisplay> = safeApiCall {
        val response = if (request.mediaFiles.isNullOrEmpty()) {
            // 1. Text-only posts using standard JSON request
            val apiRequest = PostCreateRequest(
                content = request.content,
                group = request.group,
                postType = request.postType,
                privacy = request.privacy
            )
            api.apiV1FeedPostsCreate(apiRequest)
        } else {
            // 2. Multipart request for media uploads
            val contentBody = request.content?.toRequestBody("text/plain".toMediaTypeOrNull())
            val postTypeBody = request.postType.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val privacyBody = request.privacy.value.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val mediaParts = request.mediaFiles.mapIndexed { index, file ->
                val mimeType = request.mimeTypes?.getOrNull(index) ?: "image/jpeg"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("media_files", file.name, requestFile)
            }

            createPostApi.createPostMultipart(contentBody, postTypeBody, privacyBody, mediaParts)
        }

        response
    }

    suspend fun deletePost(postId: Int, hard: Boolean? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1FeedPostsDestroy(postId, hard) }

    suspend fun restorePost(postId: Int): Result<Unit> =
        safeApiCall { api.apiV1FeedPostsRestoreCreate(postId) }

    suspend fun getPosts(feed: Boolean? = null, page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsRetrieve(feed, page, pageSize, userId) }

    suspend fun getPost(postId: Int): Result<PostDetail> =
        safeApiCall { api.apiV1FeedPostsRetrieve2(postId) }

    suspend fun searchPosts(query: String, page: Int? = null, pageSize: Int? = null, postType: String? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsSearchRetrieve(query, page, pageSize, postType) }

    suspend fun shareToGroup(postId: Int, request: ShareToGroupRequestRequest): Result<PostDisplay> =
        safeApiCall { api.apiV1FeedPostsShareToGroupCreate(postId, request) }

    suspend fun getPostStatistics(postId: Int): Result<PostStatistics> =
        safeApiCall { api.apiV1FeedPostsStatisticsRetrieve(postId) }

    suspend fun getTrendingPosts(hours: Int? = null, limit: Int? = null, minLikes: Int? = null): Result<ApiV1FeedPostsTrendingRetrieve200Response> =
        safeApiCall { api.apiV1FeedPostsTrendingRetrieve(hours, limit, minLikes) }

    suspend fun updatePost(postId: Int, request: PostCreateRequest? = null): Result<PostDisplay> =
        safeApiCall { api.apiV1FeedPostsUpdate(postId, request) }

    suspend fun getMyPostStatistics(userId: Int? = null): Result<UserPostStatistics> =
        safeApiCall { api.apiV1FeedUsersMePostStatisticsRetrieve(userId) }

    suspend fun getUserPostStatistics(userId: Int, userId2: Int? = null): Result<UserPostStatistics> =
        safeApiCall { api.apiV1FeedUsersPostStatisticsRetrieve(userId, userId2) }

    suspend fun getForYouFeed(page: Int? = null, pageSize: Int? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsRetrieve(true, page, pageSize) }

    suspend fun getFollowingFeed(page: Int? = null, pageSize: Int? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsRetrieve(true, page, pageSize) }

    suspend fun getDiscoverFeed(page: Int? = null, pageSize: Int? = null): Result<PaginatedPostFeed> =
        safeApiCall { api.apiV1FeedPostsRetrieve(true, page, pageSize) }
}

// Custom API interface for handling Multipart media uploads
interface UserCreatePostApi {
    @Multipart
    @POST("api/v1/feed/posts/")
    suspend fun createPostMultipart(
        @Part("content") content: okhttp3.RequestBody?,
        @Part("post_type") postType: okhttp3.RequestBody,
        @Part("privacy") privacy: okhttp3.RequestBody,
        @Part mediaFiles: List<MultipartBody.Part>
    ): Response<PostDisplay>
}

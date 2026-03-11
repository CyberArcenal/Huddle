package com.cyberarcenal.huddle.data.repositories.feed

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.time.OffsetDateTime

class FeedRepository {
    private val api = ApiService.v1Api
    private val multipartApi: FeedMultipartApi by lazy {
        ApiService.retrofit.create(FeedMultipartApi::class.java)
    }

    // ========== POSTS ==========

    suspend fun getFeedPosts(
        feed: Boolean? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPostFeed> = safeApiCall {
        api.v1FeedPostsRetrieve(feed = feed, userId = userId, page = page, pageSize = pageSize)
    }

    suspend fun createPost(
        post: PostCreate,
        media: MultipartBody.Part? = null
    ): Result<Post> = safeApiCall {
        api.v1FeedPostsCreate(post)
    }

    suspend fun getPost(postId: Int): Result<PostDetail> = safeApiCall {
        api.v1FeedPostsRetrieve2(postId)
    }

    suspend fun updatePost(postId: Int, post: Post): Result<Post> = safeApiCall {
        api.v1FeedPostsUpdate(postId, post)
    }

    suspend fun deletePost(postId: Int, hard: Boolean? = null): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1FeedPostsDestroy(postId, hard)
    }

    suspend fun restorePost(postId: Int): Result<PostRestoreResponse> = safeApiCall {
        api.v1FeedPostsRestoreCreate(postId)
    }

    suspend fun getPostStatistics(postId: Int): Result<PostStatistics> = safeApiCall {
        api.v1FeedPostsStatisticsRetrieve(postId)
    }

    suspend fun searchPosts(
        query: String,
        postType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPostFeed> = safeApiCall {
        api.v1FeedPostsSearchRetrieve(query, page, pageSize, postType)
    }

    suspend fun getTrendingPosts(
        hours: Int? = null,
        limit: Int? = null,
        minLikes: Int? = null
    ): Result<V1FeedPostsTrendingRetrieve200Response> = safeApiCall {
        api.v1FeedPostsTrendingRetrieve(hours, limit, minLikes)
    }

    // ========== LIKES ==========

    suspend fun toggleLike(postId: Int): Result<V1FeedLikesToggleCreate200Response> {
        val request = LikeToggle(contentType = "post", objectId = postId)
        return safeApiCall { api.v1FeedLikesToggleCreate(request) }
    }

    suspend fun getLikes(
        contentType: String,
        objectId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedLike> = safeApiCall {
        api.v1FeedLikesRetrieve2(contentType, objectId, page, pageSize)
    }

    suspend fun checkLike(
        contentType: String,
        objectId: Int
    ): Result<V1FeedLikesCheckRetrieve200Response> = safeApiCall {
        api.v1FeedLikesCheckRetrieve(contentType, objectId)
    }

    suspend fun getMostLiked(
        contentType: String,
        days: Int? = null,
        limit: Int? = null
    ): Result<V1FeedLikesMostLikedRetrieve200Response> = safeApiCall {
        api.v1FeedLikesMostLikedRetrieve(contentType, days, limit)
    }

    suspend fun getMutualLikes(userId: Int): Result<V1FeedLikesMutualRetrieve200Response> = safeApiCall {
        api.v1FeedLikesMutualRetrieve(userId)
    }

    suspend fun getRecentLikers(
        contentType: String,
        objectId: Int,
        limit: Int? = null
    ): Result<V1FeedLikesRecentRetrieve200Response> = safeApiCall {
        api.v1FeedLikesRecentRetrieve(contentType, objectId, limit)
    }

    suspend fun getUserLikeStatistics(userId: Int? = null): Result<Any> = safeApiCall {
        api.v1FeedLikesStatisticsRetrieve(userId)
    }

    // ========== COMMENTS ==========

    suspend fun getComments(
        postId: Int,
        includeDeleted: Boolean? = null,
        includeReplies: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> = safeApiCall {
        api.v1FeedCommentsRetrieve(postId, includeDeleted, includeReplies, page, pageSize)
    }

    suspend fun createComment(comment: CommentCreate): Result<Comment> = safeApiCall {
        api.v1FeedCommentsCreate(comment)
    }

    suspend fun getComment(commentId: Int): Result<Comment> = safeApiCall {
        api.v1FeedCommentsRetrieve2(commentId)
    }

    suspend fun updateComment(commentId: Int, comment: CommentCreate): Result<Comment> = safeApiCall {
        api.v1FeedCommentsUpdate(commentId, comment)
    }

    suspend fun deleteComment(commentId: Int): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1FeedCommentsDestroy(commentId)
    }

    suspend fun getReplies(
        commentId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> = safeApiCall {
        api.v1FeedCommentsRepliesRetrieve(commentId, page, pageSize)
    }

    suspend fun createReply(commentId: Int, comment: Comment): Result<Comment> = safeApiCall {
        api.v1FeedCommentsRepliesCreate(commentId, comment)
    }

    suspend fun getCommentThread(commentId: Int): Result<V1FeedCommentsThreadRetrieve200Response> = safeApiCall {
        api.v1FeedCommentsThreadRetrieve(commentId)
    }

    suspend fun searchComments(
        query: String,
        postId: Int? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> = safeApiCall {
        api.v1FeedCommentsSearchRetrieve(query, page, pageSize, postId, userId)
    }

    // ========== USER POST STATISTICS ==========

    suspend fun getCurrentUserPostStatistics(): Result<UserPostStatistics> = safeApiCall {
        api.v1FeedUsersMePostStatisticsRetrieve()
    }

    suspend fun getUserPostStatistics(userId: Int): Result<UserPostStatistics> = safeApiCall {
        api.v1FeedUsersPostStatisticsRetrieve(userId)
    }

    suspend fun createTextPost(content: String, privacyEnum: PrivacyB23Enum): Result<Post> {
        val post = PostCreate(
            content = content,
            postType = PostTypeEnum.TEXT,
            privacy = privacyEnum
        )
        return createPost(post)
    }

    /**
     * Create a post with multiple media files using multipart upload.
     * Matches the working pattern from StoriesRepository.
     */
    suspend fun createPostWithMedia(
        content: String,
        privacy: PrivacyB23Enum,
        postType: PostTypeEnum,
        mediaFiles: List<File>,
        mimeTypes: List<String>
    ): Result<Post> = safeApiCall {
        // Convert text fields to RequestBody
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val privacyBody = privacy.value.toRequestBody("text/plain".toMediaTypeOrNull())
        val postTypeBody = postType.value.toRequestBody("text/plain".toMediaTypeOrNull())

        // Create file parts – each part named "media_file" (backend expects this name for each file)
        val fileParts = mediaFiles.mapIndexed { index, file ->
            val mimeType = mimeTypes.getOrElse(index) { "image/jpeg" }
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(
                name = "media_files", // Must match backend field name
                filename = file.name,
                body = requestFile
            )
        }

        multipartApi.createPost(contentBody, privacyBody, postTypeBody, fileParts)
    }
}

interface FeedMultipartApi {
    @Multipart
    @POST("api/v1/feed/posts/")
    suspend fun createPost(
        @Part("content") content: RequestBody,
        @Part("privacy") privacy: RequestBody,
        @Part("post_type") postType: RequestBody,
        @Part mediaFiles: List<MultipartBody.Part>
    ): Response<Post>
}
package com.cyberarcenal.huddle.data.repositories.feed

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody
import java.time.OffsetDateTime

class FeedRepository {
    private val api = ApiService.v1Api

    // ========== POSTS ==========

    /**
     * Get feed posts (personalized feed, public posts, or by user).
     * @param feed If true, returns personalized feed for authenticated user.
     * @param userId Filter by user ID.
     * @param page Page number.
     * @param pageSize Results per page.
     */
    suspend fun getFeedPosts(
        feed: Boolean? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPostFeed> = safeApiCall {
        api.v1FeedPostsRetrieve(feed = feed, userId = userId, page = page, pageSize = pageSize)
    }

    /**
     * Create a new post.
     * @param post The post data (without media).
     * @param media Optional media file.
     */
    suspend fun createPost(
        post:  PostCreate,
        media: MultipartBody.Part? = null
    ): Result<Post> {
        // Note: The generated API uses @Multipart for creation.
        // We need to adapt – in the generated V1Api, v1FeedPostsCreate expects a @Body Post,
        // but the spec also shows multipart for media. This is inconsistent.
        // Based on the generated code, v1FeedPostsCreate is a JSON POST, not multipart.
        // The multipart is for v1Messaging... So we'll use the JSON version.
        return safeApiCall { api.v1FeedPostsCreate(post) }
    }

    /**
     * Get a single post by ID.
     */
    suspend fun getPost(postId: Int): Result<PostDetail> = safeApiCall {
        api.v1FeedPostsRetrieve2(postId)
    }

    /**
     * Update a post (full update).
     */
    suspend fun updatePost(postId: Int, post: Post): Result<Post> = safeApiCall {
        api.v1FeedPostsUpdate(postId, post)
    }

    /**
     * Delete a post (soft delete by default).
     * @param hard If true, permanently delete.
     */
    suspend fun deletePost(postId: Int, hard: Boolean? = null): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1FeedPostsDestroy(postId, hard)
    }

    /**
     * Restore a soft-deleted post.
     */
    suspend fun restorePost(postId: Int): Result<PostRestoreResponse> = safeApiCall {
        api.v1FeedPostsRestoreCreate(postId)
    }

    /**
     * Get statistics for a post.
     */
    suspend fun getPostStatistics(postId: Int): Result<PostStatistics> = safeApiCall {
        api.v1FeedPostsStatisticsRetrieve(postId)
    }

    /**
     * Search posts by content.
     */
    suspend fun searchPosts(
        query: String,
        postType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPostFeed> = safeApiCall {
        api.v1FeedPostsSearchRetrieve(query, page, pageSize, postType)
    }

    /**
     * Get trending posts (most liked within a time window).
     */
    suspend fun getTrendingPosts(
        hours: Int? = null,
        limit: Int? = null,
        minLikes: Int? = null
    ): Result<V1FeedPostsTrendingRetrieve200Response> = safeApiCall {
        api.v1FeedPostsTrendingRetrieve(hours, limit, minLikes)
    }

    // ========== LIKES ==========

    /**
     * Toggle like on a post.
     */
    suspend fun toggleLike(postId: Int): Result<V1FeedLikesToggleCreate200Response> {
        val request = LikeToggle(contentType = "post", objectId = postId)
        return safeApiCall { api.v1FeedLikesToggleCreate(request) }
    }

    /**
     * Get likes for a specific object (post or comment).
     */
    suspend fun getLikes(
        contentType: String,
        objectId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedLike> = safeApiCall {
        api.v1FeedLikesRetrieve2(contentType, objectId, page, pageSize)
    }

    /**
     * Check if the current user has liked an object and get total like count.
     */
    suspend fun checkLike(
        contentType: String,
        objectId: Int
    ): Result<V1FeedLikesCheckRetrieve200Response> = safeApiCall {
        api.v1FeedLikesCheckRetrieve(contentType, objectId)
    }

    /**
     * Get most liked content of a specific type within a time period.
     */
    suspend fun getMostLiked(
        contentType: String,
        days: Int? = null,
        limit: Int? = null
    ): Result<V1FeedLikesMostLikedRetrieve200Response> = safeApiCall {
        api.v1FeedLikesMostLikedRetrieve(contentType, days, limit)
    }

    /**
     * Get mutual likes between current user and another user.
     */
    suspend fun getMutualLikes(userId: Int): Result<V1FeedLikesMutualRetrieve200Response> = safeApiCall {
        api.v1FeedLikesMutualRetrieve(userId)
    }

    /**
     * Get recent likers of an object.
     */
    suspend fun getRecentLikers(
        contentType: String,
        objectId: Int,
        limit: Int? = null
    ): Result<V1FeedLikesRecentRetrieve200Response> = safeApiCall {
        api.v1FeedLikesRecentRetrieve(contentType, objectId, limit)
    }

    /**
     * Get like statistics for a user.
     */
    suspend fun getUserLikeStatistics(userId: Int? = null): Result<Any> = safeApiCall {
        api.v1FeedLikesStatisticsRetrieve(userId)
    }

    // ========== COMMENTS ==========

    /**
     * Get comments for a post.
     */
    suspend fun getComments(
        postId: Int,
        includeDeleted: Boolean? = null,
        includeReplies: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> = safeApiCall {
        api.v1FeedCommentsRetrieve(postId, includeDeleted, includeReplies, page, pageSize)
    }

    /**
     * Create a new comment on a post.
     */
    suspend fun createComment(comment: CommentCreate): Result<Comment> = safeApiCall {
        api.v1FeedCommentsCreate(comment)
    }

    /**
     * Get a single comment by ID.
     */
    suspend fun getComment(commentId: Int): Result<Comment> = safeApiCall {
        api.v1FeedCommentsRetrieve2(commentId)
    }

    /**
     * Update a comment.
     */
    suspend fun updateComment(commentId: Int, comment: CommentCreate): Result<Comment> = safeApiCall {
        api.v1FeedCommentsUpdate(commentId, comment)
    }

    /**
     * Delete a comment (soft delete).
     */
    suspend fun deleteComment(commentId: Int): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1FeedCommentsDestroy(commentId)
    }

    /**
     * Get replies to a comment.
     */
    suspend fun getReplies(
        commentId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> = safeApiCall {
        api.v1FeedCommentsRepliesRetrieve(commentId, page, pageSize)
    }

    /**
     * Create a reply to a comment.
     */
    suspend fun createReply(commentId: Int, comment: Comment): Result<Comment> = safeApiCall {
        api.v1FeedCommentsRepliesCreate(commentId, comment)
    }

    /**
     * Get the full thread of a comment.
     */
    suspend fun getCommentThread(commentId: Int): Result<V1FeedCommentsThreadRetrieve200Response> = safeApiCall {
        api.v1FeedCommentsThreadRetrieve(commentId)
    }

    /**
     * Search comments.
     */
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

    /**
     * Get post statistics for the current user.
     */
    suspend fun getCurrentUserPostStatistics(): Result<UserPostStatistics> = safeApiCall {
        api.v1FeedUsersMePostStatisticsRetrieve()
    }

    /**
     * Get post statistics for a specific user.
     */
    suspend fun getUserPostStatistics(userId: Int): Result<UserPostStatistics> = safeApiCall {
        api.v1FeedUsersPostStatisticsRetrieve(userId)
    }

    suspend fun createTextPost(content: String, privacyEnum: PrivacyB23Enum): Result<Post> {
        val post =  PostCreate(
            content = content,
            postType = PostTypeEnum.TEXT,
            mediaUrl = null,
            privacy = privacyEnum
        )
        return createPost(post)
    }
}
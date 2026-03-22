// CommentsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class CommentsRepository {
    private val api = ApiService.commentsApi

    /**
     * Create a new comment on any content object.
     * @param request Must contain target_type, target_id, and content.
     */
    suspend fun createComment(request: CommentCreateRequest): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsCreate(request) }

    /**
     * Delete a comment (soft delete).
     */
    suspend fun deleteComment(commentId: Int): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1FeedCommentsDestroy(commentId) }

    /**
     * Create a reply to an existing comment.
     * @param parentCommentId ID of the comment being replied to.
     * @param request Must contain target_type, target_id (same as parent comment), and content.
     */
    suspend fun createReply(
        parentCommentId: Int,
        request: CommentCreateRequest
    ): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsRepliesCreate(parentCommentId, request) }

    /**
     * Get paginated replies of a specific comment.
     */
    suspend fun getReplies(
        commentId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedCommentsRepliesRetrieve(commentId, page, pageSize) }

    /**
     * Get a single comment by ID.
     */
    suspend fun getComment(commentId: Int): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsRetrieve2(commentId) }

    /**
     * Update a comment (partial or full).
     */
    suspend fun updateComment(
        commentId: Int,
        request: CommentCreateRequest
    ): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsUpdate(commentId, request) }

    /**
     * List comments for a given content object.
     * @param contentType e.g., "post", "reel"
     * @param objectId ID of the target object
     * @param includeDeleted include soft-deleted comments (requires permission)
     * @param includeReplies include nested replies (default true)
     * @param page pagination page
     * @param pageSize items per page
     */
    suspend fun getCommentsForObject(
        contentType: String,
        objectId: Int,
        includeDeleted: Boolean? = false,
        includeReplies: Boolean? = true,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> =
        safeApiCall {
            api.apiV1FeedCommentsObjectRetrieve(
                contentType = contentType,
                objectId = objectId,
                includeDeleted = includeDeleted,
                includeReplies = includeReplies,
                page = page,
                pageSize = pageSize
            )
        }

    /**
     * List comments created by the authenticated user.
     */
    suspend fun getMyComments(
        includeDeleted: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedCommentsRetrieve(includeDeleted, page, pageSize) }

    /**
     * Search comments by content, optionally filtered by user or target object.
     * @param query search term
     * @param contentType filter by content type
     * @param objectId filter by object ID (requires contentType)
     * @param userId filter by user who wrote the comment
     * @param page pagination
     * @param pageSize items per page
     */
    suspend fun searchComments(
        query: String,
        contentType: String? = null,
        objectId: Int? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> =
        safeApiCall {
            api.apiV1FeedCommentsSearchRetrieve(
                query = query,
                contentType = contentType,
                objectId = objectId,
                page = page,
                pageSize = pageSize,
                userId = userId
            )
        }
}
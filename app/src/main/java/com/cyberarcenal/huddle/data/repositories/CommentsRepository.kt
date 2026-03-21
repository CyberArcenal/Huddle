// CommentsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class CommentsRepository {
    private val api = ApiService.commentsApi

    suspend fun createComment(request: CommentCreateRequest): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsCreate(request) }

    suspend fun deleteComment(commentId: Int): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1FeedCommentsDestroy(commentId) }

    suspend fun createReply(commentId: Int, request: CommentCreateRequest): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsRepliesCreate(commentId, request) }

    suspend fun getReplies(commentId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedCommentsRepliesRetrieve(commentId, page, pageSize) }

    suspend fun getComments(
        postId: Int,
        includeDeleted: Boolean? = null,
        includeReplies: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedCommentsRetrieve(postId, includeDeleted, includeReplies, page, pageSize) }

    suspend fun getComment(commentId: Int): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsRetrieve2(commentId) }

    suspend fun searchComments(query: String, page: Int? = null, pageSize: Int? = null, postId: Int? = null, userId: Int? = null): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedCommentsSearchRetrieve(query, page, pageSize, postId, userId) }

    suspend fun getCommentThread(commentId: Int): Result<ApiV1FeedCommentsThreadRetrieve200Response> =
        safeApiCall { api.apiV1FeedCommentsThreadRetrieve(commentId) }

    suspend fun updateComment(commentId: Int, request: CommentCreateRequest): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedCommentsUpdate(commentId, request) }

    suspend fun createCommentOnPost(postId: Int, request: CommentCreateRequest): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedPostsCommentsCreate(postId, request) }

    suspend fun createReplyOnPost(postId: Int, commentId: Int, request: CommentCreateRequest): Result<CommentDisplay> =
        safeApiCall { api.apiV1FeedPostsCommentsRepliesCreate(commentId, postId, request) }

    suspend fun getRepliesOnPost(postId: Int, commentId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedPostsCommentsRepliesRetrieve(commentId, postId, page, pageSize) }

    suspend fun getCommentsOnPost(
        postId: Int,
        includeDeleted: Boolean? = null,
        includeReplies: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedComment> =
        safeApiCall { api.apiV1FeedPostsCommentsRetrieve(postId, includeDeleted, includeReplies, page, pageSize) }
}
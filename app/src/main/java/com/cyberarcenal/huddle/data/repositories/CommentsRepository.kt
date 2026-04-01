package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class CommentsRepository {
    private val api = ApiService.commentsApi

    suspend fun createComment(request: CommentCreateRequest): Result<CommentCreateResponse> =
        safeApiCall { api.apiV1FeedCommentsCreate(request) }

    suspend fun deleteComment(commentId: Int): Result<CommentDeleteResponse> =
        safeApiCall { api.apiV1FeedCommentsDestroy(commentId) }

    suspend fun createReply(
        parentCommentId: Int,
        request: CommentCreateRequest
    ): Result<CommentReplyResponse> =
        safeApiCall { api.apiV1FeedCommentsRepliesCreate(parentCommentId, request) }

    suspend fun getReplies(
        commentId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<CommentListResponse> =
        safeApiCall { api.apiV1FeedCommentsRepliesRetrieve(commentId, page, pageSize) }

    suspend fun getComment(commentId: Int): Result<CommentDetailResponse> =
        safeApiCall { api.apiV1FeedCommentsRetrieve2(commentId) }

    suspend fun updateComment(
        commentId: Int,
        request: CommentCreateRequest
    ): Result<CommentUpdateResponse> =
        safeApiCall { api.apiV1FeedCommentsUpdate(commentId, request) }

    suspend fun getCommentsForObject(
        contentType: String,
        objectId: Int,
        includeDeleted: Boolean? = false,
        includeReplies: Boolean? = true,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<CommentListResponse> =
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

    suspend fun getMyComments(
        includeDeleted: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<CommentListResponse> =
        safeApiCall { api.apiV1FeedCommentsRetrieve(includeDeleted, page, pageSize) }

    suspend fun searchComments(
        query: String,
        contentType: String? = null,
        objectId: Int? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<CommentListResponse> =
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
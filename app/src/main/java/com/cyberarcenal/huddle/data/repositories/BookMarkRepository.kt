package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.BookmarksApi
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class BookmarksRepository {
    private val api: BookmarksApi = ApiService.bookmarksApi

    suspend fun createBookmark(request: BookmarkActionRequest): Result<BookmarkCreateResponse> =
        safeApiCall { api.apiV1FeedBookmarksActionCreate(request) }

    suspend fun deleteBookmark(): Result<BookmarkDeleteResponse> =
        safeApiCall { api.apiV1FeedBookmarksActionDestroy() }

    suspend fun getBookmarks(
        contentType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<BookmarkListResponse> =
        safeApiCall { api.apiV1FeedBookmarksRetrieve(contentType, page, pageSize) }

    suspend fun getStatistics(targetId: Int, targetType: String): Result<BookmarkStatisticsResponse> =
        safeApiCall { api.apiV1FeedBookmarksStatisticsRetrieve(targetId, targetType) }

    suspend fun getTop(contentType: String? = null, limit: Int? = null): Result<BookmarkTopResponse> =
        safeApiCall { api.apiV1FeedBookmarksTopRetrieve(contentType, limit) }
}
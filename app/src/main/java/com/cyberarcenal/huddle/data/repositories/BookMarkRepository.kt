package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.BookmarksApi
import com.cyberarcenal.huddle.api.models.BookmarkActionRequest
import com.cyberarcenal.huddle.api.models.BookmarkDeleteResponse
import com.cyberarcenal.huddle.api.models.BookmarkDisplay
import com.cyberarcenal.huddle.api.models.BookmarkMinimal
import com.cyberarcenal.huddle.api.models.BookmarkResponse
import com.cyberarcenal.huddle.api.models.BookmarkStatistics
import com.cyberarcenal.huddle.api.models.PaginatedBookmarkMinimal
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class BookmarksRepository {
    private val api = ApiService.bookmarksApi

    /**
     * Create a bookmark for the given object.
     */
    suspend fun createBookmark(request: BookmarkActionRequest): Result<BookmarkResponse> = safeApiCall {
        api.apiV1FeedBookmarksActionCreate(request)
    }

    /**
     * Remove a bookmark for the given object.
     */
    suspend fun deleteBookmark(): Result<BookmarkDeleteResponse> = safeApiCall {
        api.apiV1FeedBookmarksActionDestroy()
    }

    /**
     * Get all bookmarks created by the current user.
     */
    suspend fun getBookmarks(
        contentType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedBookmarkMinimal> = safeApiCall {
        api.apiV1FeedBookmarksRetrieve(contentType, page, pageSize)
    }

    /**
     * Get total bookmarks for the object and whether the current user has bookmarked it.
     */
    suspend fun getStatistics(targetId: Int, targetType: String): Result<BookmarkStatistics> = safeApiCall {
        api.apiV1FeedBookmarksStatisticsRetrieve(targetId, targetType)
    }

    /**
     * Get the most bookmarked objects (Admin only).
     */
    suspend fun getTop(contentType: String? = null, limit: Int? = null): Result<List<BookmarkMinimal>> = safeApiCall {
        api.apiV1FeedBookmarksTopList(contentType, limit)
    }
}
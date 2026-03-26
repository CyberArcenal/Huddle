package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.ViewsApi
import com.cyberarcenal.huddle.api.models.PaginatedViewMinimal
import com.cyberarcenal.huddle.api.models.ViewCreateRequest
import com.cyberarcenal.huddle.api.models.ViewDisplay
import com.cyberarcenal.huddle.api.models.ViewStatistics
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class ViewsRepository {
    private val api = ApiService.viewsApi

    /**
     * Get the current user's view history.
     */
    suspend fun getHistory(
        contentType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedViewMinimal> = safeApiCall {
        api.apiV1FeedViewsHistoryRetrieve(contentType, page, pageSize)
    }

    /**
     * Record a view for a content object.
     */
    suspend fun recordView(request: ViewCreateRequest): Result<ViewDisplay> = safeApiCall {
        api.apiV1FeedViewsRecordCreate(request)
    }

    /**
     * Get aggregated view statistics for a content object.
     */
    suspend fun getStatistics(targetId: Int, targetType: String): Result<ViewStatistics> = safeApiCall {
        api.apiV1FeedViewsStatisticsRetrieve(targetId, targetType)
    }

    /**
     * Get the most viewed objects (Admin only).
     */
    suspend fun getTop(contentType: String? = null, limit: Int? = null): Result<List<ViewStatistics>> = safeApiCall {
        api.apiV1FeedViewsTopList(contentType, limit)
    }
}
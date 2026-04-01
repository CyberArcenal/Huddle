package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class ViewsRepository {
    private val api = ApiService.viewsApi

    suspend fun getHistory(
        contentType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<ViewHistoryResponse> = safeApiCall {
        api.apiV1FeedViewsHistoryRetrieve(contentType, page, pageSize)
    }

    suspend fun recordView(request: ViewCreateRequest): Result<ViewRecordResponse> = safeApiCall {
        api.apiV1FeedViewsRecordCreate(request)
    }

    suspend fun getStatistics(targetId: Int, targetType: String): Result<ViewStatisticsResponse> = safeApiCall {
        api.apiV1FeedViewsStatisticsRetrieve(targetId, targetType)
    }

    suspend fun getTop(contentType: String? = null, limit: Int? = null): Result<TopViewedResponse> = safeApiCall {
        api.apiV1FeedViewsTopRetrieve(contentType, limit)
    }
}
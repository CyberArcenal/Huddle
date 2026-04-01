package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.FeedResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class FeedRepository {
    private val api = ApiService.feedApi

    suspend fun getFeed(
        feedType: String? = "home",
        page: Int? = null,
        pageSize: Int? = null,
        postsPreview: Int? = null,
        sharesPreview: Int? = null
    ): Result<FeedResponse> =
        safeApiCall {
            api.apiV1FeedFeedRetrieve(feedType, page, pageSize, postsPreview, sharesPreview)
        }
}
// UserContentRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.UserContentApi
import com.cyberarcenal.huddle.api.models.UnifiedContentItem
import com.cyberarcenal.huddle.api.models.UserContentFeedResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserContentRepository(
    private val api: UserContentApi = ApiService.userContentApi
) {
    suspend fun getMyContent(
        page: Int? = null,
        page_size: Int?=null,
    ): Result<UserContentFeedResponse> =
        safeApiCall { api.apiV1FeedMeContentRetrieve(page, page_size) }

    suspend fun getUserContent(
        userId: Int,
        page: Int? = null,
        page_size: Int?=null,
    ): Result<UserContentFeedResponse> =
        safeApiCall { api.apiV1FeedUsersContentRetrieve(userId, page, page_size) }
}
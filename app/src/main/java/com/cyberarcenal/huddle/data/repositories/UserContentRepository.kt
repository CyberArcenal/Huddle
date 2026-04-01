package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.UserContentApi
import com.cyberarcenal.huddle.api.models.UserContentResponse
import com.cyberarcenal.huddle.api.models.LikedItemsResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserContentRepository {
    private val api: UserContentApi = ApiService.userContentApi

    suspend fun getMyContent(page: Int? = null, pageSize: Int? = null): Result<UserContentResponse> =
        safeApiCall { api.apiV1FeedMeContentRetrieve(page, pageSize) }

    suspend fun getUserContent(userId: Int, page: Int? = null, pageSize: Int? = null): Result<UserContentResponse> =
        safeApiCall { api.apiV1FeedUsersContentRetrieve(userId, page, pageSize) }

    suspend fun getMyLikedItems(page: Int? = null, pageSize: Int? = null): Result<LikedItemsResponse> =
        safeApiCall { api.apiV1FeedMeLikedRetrieve(page, pageSize) }

    suspend fun getUserLikedItems(userId: Int, page: Int? = null, pageSize: Int? = null): Result<LikedItemsResponse> =
        safeApiCall { api.apiV1FeedUsersLikedRetrieve(userId, page, pageSize) }
}
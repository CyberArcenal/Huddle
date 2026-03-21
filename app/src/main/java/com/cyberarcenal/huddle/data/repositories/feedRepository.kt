package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.FeedResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class FeedRepository {
    private val api = ApiService.feedApi
//    "Type of feed: "posts", "reels", "stories", "suggested_users", "match_users",
//    "recommended_groups", "shares", "events", "group_posts",
//    "following_posts", "friends_posts", "other" "
//    "Controls which rows are included and their titles."
    suspend fun getFeed(feedType: String = "home",page: Int? = null, pageSize: Int? = null,
                        postPreview: Int? = null, sharesPreview: Int? = null):
            Result<FeedResponse> =
        safeApiCall { api.apiV1FeedFeedRetrieve(feedType, page, pageSize) }
 }
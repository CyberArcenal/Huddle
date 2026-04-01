package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserMatchingRepository {
    private val api = ApiService.userMatchingApi

    suspend fun getFriendSuggestions(
        limitMatches: Int? = null,
        limitSocial: Int? = null,
        maxAge: Int? = null,
        maxDistanceKm: Double? = null,
        minAge: Int? = null,
        offsetMatches: Int? = null,
        offsetSocial: Int? = null
    ): Result<FriendSuggestionsResponse> =
        safeApiCall {
            api.apiV1UsersFriendSuggestionsRetrieve(
                limitMatches, limitSocial, maxAge, maxDistanceKm, minAge,
                offsetMatches, offsetSocial
            )
        }

    suspend fun getMatches(
        limit: Int? = null,
        maxAge: Int? = null,
        maxDistanceKm: Double? = null,
        minAge: Int? = null,
        offset: Int? = null
    ): Result<MatchScoresResponse> =
        safeApiCall { api.apiV1DatingRetrieve(limit, maxAge, maxDistanceKm, minAge, offset) }
}
// GroupSuggestionRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class GroupSuggestionRepository {
    private val api = ApiService.groupSuggestionApi

    suspend fun getGroupSuggestions(limit: Int? = null, offset: Int? = null): Result<PaginatedGroupSuggestion> =
        safeApiCall { api.apiV1GroupsSuggestionsRetrieve(limit, offset) }
}
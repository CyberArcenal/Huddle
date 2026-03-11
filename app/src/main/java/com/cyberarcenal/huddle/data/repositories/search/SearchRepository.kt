package com.cyberarcenal.huddle.data.repositories.search

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class SearchRepository {
    private val api = ApiService.v1Api

    suspend fun getUserSearch(
        query: String,
        types: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSearchResult> = safeApiCall {
        api.v1UsersSearchUsersRetrieve(query, page, pageSize)
    }

    suspend fun getGroupSearch(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedGroupSearch> = safeApiCall {
        api.v1SearchGroupsRetrieve(query, page, pageSize)
    }

    suspend fun getPostSearch(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedPostSearch> = safeApiCall {
        api.v1SearchPostsRetrieve(query, page, pageSize)
    }

    suspend fun getEventSearch(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventSearch> = safeApiCall {
        api.v1SearchEventsRetrieve(query, page, pageSize)
    }

    // ========== SEARCH HISTORY ==========

    suspend fun getSearchHistory(
        days: Int? = null,
        searchType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSearchHistory> = safeApiCall {
        api.v1SearchHistoryRetrieve(days, page, pageSize, searchType)
    }

    suspend fun recordSearch(
        query: String,
        searchType: SearchTypeEnum? = null,
        resultsCount: Int? = null
    ): Result<SearchHistory> {
        val request = RecordSearchInputRequest(query = query, searchType = searchType, resultsCount = resultsCount)
        return safeApiCall { api.v1SearchHistoryCreate(request) }
    }

    suspend fun clearSearchHistory(): Result<V1SearchHistoryDestroy200Response> = safeApiCall {
        api.v1SearchHistoryDestroy()
    }

    suspend fun deleteSearchHistoryEntry(entryId: Int): Result<V1SearchHistoryDestroy2200Response> = safeApiCall {
        api.v1SearchHistoryDestroy2(entryId)
    }

    // ========== POPULAR SEARCHES ==========

    suspend fun getPopularSearches(
        days: Int? = null,
        limit: Int? = null,
        searchType: String? = null,
        userOnly: Boolean? = null
    ): Result<PaginatedSearchHistory> = safeApiCall {
        api.v1SearchPopularRetrieve(days, limit, searchType, userOnly)
    }

    // ========== RECENT SEARCHES ==========

    suspend fun getRecentSearches(
        limit: Int? = null,
        unique: Boolean? = null
    ): Result<V1SearchRecentRetrieve200Response> = safeApiCall {
        api.v1SearchRecentRetrieve(limit, unique)
    }

    // ========== SUGGESTIONS ==========

    suspend fun getSuggestions(
        prefix: String,
        includeAnonymous: Boolean? = null,
        limit: Int? = null
    ): Result<V1SearchSuggestionsRetrieve200Response> = safeApiCall {
        api.v1SearchSuggestionsRetrieve(prefix, includeAnonymous, limit)
    }

    suspend fun getSearchSuggestions(
        prefix: String,
        includeAnonymous: Boolean? = null,
        limit: Int? = null
    ): Result<V1SearchSuggestionsRetrieve200Response> = getSuggestions(prefix, includeAnonymous, limit)

    // ========== SEARCH STATISTICS ==========

    suspend fun getSearchStatistics(days: Int? = null): Result<SearchStatistics> = safeApiCall {
        api.v1SearchStatisticsRetrieve(days)
    }

    // ========== SEARCH TRENDS ==========

    suspend fun getSearchTrends(
        days: Int? = null,
        interval: String? = null
    ): Result<V1SearchTrendsRetrieve200Response> = safeApiCall {
        api.v1SearchTrendsRetrieve(days, interval)
    }

    // ========== EXPORT ==========

    suspend fun exportSearchHistory(
        format: String? = null,
        includeMetadata: Boolean? = null
    ): Result<Any> = safeApiCall {
        api.v1SearchExportRetrieve(format, includeMetadata)
    }
}
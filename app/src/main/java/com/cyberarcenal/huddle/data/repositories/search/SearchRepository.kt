package com.cyberarcenal.huddle.data.repositories.search

import com.cyberarcenal.huddle.api.models.PaginatedSearchHistory
import com.cyberarcenal.huddle.api.models.RecordSearchInput
import com.cyberarcenal.huddle.api.models.SearchHistory
import com.cyberarcenal.huddle.api.models.SearchStatistics
import com.cyberarcenal.huddle.api.models.SearchTypeEnum
import com.cyberarcenal.huddle.api.models.V1SearchHistoryCreateRequest
import com.cyberarcenal.huddle.api.models.V1SearchHistoryDeleteDestroy200Response
import com.cyberarcenal.huddle.api.models.V1SearchHistoryDestroy200Response
import com.cyberarcenal.huddle.api.models.V1SearchRecentRetrieve200Response
import com.cyberarcenal.huddle.api.models.V1SearchSuggestionsRetrieve200Response
import com.cyberarcenal.huddle.api.models.V1SearchTrendsRetrieve200Response
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class SearchRepository {
    private val api = ApiService.v1Api

    // ========== SEARCH HISTORY ==========

    /**
     * Retrieve the authenticated user's search history with optional filters and pagination.
     * @param days Filter by last X days.
     * @param searchType Filter by type (all, users, groups, posts).
     * @param page Page number.
     * @param pageSize Results per page.
     */
    suspend fun getSearchHistory(
        days: Int? = null,
        searchType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSearchHistory> = safeApiCall {
        api.v1SearchHistoryRetrieve(days, page, pageSize, searchType)
    }

    /**
     * Record a new search entry.
     * @param query The search query.
     * @param searchType Type of search (all, users, groups, posts).
     * @param resultsCount Number of results returned.
     */
    suspend fun recordSearch(
        query: String,
        searchType: SearchTypeEnum? = null,
        resultsCount: Int? = null
    ): Result<SearchHistory> {
        val request = RecordSearchInput( query = query, searchType = searchType, resultsCount = resultsCount)
        return safeApiCall { api.v1SearchHistoryCreate(request) }
    }

    /**
     * Clear the user's search history, optionally filtering by age or type.
     */
    suspend fun clearSearchHistory(): Result<V1SearchHistoryDestroy200Response> = safeApiCall {
        api.v1SearchHistoryDestroy()
    }

    /**
     * Delete a specific search history entry by its ID.
     */
    suspend fun deleteSearchHistoryEntry(entryId: Int): Result<V1SearchHistoryDeleteDestroy200Response> = safeApiCall {
        api.v1SearchHistoryDeleteDestroy(entryId)
    }

    // ========== POPULAR SEARCHES ==========

    /**
     * Get globally popular searches or, if userOnly=true, popular searches for the authenticated user.
     * @param days Time period in days (default 7).
     * @param limit Number of results (default 10).
     * @param searchType Filter by search type.
     * @param userOnly Show only user's popular searches (auth required).
     */
    suspend fun getPopularSearches(
        days: Int? = null,
        limit: Int? = null,
        searchType: String? = null,
        userOnly: Boolean? = null
    ): Result<PaginatedSearchHistory> = safeApiCall {
        api.v1SearchPopularRetrieve(days, limit, searchType, userOnly)
    }

    // ========== RECENT SEARCHES ==========

    /**
     * Get a list of the user's recent search queries.
     * @param limit Number of results (default 10).
     * @param unique Return unique queries only.
     */
    suspend fun getRecentSearches(
        limit: Int? = null,
        unique: Boolean? = null
    ): Result<V1SearchRecentRetrieve200Response> = safeApiCall {
        api.v1SearchRecentRetrieve(limit, unique)
    }

    // ========== SUGGESTIONS ==========

    /**
     * Get autocomplete suggestions based on a prefix.
     * @param prefix Search prefix (required).
     * @param includeAnonymous Include anonymous searches.
     * @param limit Number of suggestions (default 10).
     */
    suspend fun getSuggestions(
        prefix: String,
        includeAnonymous: Boolean? = null,
        limit: Int? = null
    ): Result<V1SearchSuggestionsRetrieve200Response> = safeApiCall {
        api.v1SearchSuggestionsRetrieve(prefix, includeAnonymous, limit)
    }

    /**
     * Alias for getSuggestions – same functionality.
     */
    suspend fun getSearchSuggestions(
        prefix: String,
        includeAnonymous: Boolean? = null,
        limit: Int? = null
    ): Result<V1SearchSuggestionsRetrieve200Response> = getSuggestions(prefix, includeAnonymous, limit)

    // ========== SEARCH STATISTICS ==========

    /**
     * Get search statistics for the authenticated user.
     * @param days Time period in days (default 30).
     */
    suspend fun getSearchStatistics(days: Int? = null): Result<SearchStatistics> = safeApiCall {
        api.v1SearchStatisticsRetrieve(days)
    }

    // ========== SEARCH TRENDS ==========

    /**
     * Get search volume trends over time.
     * @param days Time period in days (default 7).
     * @param interval Grouping interval (day/hour).
     */
    suspend fun getSearchTrends(
        days: Int? = null,
        interval: String? = null
    ): Result<V1SearchTrendsRetrieve200Response> = safeApiCall {
        api.v1SearchTrendsRetrieve(days, interval)
    }

    // ========== EXPORT ==========

    /**
     * Export the user's search history as a JSON file.
     * @param format Export format (json).
     * @param includeMetadata Include metadata.
     */
    suspend fun exportSearchHistory(
        format: String? = null,
        includeMetadata: Boolean? = null
    ): Result<Any> = safeApiCall {
        api.v1SearchExportRetrieve(format, includeMetadata)
    }
}
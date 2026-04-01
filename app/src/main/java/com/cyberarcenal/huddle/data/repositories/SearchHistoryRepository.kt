package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class SearchHistoryRepository {
    private val api = ApiService.searchHistoryApi

    suspend fun exportSearchHistory(
        format: String? = null,
        includeMetadata: Boolean? = null
    ): Result<ExportSearchHistoryResponse> =
        safeApiCall { api.apiV1SearchExportRetrieve(format, includeMetadata) }

    suspend fun recordSearch(request: RecordSearchInputRequest): Result<SearchHistoryCreateResponse> =
        safeApiCall { api.apiV1SearchHistoryCreate(request) }

    suspend fun clearSearchHistory(): Result<ClearHistoryResponse> =
        safeApiCall { api.apiV1SearchHistoryDestroy() }

    suspend fun deleteSearchHistoryEntry(entryId: Int): Result<DeleteEntryResponse> =
        safeApiCall { api.apiV1SearchHistoryDestroy2(entryId) }

    suspend fun getSearchHistory(
        days: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        searchType: String? = null
    ): Result<SearchHistoryListResponse> =
        safeApiCall { api.apiV1SearchHistoryRetrieve(days, page, pageSize, searchType) }

    suspend fun getPopularSearches(
        days: Int? = null,
        limit: Int? = null,
        searchType: String? = null,
        userOnly: Boolean? = null
    ): Result<PopularSearchesResponse> =
        safeApiCall { api.apiV1SearchPopularRetrieve(days, limit, searchType, userOnly) }

    suspend fun getRecentSearches(limit: Int? = null, unique: Boolean? = null): Result<RecentSearchesResponse> =
        safeApiCall { api.apiV1SearchRecentRetrieve(limit, unique) }

    suspend fun getSearchStatistics(days: Int? = null): Result<SearchStatisticsResponse> =
        safeApiCall { api.apiV1SearchStatisticsRetrieve(days) }

    suspend fun getSuggestions(
        prefix: String,
        includeAnonymous: Boolean? = null,
        limit: Int? = null
    ): Result<SearchSuggestionsResponse> =
        safeApiCall { api.apiV1SearchSuggestionsRetrieve(prefix, includeAnonymous, limit) }

    suspend fun getSearchTrends(days: Int? = null, interval: String? = null): Result<SearchTrendsResponse> =
        safeApiCall { api.apiV1SearchTrendsRetrieve(days, interval) }
}
// UserSearchsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserSearchsRepository {
    private val api = ApiService.userSearchsApi

    suspend fun advancedSearch(
        createdAfter: String? = null,
        createdBefore: String? = null,
        email: String? = null,
        firstName: String? = null,
        isVerified: Boolean? = null,
        lastName: String? = null,
        orderBy: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        username: String? = null
    ): Result<AdvancedSearchPaginatedResponse> =
        safeApiCall { api.apiV1UsersSearchAdvancedRetrieve(createdAfter, createdBefore, email, firstName, isVerified, lastName, orderBy, page, pageSize, username) }

    suspend fun autocomplete(query: String): Result<AutocompleteResponse> =
        safeApiCall { api.apiV1UsersSearchAutocompleteRetrieve(query) }

    suspend fun searchByEmail(email: String): Result<SearchByEmailResponse> =
        safeApiCall { api.apiV1UsersSearchByEmailRetrieve(email) }

    suspend fun searchByUsername(username: String): Result<SearchByUsernameResponse> =
        safeApiCall { api.apiV1UsersSearchByUsernameRetrieve(username) }

    suspend fun globalSearch(q: String): Result<GlobalSearchResponse> =
        safeApiCall { api.apiV1UsersSearchGlobalRetrieve(q) }

    suspend fun basicSearch(query: String, page: Int? = null, pageSize: Int? = null): Result<PaginatedSearchResult> =
        safeApiCall { api.apiV1UsersSearchRetrieve(query, page, pageSize) }

    suspend fun basicSearchUsers(query: String, page: Int? = null, pageSize: Int? = null): Result<PaginatedSearchResult> =
        safeApiCall { api.apiV1UsersSearchUsersRetrieve(query, page, pageSize) }
}
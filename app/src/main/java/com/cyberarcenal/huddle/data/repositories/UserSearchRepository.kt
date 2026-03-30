package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSearchRepository @Inject constructor() {

    private val api = ApiService.userSearchApi

    /**
     * Basic user search by username, email, or name.
     */
    suspend fun searchUsers(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserSearchResponse> = safeApiCall {
        api.apiV1SearchUsersRetrieve(query, page, pageSize)
    }

    /**
     * Get autocomplete suggestions based on a prefix.
     */
    suspend fun getAutocompleteSuggestions(
        query: String
    ): Result<PaginatedSearchAutocompleteResponse> = safeApiCall {
        api.apiV1SearchSearchAutocompleteRetrieve(query)
    }

    /**
     * Search users by exact or partial username.
     */
    suspend fun searchByUsername(
        username: String
    ): Result<PaginatedSearchByUsernameResponse> = safeApiCall {
        api.apiV1SearchSearchByUsernameRetrieve(username)
    }

    /**
     * Search users by email (Partial match - Admin only).
     */
    suspend fun searchByEmail(
        email: String
    ): Result<PaginatedSearchByEmailResponse> = safeApiCall {
        api.apiV1SearchSearchByEmailRetrieve(email)
    }

    /**
     * Advanced user search with multiple filters.
     */
    suspend fun advancedSearch(
        params: AdvancedSearchParams
    ): Result<PaginatedAdvancedUserSearchResponse> = safeApiCall {
        api.apiV1SearchSearchAdvancedRetrieve(
            createdAfter = params.createdAfter,
            createdBefore = params.createdBefore,
            email = params.email,
            firstName = params.firstName,
            isVerified = params.isVerified,
            lastName = params.lastName,
            orderBy = params.orderBy,
            page = params.page,
            pageSize = params.pageSize,
            username = params.username
        )
    }
}

/**
 * Data class para malinis na maipasa ang madaming filters ng Advanced Search.
 */
data class AdvancedSearchParams(
    val createdAfter: String? = null,
    val createdBefore: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val isVerified: Boolean? = null,
    val lastName: String? = null,
    val orderBy: String? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
    val username: String? = null
)
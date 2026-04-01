package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSearchRepository @Inject constructor() {

    private val api = ApiService.userSearchApi

    suspend fun searchUsers(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<UserSearchResponse> = safeApiCall {
        api.apiV1SearchUsersRetrieve(query, page, pageSize)
    }

    suspend fun getAutocompleteSuggestions(query: String): Result<SearchAutocompleteResponse> =
        safeApiCall { api.apiV1SearchSearchAutocompleteRetrieve(query) }

    suspend fun searchByUsername(username: String): Result<SearchByUsernameResponse> =
        safeApiCall { api.apiV1SearchSearchByUsernameRetrieve(username) }

    suspend fun searchByEmail(email: String): Result<SearchByEmailResponse> =
        safeApiCall { api.apiV1SearchSearchByEmailRetrieve(email) }

    suspend fun advancedSearch(params: AdvancedSearchParams): Result<AdvancedUserSearchResponse> =
        safeApiCall {
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
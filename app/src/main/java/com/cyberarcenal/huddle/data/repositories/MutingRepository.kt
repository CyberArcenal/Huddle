package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.MutingApi
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class MutingRepository {
    private val api: MutingApi = ApiService.mutingApi

    /**
     * Mute another user.
     */
    suspend fun muteUser(request: MutedUserCreateRequest): Result<MuteResponse> =
        safeApiCall { api.apiV1UsersMutingMuteCreate(request) }

    /**
     * Unmute a previously muted user.
     */
    suspend fun unmuteUser(request: UnMuteUserCreateRequest): Result<UnmuteResponse> =
        safeApiCall { api.apiV1UsersMutingUnmuteCreate(request) }

    /**
     * Get paginated list of users muted by the current user.
     */
    suspend fun getMutedUsers(
        limit: Int? = null,
        offset: Int? = null
    ): Result<PaginatedMutedUsersResponse> =
        safeApiCall { api.apiV1UsersMutingRetrieve(limit, offset) }

    /**
     * Check if the current user has muted another user.
     * (Note: This endpoint may require a user ID, but based on the API definition, it returns a response
     * for the current user. The actual implementation may need to pass a user ID; adjust if necessary.)
     */
    suspend fun checkMuted(): Result<CheckMutedResponse> =
        safeApiCall { api.apiV1UsersMutingCheckRetrieve() }
}
// FriendshipsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class FriendshipsRepository {
    private val api = ApiService.friendshipsApi

    suspend fun removeFriend(request: FriendRemoveRequest): Result<FriendRemoveResponse> =
        safeApiCall { api.apiV1UsersFriendsRemoveCreate(request) }

    suspend fun acceptRequest(id: Int): Result<FriendshipDetail> =
        safeApiCall { api.apiV1UsersFriendsRequestsAcceptCreate(id) }

    suspend fun declineRequest(id: Int): Result<FriendshipDetail> =
        safeApiCall { api.apiV1UsersFriendsRequestsDeclineCreate(id) }

    suspend fun getPendingRequests(limit: Int? = null, offset: Int? = null): Result<PaginatedPendingRequests> =
        safeApiCall { api.apiV1UsersFriendsRequestsPendingRetrieve(limit, offset) }

    suspend fun sendRequest(request: FriendshipCreateRequest): Result<FriendshipDetail> =
        safeApiCall { api.apiV1UsersFriendsRequestsSendCreate(request) }

    suspend fun getFriends(limit: Int? = null, offset: Int? = null): Result<PaginatedFriends> =
        safeApiCall { api.apiV1UsersFriendsRetrieve(limit, offset) }

    suspend fun updateFriendTag(id: Int, request: PatchedTagUpdateRequest? = null): Result<TagUpdateResponse> =
        safeApiCall { api.apiV1UsersFriendsTagPartialUpdate(id, request) }
}
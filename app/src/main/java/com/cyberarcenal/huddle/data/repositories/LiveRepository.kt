package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class LiveRepository {
    private val api = ApiService.liveApi

    suspend fun startLive(request: LiveCreateRequest): Result<StartLiveResponse> =
        safeApiCall { api.apiV1LiveStartCreate(request) }

    suspend fun endLive(liveId: Int): Result<EndLiveResponse> =
        safeApiCall { api.apiV1LiveEndCreate(liveId) }

    suspend fun getActiveStreams(page: Int?=1, pageSize: Int?=10): Result<ActiveLivesResponse> =
        safeApiCall { api.apiV1LiveActiveRetrieve(page, pageSize) }

    suspend fun getLiveDetails(liveId: Int): Result<LiveDetailResponse> =
        safeApiCall { api.apiV1LiveRetrieve(liveId) }

    suspend fun requestJoin(liveId: Int, message: String): Result<RequestJoinLiveViewResponse> =
        safeApiCall { api.apiV1LiveRequestCreate(liveId, mapOf("message" to message)) }

    suspend fun respondToRequest(requestId: Int, approve: Boolean): Result<RespondToJoinRequestResponse> =
        safeApiCall {
            val request = RespondToJoinRequestCreateRequest(approve)
            api.apiV1LiveRequestsRespondCreate(requestId, request) }

    suspend fun leaveStream(liveId: Int): Result<LeaveLiveViewResponse> =
        safeApiCall { api.apiV1LiveLeaveCreate(liveId) }

    suspend fun getParticipants(liveId: Int): Result<LiveParticipantsResponse> =
        safeApiCall { api.apiV1LiveParticipantsRetrieve(liveId) }

    suspend fun getPendingRequests(liveId: Int): Result<GetPendingRequestsResponse> =
        safeApiCall { api.apiV1LivePendingRequestsRetrieve(liveId) }

    suspend fun getLiveKitToken(liveId: Int): Result<LiveTokenResponse> =
        safeApiCall { api.getLiveToken(liveId) }
}

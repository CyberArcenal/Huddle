package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class DatingMessagesRepository {
    private val api = ApiService.datingMessagesApi

    suspend fun getConversation(
        userId: Int,
        limit: Int? = null,
        offset: Int? = null
    ): Result<DatingMessageListResponse> =
        safeApiCall { api.apiV1DatingMessagesConversationRetrieve(userId, limit, offset) }

    suspend fun getInbox(limit: Int? = null, offset: Int? = null): Result<DatingMessageListResponse> =
        safeApiCall { api.apiV1DatingMessagesInboxRetrieve(limit, offset) }

    suspend fun markMessageRead(id: Int): Result<DatingMessageMarkReadResponse> =
        safeApiCall { api.apiV1DatingMessagesMarkReadPartialUpdate(id) }

    suspend fun sendMessage(request: DatingMessageCreateRequest): Result<DatingMessageSendResponse> =
        safeApiCall { api.apiV1DatingMessagesSendCreate(request) }

    suspend fun getSentMessages(limit: Int? = null, offset: Int? = null): Result<DatingMessageListResponse> =
        safeApiCall { api.apiV1DatingMessagesSentRetrieve(limit, offset) }
}
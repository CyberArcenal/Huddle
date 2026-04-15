package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor() {
    private val api = ApiService.conversationApi

    suspend fun createConversation(request: ConversationCreateRequest): Result<ConversationCreateResponse> =
        safeApiCall { api.apiV1MessagingConversationsCreate(request) }

    suspend fun deleteConversation(id: Int): Result<ConversationDeleteResponse> =
        safeApiCall { api.apiV1MessagingConversationsDestroy(id) }

    suspend fun markConversationRead(conversationPk: Int): Result<MarkMessagesReadResponse> =
        safeApiCall { api.apiV1MessagingConversationsMarkReadCreate(conversationPk) }

    suspend fun getConversations(page: Int? = null, pageSize: Int? = null): Result<ConversationListResponse> =
        safeApiCall { api.apiV1MessagingConversationsRetrieve(page, pageSize) }

    suspend fun getConversation(id: Int): Result<ConversationDetailResponse> =
        safeApiCall { api.apiV1MessagingConversationsRetrieve2(id) }
}
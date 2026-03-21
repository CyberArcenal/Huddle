// ConversationRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class ConversationRepository {
    private val api = ApiService.conversationApi

    suspend fun createConversation(request: ConversationCreateRequest): Result<Conversation> =
        safeApiCall { api.apiV1MessagingConversationsCreate(request) }

    suspend fun deleteConversation(id: Int): Result<Unit> =
        safeApiCall { api.apiV1MessagingConversationsDestroy(id) }

    suspend fun markConversationRead(conversationPk: Int): Result<ApiV1MessagingConversationsMarkReadCreate200Response> =
        safeApiCall { api.apiV1MessagingConversationsMarkReadCreate(conversationPk) }

    suspend fun getConversations(page: Int? = null, pageSize: Int? = null): Result<PaginatedConversation> =
        safeApiCall { api.apiV1MessagingConversationsRetrieve(page, pageSize) }

    suspend fun getConversation(id: Int): Result<Conversation> =
        safeApiCall { api.apiV1MessagingConversationsRetrieve2(id) }
}
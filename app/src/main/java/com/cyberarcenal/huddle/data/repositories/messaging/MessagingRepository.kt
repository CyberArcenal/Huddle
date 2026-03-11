package com.cyberarcenal.huddle.data.repositories.messaging

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody
import java.net.URI

class MessagingRepository {
    private val api = ApiService.v1Api

    // ========== CONVERSATIONS ==========

    suspend fun getConversations(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedConversation> = safeApiCall {
        api.v1MessagingConversationsRetrieve(page, pageSize)
    }

    suspend fun createConversation(conversationCreate: ConversationCreateRequest): Result<Conversation> = safeApiCall {
        api.v1MessagingConversationsCreate(conversationCreate)
    }

    suspend fun getConversation(conversationId: Int): Result<Conversation> = safeApiCall {
        api.v1MessagingConversationsRetrieve2(conversationId)
    }

    suspend fun deleteConversation(conversationId: Int): Result<Unit> = safeApiCall {
        api.v1MessagingConversationsDestroy(conversationId)
    }

    suspend fun markConversationRead(conversationId: Int): Result<V1MessagingConversationsMarkReadCreate200Response> = safeApiCall {
        api.v1MessagingConversationsMarkReadCreate(conversationId)
    }

    // ========== MESSAGES ==========

    suspend fun getMessages(
        conversationId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedMessage> = safeApiCall {
        api.v1MessagingConversationsMessagesRetrieve(conversationId, page, pageSize)
    }

    suspend fun sendTextMessage(
        conversationId: Int,
        content: String
    ): Result<Message> = safeApiCall {
        api.v1MessagingConversationsMessagesCreate(
            conversationPk = conversationId,
            conversation = conversationId,
            content = content,
            media = null,
            mediaType = null
        )
    }

    suspend fun sendMediaMessage(
        conversationId: Int,
        content: String? = null,
        media: MultipartBody.Part,
        mediaType: String
    ): Result<Message> = safeApiCall {
        api.v1MessagingConversationsMessagesCreate(
            conversationPk = conversationId,
            conversation = conversationId,
            content = content ?: "",
            media = media,
            mediaType = mediaType
        )
    }
}
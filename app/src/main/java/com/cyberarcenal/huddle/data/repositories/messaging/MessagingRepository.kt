package com.cyberarcenal.huddle.data.repositories.messaging

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody
import java.net.URI

class MessagingRepository {
    private val api = ApiService.v1Api

    // ========== CONVERSATIONS ==========

    /**
     * List all conversations the current user participates in, ordered by most recent activity.
     */
    suspend fun getConversations(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedConversation> = safeApiCall {
        api.v1MessagingConversationsRetrieve(page, pageSize)
    }

    /**
     * Create a new conversation. The current user is automatically added to participants.
     */
    suspend fun createConversation(conversationCreate: ConversationCreate): Result<Conversation> = safeApiCall {
        api.v1MessagingConversationsCreate(conversationCreate)
    }

    /**
     * Get a single conversation by ID.
     */
    suspend fun getConversation(conversationId: Int): Result<Conversation> = safeApiCall {
        api.v1MessagingConversationsRetrieve2(conversationId)
    }

    /**
     * Delete a conversation. Only participants can delete (or you may choose to just leave).
     */
    suspend fun deleteConversation(conversationId: Int): Result<Unit> = safeApiCall {
        api.v1MessagingConversationsDestroy(conversationId)
    }

    /**
     * Mark all unread messages in a conversation as read (except those sent by the current user).
     */
    suspend fun markConversationRead(conversationId: Int): Result<V1MessagingConversationsMarkReadCreate200Response> = safeApiCall {
        api.v1MessagingConversationsMarkReadCreate(conversationId)
    }

    // ========== MESSAGES ==========

    /**
     * Get paginated messages in a conversation (oldest first).
     */
    suspend fun getMessages(
        conversationId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedMessage> = safeApiCall {
        api.v1MessagingConversationsMessagesRetrieve(conversationId, page, pageSize)
    }

    /**
     * Send a text message in a conversation.
     */
    suspend fun sendTextMessage(
        conversationId: Int,
        content: String
    ): Result<Message> = safeApiCall {
        // The generated API uses @Multipart for messages. We'll use the same.
        // For text only, we pass content and conversation as parts.
        api.v1MessagingConversationsMessagesCreate(
            conversationPk = conversationId,
            conversation = conversationId,
            content = content,
            media = null,
            mediaType = null
        )
    }

    /**
     * Send a media message in a conversation.
     */
    suspend fun sendMediaMessage(
        conversationId: Int,
        content: String? = null,
        media: URI,
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

    // Note: The generated API doesn't have endpoints for updating or deleting messages.
    // Possibly because messages are immutable. So we only have create and list.
}
// ChatRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody

class ChatRepository {
    private val api = ApiService.chatApi

    suspend fun sendMessage(
        conversationPk: Int,
        conversation: Int,
        content: String,
        media: MultipartBody.Part? = null,
        mediaType: String? = null
    ): Result<Message> =
        safeApiCall { api.apiV1MessagingConversationsMessagesCreate(conversationPk, conversation, content, media, mediaType) }

    suspend fun getMessages(conversationPk: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedMessage> =
        safeApiCall { api.apiV1MessagingConversationsMessagesRetrieve(conversationPk, page, pageSize) }
}
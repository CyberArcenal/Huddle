// ChatRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

class ChatRepository {
    private val api = ApiService.chatApi
    private  val upload = ApiService.chatUploadApi

    suspend fun sendMessage(
        conversationPk: Int,
        conversation: Int,
        content: String,
        media: MultipartBody.Part? = null,
        mediaType: String? = null
    ): Result<Message> = safeApiCall {
        val conversationBody = conversation.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val mediaTypeBody = mediaType?.toRequestBody("text/plain".toMediaTypeOrNull())
       upload.sendMessageWithMedia(conversationPk, conversationBody, contentBody, media, mediaTypeBody)
    }

    suspend fun getMessages(conversationPk: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedMessage> =
        safeApiCall { api.apiV1MessagingConversationsMessagesRetrieve(conversationPk, page, pageSize) }
}



interface ChatUploadApi {
    @Multipart
    @POST("api/v1/messaging/conversations/{conversation_pk}/messages/")
    suspend fun sendMessageWithMedia(
        @Path("conversation_pk") conversationPk: Int,
        @Part("conversation") conversation: RequestBody,
        @Part("content") content: RequestBody,
        @Part media: MultipartBody.Part? = null,
        @Part("media_type") mediaType: RequestBody? = null
    ): Response<Message>
}
package com.cyberarcenal.huddle.ui.chat.websocket

import android.util.Log
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

object WebSocketManager {
    private const val TAG = "WebSocketManager"
    private val client = OkHttpClient.Builder().build()
    private var webSocket: WebSocket? = null
    private var currentConversationId: Int? = null
    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened for conversation $currentConversationId")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                if (json.getString("type") == "new_message") {
                    val messageId = json.getInt("message_id")
                    val senderId = json.getInt("sender_id")
                    val senderUsername = json.getString("sender_username")
                    val content = json.optString("content")
                    val mediaUrl = json.optString("media_url").takeIf { it != "null" }
                    val mediaType = json.optString("media_type").takeIf { it != "null" }
                    val timestamp = json.getString("timestamp")
                    scope.launch {
                        _events.emit(
                            WebSocketEvent.NewMessage(
                                messageId = messageId,
                                senderId = senderId,
                                senderUsername = senderUsername,
                                content = content,
                                mediaUrl = mediaUrl,
                                mediaType = mediaType,
                                timestamp = timestamp
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing WebSocket message", e)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
            scope.launch { _events.emit(WebSocketEvent.Closed) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            scope.launch { _events.emit(WebSocketEvent.Error(t.message ?: "Unknown error")) }
        }
    }

    fun connect(conversationId: Int) {
        disconnect()
        val token = TokenManager.accessToken ?: run {
            Log.e(TAG, "No access token")
            return
        }
        val url = "ws://127.0.0.1:8000/ws/chat/$conversationId/?token=$token"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
        currentConversationId = conversationId
    }

    fun disconnect() {
        webSocket?.close(1000, "Closing")
        webSocket = null
        currentConversationId = null
    }

    sealed class WebSocketEvent {
        data class NewMessage(
            val messageId: Int,
            val senderId: Int,
            val senderUsername: String,
            val content: String?,
            val mediaUrl: String?,
            val mediaType: String?,
            val timestamp: String
        ) : WebSocketEvent()
        object Closed : WebSocketEvent()
        data class Error(val message: String) : WebSocketEvent()
    }
}
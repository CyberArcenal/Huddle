package com.cyberarcenal.huddle.ui.live

import android.util.Log
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Request
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import com.cyberarcenal.huddle.api.models.LiveJoinRequest
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.api.models.LiveJoinStatusEnum
import com.cyberarcenal.huddle.network.ApiService
import java.time.OffsetDateTime

class LiveWebSocketManager(private val liveId: Int, private val jwtToken: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val listeners = mutableListOf<LiveStreamListener>()

    fun connect() {
        // Using the same BASE_URL logic as ApiService but for WSS
        // For development, we might need to handle 10.0.2.2 vs localhost
        val baseUrl = ApiService.WS_BASE_URL // Adjust as needed for production
        val request = Request.Builder()
            .url("${baseUrl}ws/live/$liveId/?token=$jwtToken")
            .build()

        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "new_request" -> {
                            val requestJson = json.getJSONObject("request")
                            val joinRequest = parseJoinRequest(requestJson)
                            listeners.forEach { it.onNewRequest(joinRequest) }
                        }
                        "request_responded" -> {
                            val status = json.getString("status")
                            val requestId = json.getInt("request_id")
                            listeners.forEach { it.onRequestResponded(requestId, status) }
                        }
                        "participant_update" -> {
                            val action = json.getString("action")
                            val userId = json.getInt("user_id")
                            val username = json.getString("username")
                            listeners.forEach { it.onParticipantUpdate(action, userId, username) }
                        }
                        "new_comment" -> {
                            val commentId = json.getInt("comment_id")
                            val userId = json.getInt("user_id")
                            val username = json.getString("username")
                            val content = json.getString("content")
                            val createdAt = json.getString("created_at")
                            listeners.forEach { it.onNewComment(commentId, userId, username, content, createdAt) }
                        }
                        "stream_ended" -> {
                            listeners.forEach { it.onStreamEnded() }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LiveWebSocketManager", "Error parsing message: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("LiveWebSocketManager", "WebSocket failure", t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("LiveWebSocketManager", "WebSocket closed: $reason")
            }
        })
    }

    private fun parseJoinRequest(json: JSONObject): LiveJoinRequest {
        val userJson = json.optJSONObject("user")
        val user = userJson?.let {
            UserMinimal(
                id = it.optInt("id"),
                username = it.optString("username"),
                profilePictureUrl = it.optString("profile_picture_url")
            )
        }
        
        return LiveJoinRequest(
            id = json.optInt("id"),
            user = user,
            status = try { LiveJoinStatusEnum.valueOf(json.optString("status").uppercase()) } catch(e: Exception) { null },
            requestedAt = try { OffsetDateTime.parse(json.optString("requested_at")) } catch(e: Exception) { null },
            message = json.optString("message")
        )
    }

    fun sendComment(content: String) {
        val json = JSONObject().apply {
            put("type", "comment")
            put("content", content)
        }
        webSocket?.send(json.toString())
    }

    fun addListener(listener: LiveStreamListener) = listeners.add(listener)
    fun removeListener(listener: LiveStreamListener) = listeners.remove(listener)
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    interface LiveStreamListener {
        fun onNewRequest(request: LiveJoinRequest)
        fun onRequestResponded(requestId: Int, status: String)
        fun onParticipantUpdate(action: String, userId: Int, username: String)
        fun onNewComment(commentId: Int, userId: Int, username: String, content: String, createdAt: String)
        fun onStreamEnded()


    }
}

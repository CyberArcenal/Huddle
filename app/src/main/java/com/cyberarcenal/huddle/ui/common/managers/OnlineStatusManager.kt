package com.cyberarcenal.huddle.ui.common.managers

import android.util.Log
import com.cyberarcenal.huddle.network.ApiService
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * Manager to handle user's online status via WebSocket.
 * Keeps the connection alive while the app is in use.
 */
object OnlineStatusManager {
    private const val TAG = "OnlineStatusManager"
    private val BASE_WS_URL = "${ApiService.WS_BASE_URL}ws/online/" // Adjust based on your backend

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Online Status WebSocket connected")
            _isOnline.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Online Status received: $text")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            _isOnline.value = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "Online Status WebSocket failure", t)
            _isOnline.value = false
            attemptReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Online Status WebSocket closed")
            _isOnline.value = false
        }
    }

    /**
     * Connect to the online status WebSocket.
     */
    fun connect() {
        val token = TokenManager.accessToken ?: return
        if (webSocket != null) return

        val request = Request.Builder()
            .url("$BASE_WS_URL?token=$token")
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    /**
     * Disconnect from the online status WebSocket.
     */
    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User logged out or app backgrounded")
        webSocket = null
        _isOnline.value = false
    }

    private fun attemptReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5000) // Wait 5 seconds before retrying
            Log.d(TAG, "Attempting to reconnect Online Status WebSocket...")
            connect()
        }
    }
}

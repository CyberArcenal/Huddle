// network/TokenManager.kt
package com.cyberarcenal.huddle.network

/**
 * Stores the current access token in memory for synchronous access by interceptors.
 * Updated whenever token changes (login, refresh, logout).
 */
object TokenManager {
    var accessToken: String? = null
        private set

    fun updateToken(token: String?) {
        accessToken = token
    }

    fun clearToken() {
        accessToken = null
    }
}
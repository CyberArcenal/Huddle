package com.cyberarcenal.huddle.network

import android.content.Context

/**
 * DEPRECATED: Use TokenManager instead. 
 * This class is kept temporarily to avoid compilation errors but should be removed.
 */
@Deprecated("Use TokenManager", ReplaceWith("TokenManager"))
object AuthManager {
    suspend fun getAccessToken(context: Context) = TokenManager.getAccessToken(context)
    suspend fun getRefreshToken(context: Context) = TokenManager.getRefreshToken(context)
    suspend fun saveTokens(context: Context, access: String, refresh: String) = TokenManager.saveTokens(context, access, refresh)
    suspend fun clearTokens(context: Context) = TokenManager.clearAll(context)
    suspend fun isLoggedIn(context: Context) = TokenManager.isLoggedIn(context)
}
package com.cyberarcenal.huddle.network

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("auth")

object AuthManager {
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    suspend fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
        TokenManager.updateToken(accessToken)
    }

    suspend fun getAccessToken(context: Context): String? =
        context.dataStore.data.first()[ACCESS_TOKEN_KEY]

    suspend fun getRefreshToken(context: Context): String? =
        context.dataStore.data.first()[REFRESH_TOKEN_KEY]

    suspend fun clearTokens(context: Context) {
        context.dataStore.edit { it.clear() }
        TokenManager.clearAll(context)
    }

    suspend fun isLoggedIn(context: Context): Boolean = getAccessToken(context) != null
}
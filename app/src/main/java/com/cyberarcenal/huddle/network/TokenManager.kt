package com.cyberarcenal.huddle.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object TokenManager {
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val USER_PROFILE_KEY = stringPreferencesKey("user_profile")

    var accessToken: String? = null
        private set

    // Memory cache para sa mabilisang access ng interceptors
    fun updateToken(token: String?) {
        accessToken = token
    }

    /**
     * Permanent save sa DataStore
     */
    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = token
        }
        updateToken(token)
    }

    /**
     * Permanent save ng User Profile
     */
    suspend fun saveUser(context: Context, user: UserProfile) {
        val userJson = Serializer.gson.toJson(user)
        context.dataStore.edit { prefs ->
            prefs[USER_PROFILE_KEY] = userJson
        }
    }

    /**
     * Retrieve User Profile mula DataStore
     */
    suspend fun getUser(context: Context): UserProfile? {
        val prefs = context.dataStore.data.first()
        val userJson = prefs[USER_PROFILE_KEY] ?: return null
        return try {
            Serializer.gson.fromJson(userJson, UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieve Token mula DataStore (ginagamit sa App Start)
     */
    suspend fun getToken(context: Context): String? {
        val prefs = context.dataStore.data.first()
        val token = prefs[ACCESS_TOKEN_KEY]
        accessToken = token
        return token
    }

    suspend fun clearAll(context: Context) {
        context.dataStore.edit { it.clear() }
        accessToken = null
    }
}

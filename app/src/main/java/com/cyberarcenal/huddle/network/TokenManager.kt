package com.cyberarcenal.huddle.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.local.HuddleDatabase
import com.cyberarcenal.huddle.data.local.entities.ProfileEntity
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object TokenManager {
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val USER_PROFILE_KEY = stringPreferencesKey("user_profile")

    var accessToken: String? = null
        private set

    fun updateToken(token: String?) {
        accessToken = token
    }

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = token
        }
        updateToken(token)
    }

    /**
     * I‑save (o i‑update) ang buong user profile sa DataStore at Room.
     * Ginagamit ito sa login at sa anumang profile update.
     */
    suspend fun saveUser(context: Context, user: UserProfile) {
        // DataStore
        val userJson = Serializer.gson.toJson(user)
        context.dataStore.edit { prefs ->
            prefs[USER_PROFILE_KEY] = userJson
        }

        // Room
        user.id?.let { id ->
            HuddleDatabase.getDatabase(context).profileDao().insertProfile(
                ProfileEntity(
                    id = id,
                    username = user.username,
                    profilePictureUrl = user.profilePictureUrl,
                    coverPhotoUrl = user.coverPhotoUrl,
                    bio = user.bio,
                    rawData = user
                )
            )
        }
    }

    /**
     * Alias para sa saveUser – mas malinaw na tawagin ito pagkatapos ng profile update.
     */
    suspend fun updateUser(context: Context, user: UserProfile) = saveUser(context, user)

    /**
     * Kunin ang user profile mula sa DataStore.
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
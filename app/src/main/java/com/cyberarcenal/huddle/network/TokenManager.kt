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
    private const val TAG = "HUDDLE_DEBUG"
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val USER_PROFILE_KEY = stringPreferencesKey("user_profile")

    var accessToken: String? = null
        private set

    fun updateToken(token: String?) {
        accessToken = token
    }

    suspend fun saveTokens(context: Context, access: String, refresh: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = access
            prefs[REFRESH_TOKEN_KEY] = refresh
        }
        updateToken(access)
    }

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = token
        }
        updateToken(token)
    }

    /**
     * Sanitizes JSON string to handle invalid dates like 0000-00-00
     */
    private fun sanitizeJson(json: String): String {
        if (!json.contains("0000-00-00")) return json
        return json.replace(Regex("\"0000-00-00\""), "null")
            .replace(Regex(": ?\"0000-00-00\""), ":null")
    }

    suspend fun saveUser(context: Context, newUser: UserProfile) {
        val userId = newUser.id ?: return
        
        try {
            android.util.Log.d(TAG, "TokenManager: Attempting to save user ID: $userId")
            
            // 1. Subukang kunin ang current data
            val currentUser = getUser(context)
            
            // 2. SMART MERGE + PROTECTION
            // Kung ang pumasok na newUser ay walang email (partial), pero ang currentUser ay MERON,
            // i-merge natin sila para hindi mawala ang email/private data.
            val mergedUser = if (newUser.email.isNullOrBlank() && currentUser != null && !currentUser.email.isNullOrBlank()) {
                android.util.Log.w(TAG, "TokenManager: Partial profile detected. Merging with existing email: ${currentUser.email}")
                newUser.copy(
                    email = currentUser.email,
                    phoneNumber = if (newUser.phoneNumber.isNullOrBlank()) currentUser.phoneNumber else newUser.phoneNumber,
                    dateOfBirth = newUser.dateOfBirth ?: currentUser.dateOfBirth
                )
            } else if (newUser.email.isNullOrBlank() && currentUser == null) {
                // EXTREME PROTECTION: Kung wala tayong mabasang current user (dahil sa crash),
                // at ang pumasok na data ay walang email, HUWAG i-save para hindi mabura ang record.
                android.util.Log.e(TAG, "TokenManager: ABORT SAVE! No current data and new data is partial. Preventing data loss.")
                return
            } else {
                newUser
            }

            // 3. Serialize and Sanitize before saving
            val userJson = sanitizeJson(Serializer.gson.toJson(mergedUser))
            
            context.dataStore.edit { prefs ->
                prefs[USER_PROFILE_KEY] = userJson
            }
            android.util.Log.d(TAG, "TokenManager: Successfully saved to DataStore (Email: ${mergedUser.email})")

            // 4. Save to Room
            HuddleDatabase.getDatabase(context).profileDao().insertProfile(
                ProfileEntity(
                    id = userId,
                    username = mergedUser.username,
                    profilePictureUrl = mergedUser.profilePictureUrl,
                    coverPhotoUrl = mergedUser.coverPhotoUrl,
                    bio = mergedUser.bio,
                    rawData = mergedUser
                )
            )

        } catch (e: Exception) {
            android.util.Log.e(TAG, "TokenManager: CRITICAL ERROR in saveUser: ${e.message}")
        }
    }

    suspend fun updateUser(context: Context, user: UserProfile) = saveUser(context, user)

    suspend fun getUser(context: Context): UserProfile? {
        return try {
            val prefs = context.dataStore.data.first()
            val userJson = prefs[USER_PROFILE_KEY] ?: return null
            
            // Linisin ang JSON bago i-parse
            val sanitized = sanitizeJson(userJson)

            Serializer.gson.fromJson(sanitized, UserProfile::class.java).also {
                if (it != null) android.util.Log.d(TAG, "TokenManager: Retrieved user: ${it.username} (Email: ${it.email})")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "TokenManager: Error reading/parsing user: ${e.message}")
            null
        }
    }

    suspend fun getAccessToken(context: Context): String? {
        val prefs = context.dataStore.data.first()
        val token = prefs[ACCESS_TOKEN_KEY]
        accessToken = token
        return token
    }

    suspend fun getRefreshToken(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[REFRESH_TOKEN_KEY]
    }

    suspend fun getToken(context: Context): String? = getAccessToken(context)

    suspend fun isLoggedIn(context: Context): Boolean = getAccessToken(context) != null

    suspend fun clearAll(context: Context) {
        context.dataStore.edit { it.clear() }
        accessToken = null
    }
}
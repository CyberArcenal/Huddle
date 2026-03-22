// UserPreferencesRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.UserPreferenceRequestRequest
import com.cyberarcenal.huddle.api.models.UserPreferenceResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UserPreferencesRepository {
    private val api = ApiService.userPreferencesApi

    // --- Achievements ---
    suspend fun getAchievements(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesAchievementsRetrieve() }

    suspend fun updateAchievements(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesAchievementsUpdate(request) }

    // --- Causes ---
    suspend fun getCauses(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesCausesRetrieve() }

    suspend fun updateCauses(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesCausesUpdate(request) }

    // --- Favorites ---
    suspend fun getFavorites(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesFavoritesRetrieve() }

    suspend fun updateFavorites(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesFavoritesUpdate(request) }

    // --- Hobbies ---
    suspend fun getHobbies(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesHobbiesRetrieve() }

    suspend fun updateHobbies(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesHobbiesUpdate(request) }

    // --- Interests ---
    suspend fun getInterests(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesInterestsRetrieve() }

    suspend fun updateInterests(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesInterestsUpdate(request) }

    // --- Lifestyle Tags ---
    suspend fun getLifestyleTags(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesLifestyleTagsRetrieve() }

    suspend fun updateLifestyleTags(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesLifestyleTagsUpdate(request) }

    // --- Music ---
    suspend fun getMusic(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesMusicRetrieve() }

    suspend fun updateMusic(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesMusicUpdate(request) }

    // --- Schools ---
    suspend fun getSchools(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesSchoolsRetrieve() }

    suspend fun updateSchools(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesSchoolsUpdate(request) }

    // --- Works ---
    suspend fun getWorks(): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesWorksRetrieve() }

    suspend fun updateWorks(request: UserPreferenceRequestRequest): Result<UserPreferenceResponse> =
        safeApiCall { api.apiV1UsersPreferencesWorksUpdate(request) }
}
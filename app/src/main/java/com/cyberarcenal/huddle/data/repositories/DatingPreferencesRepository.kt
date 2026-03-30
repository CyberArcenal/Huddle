// DatingPreferencesRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class DatingPreferencesRepository {
    private val api = ApiService.datingPreferencesApi

    suspend fun checkCompatibility(request: DatingPreferenceCompatibilityRequest): Result<Unit> =
        safeApiCall { api.apiV1DatingDatingPreferencesCheckCompatibilityCreate(request) }

    suspend fun partialUpdatePreferences(request: PatchedDatingPreferenceCreateUpdateRequest? = null): Result<DatingPreferenceDetail> =
        safeApiCall { api.apiV1DatingDatingPreferencesPartialUpdate(request) }

    suspend fun getPreferences(): Result<DatingPreferenceDetail> =
        safeApiCall { api.apiV1DatingDatingPreferencesRetrieve() }

    suspend fun updatePreferences(request: DatingPreferenceCreateUpdateRequest? = null): Result<DatingPreferenceDetail> =
        safeApiCall { api.apiV1DatingDatingPreferencesUpdate(request) }
}
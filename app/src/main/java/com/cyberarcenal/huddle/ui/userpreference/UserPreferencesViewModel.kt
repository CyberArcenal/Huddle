// ui/userpreference/UserPreferencesViewModel.kt
package com.cyberarcenal.huddle.ui.userpreference

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.UserPreferenceRequestRequest
import com.cyberarcenal.huddle.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PreferenceItem(val id: Int, val name: String)

enum class PreferenceCategory {
    HOBBIES, INTERESTS, FAVORITES, MUSIC, WORKS, SCHOOLS, ACHIEVEMENTS, CAUSES, LIFESTYLE_TAGS;

    fun title(): String = when (this) {
        HOBBIES -> "Hobbies"
        INTERESTS -> "Interests"
        FAVORITES -> "Favorites"
        MUSIC -> "Music"
        WORKS -> "Works"
        SCHOOLS -> "Schools"
        ACHIEVEMENTS -> "Achievements"
        CAUSES -> "Causes"
        LIFESTYLE_TAGS -> "Lifestyle Tags"
    }
}

class UserPreferencesViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    private val _categories = MutableStateFlow(PreferenceCategory.values().asList())
    val categories: StateFlow<List<PreferenceCategory>> = _categories

    private val _available = MutableStateFlow<List<PreferenceItem>>(emptyList())
    val available: StateFlow<List<PreferenceItem>> = _available

    private val _selected = MutableStateFlow<List<PreferenceItem>>(emptyList())
    val selected: StateFlow<List<PreferenceItem>> = _selected

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private var currentCategory: PreferenceCategory? = null

    fun loadPreferences(category: PreferenceCategory) {
        currentCategory = category
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = when (category) {
                PreferenceCategory.HOBBIES -> repository.getHobbies()
                PreferenceCategory.INTERESTS -> repository.getInterests()
                PreferenceCategory.FAVORITES -> repository.getFavorites()
                PreferenceCategory.MUSIC -> repository.getMusic()
                PreferenceCategory.WORKS -> repository.getWorks()
                PreferenceCategory.SCHOOLS -> repository.getSchools()
                PreferenceCategory.ACHIEVEMENTS -> repository.getAchievements()
                PreferenceCategory.CAUSES -> repository.getCauses()
                PreferenceCategory.LIFESTYLE_TAGS -> repository.getLifestyleTags()
            }
            result.fold(onSuccess = { response ->
                if (response.status) {
                    val data = response.data;
                    _available.value = data.available.map {
                        PreferenceItem(
                            it.id ?: 0, it.name ?: ""
                        )
                    } ?: emptyList()
                    _selected.value =
                        data.selected.map { PreferenceItem(it.id ?: 0, it.name ?: "") }
                            ?: emptyList()
                }

            }, onFailure = { error ->
                _error.value = error.message ?: "Failed to load"
            })
            _loading.value = false
        }
    }

    fun savePreferences(selectedIds: List<Int>) {
        if (currentCategory == null) return
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            val request = UserPreferenceRequestRequest(ids = selectedIds)
            val result = when (currentCategory!!) {
                PreferenceCategory.HOBBIES -> repository.updateHobbies(request)
                PreferenceCategory.INTERESTS -> repository.updateInterests(request)
                PreferenceCategory.FAVORITES -> repository.updateFavorites(request)
                PreferenceCategory.MUSIC -> repository.updateMusic(request)
                PreferenceCategory.WORKS -> repository.updateWorks(request)
                PreferenceCategory.SCHOOLS -> repository.updateSchools(request)
                PreferenceCategory.ACHIEVEMENTS -> repository.updateAchievements(request)
                PreferenceCategory.CAUSES -> repository.updateCauses(request)
                PreferenceCategory.LIFESTYLE_TAGS -> repository.updateLifestyleTags(request)
            }
            result.fold(onSuccess = { response ->
                if (response.status) {
                    _selected.value = response.data.selected.map {
                        PreferenceItem(
                            it.id ?: 0, it.name ?: ""
                        )
                    } ?: emptyList()
                    _saveSuccess.value = true
                } else {
                    _error.value = response.message
                }

            }, onFailure = { error ->
                _error.value = error.message ?: "Failed to save"
            })
            _saving.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.PersonalityDetails
import com.cyberarcenal.huddle.data.repositories.PersonalityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonalityManager(
    private val repository: PersonalityRepository = PersonalityRepository(),
    private val viewModelScope: CoroutineScope
) {
    private val _personalityDetail = MutableStateFlow<PersonalityDetails?>(null)
    val personalityDetail: StateFlow<PersonalityDetails?> = _personalityDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val personalityCache = mutableMapOf<String, PersonalityDetails>()

    fun openPersonalityDetail(mbtiType: String) {
        if (personalityCache.containsKey(mbtiType)) {
            _personalityDetail.value = personalityCache[mbtiType]
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getPersonalityDetails(mbtiType).fold(
                onSuccess = { response ->
                    if (response.status) {
                        personalityCache[mbtiType] = response.data
                        _personalityDetail.value = response.data
                    } else {
                        _error.value = response.message
                    }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to load personality details"
                }
            )
            _isLoading.value = false
        }
    }

    fun dismissPersonalityDetail() {
        _personalityDetail.value = null
        _error.value = null
    }
}

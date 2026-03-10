package com.cyberarcenal.huddle.ui.groups.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.Group
import com.cyberarcenal.huddle.api.models.GroupCreate
import com.cyberarcenal.huddle.api.models.PrivacyEnum
import com.cyberarcenal.huddle.data.repositories.groups.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CreateGroupState {
    object Idle : CreateGroupState()
    object Loading : CreateGroupState()
    data class Success(val group: Group) : CreateGroupState()
    data class Error(val message: String) : CreateGroupState()
}

class CreateGroupViewModel(
    private val groupsRepository: GroupsRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _privacy = MutableStateFlow<PrivacyEnum>(PrivacyEnum.`public`)
    val privacy: StateFlow<PrivacyEnum> = _privacy.asStateFlow()

    private val _createState = MutableStateFlow<CreateGroupState>(CreateGroupState.Idle)
    val createState: StateFlow<CreateGroupState> = _createState.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _descriptionError = MutableStateFlow<String?>(null)
    val descriptionError: StateFlow<String?> = _descriptionError.asStateFlow()

    fun updateName(name: String) {
        _name.value = name
        if (name.isNotBlank()) _nameError.value = null
    }

    fun updateDescription(description: String) {
        _description.value = description
        if (description.isNotBlank()) _descriptionError.value = null
    }

    fun updatePrivacy(privacy: PrivacyEnum) {
        _privacy.value = privacy
    }

    fun createGroup() {
        val name = _name.value.trim()
        val description = _description.value.trim()

        if (name.isEmpty()) {
            _nameError.value = "Group name is required"
            return
        }
        if (description.isEmpty()) {
            _descriptionError.value = "Description is required"
            return
        }

        viewModelScope.launch {
            _createState.value = CreateGroupState.Loading
            val create = GroupCreate(
                name = name,
                description = description,
                privacy = _privacy.value,
                profilePicture = null,
                coverPhoto = null
            )
            val result = groupsRepository.createGroup(create)
            result.fold(
                onSuccess = { group ->
                    _createState.value = CreateGroupState.Success(group)
                },
                onFailure = { error ->
                    _createState.value = CreateGroupState.Error(error.message ?: "Failed to create group")
                }
            )
        }
    }

    fun resetState() {
        _createState.value = CreateGroupState.Idle
    }

    fun clearErrors() {
        _nameError.value = null
        _descriptionError.value = null
    }
}
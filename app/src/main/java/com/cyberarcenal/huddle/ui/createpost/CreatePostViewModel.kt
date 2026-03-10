package com.cyberarcenal.huddle.ui.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.Post
import com.cyberarcenal.huddle.api.models.PostCreate
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.data.repositories.feed.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val feedRepository: FeedRepository = FeedRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onContentChange(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun onPublicChange(isPublic: Boolean) {
        // Map switch to privacy enum: public if true, followers if false
        val privacy = if (isPublic) PrivacyB23Enum.PUBLIC else PrivacyB23Enum.FOLLOWERS
        _uiState.value = _uiState.value.copy(
            isPublic = isPublic,
            privacy = privacy
        )
    }

    fun createPost() {
        val currentState = _uiState.value
        if (currentState.content.isBlank()) {
            _uiState.value = currentState.copy(error = "Post content cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            val result = feedRepository.createTextPost(
                content = currentState.content,
                privacyEnum = currentState.privacy
            )

            result.fold(
                onSuccess = { createdPost ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        postCreated = true,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create post"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(postCreated = false)
    }
}

data class CreatePostUiState(
    val content: String = "",
    val isPublic: Boolean = true,
    val privacy: PrivacyB23Enum = PrivacyB23Enum.PUBLIC, // derived from isPublic
    val isLoading: Boolean = false,
    val error: String? = null,
    val postCreated: Boolean = false
)
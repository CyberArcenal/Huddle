package com.cyberarcenal.huddle.ui.createpost

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.PostCreateRequest
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.data.repositories.PostCreateRequestWithMedia
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CreatePostViewModel(
    private val postRepository: UserPostsRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    companion object {
        const val MAX_CONTENT_LENGTH = 1000
    }

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onContentChange(content: String) {
        val truncated = content.take(MAX_CONTENT_LENGTH)
        _uiState.value = _uiState.value.copy(content = truncated)
    }

    fun onPrivacyChange(privacy: PrivacyB23Enum) {
        _uiState.value = _uiState.value.copy(privacy = privacy)
    }

    fun onImagesSelected(uris: List<Uri>) {
        val updatedList = _uiState.value.selectedImages + uris
        _uiState.value = _uiState.value.copy(selectedImages = updatedList)
    }

    fun removeImage(uri: Uri) {
        val updatedList = _uiState.value.selectedImages.filter { it != uri }
        _uiState.value = _uiState.value.copy(selectedImages = updatedList)
    }

    fun createPost() {
        val currentState = _uiState.value
        if (currentState.content.isBlank() && currentState.selectedImages.isEmpty()) {
            _uiState.value = currentState.copy(error = "Post cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            val postType = if (currentState.selectedImages.isNotEmpty()) {
                PostTypeEnum.IMAGE
            } else {
                PostTypeEnum.TEXT
            }

            val result = if (currentState.selectedImages.isEmpty()) {
                val request = PostCreateRequestWithMedia(
                    postType = PostTypeEnum.TEXT,
                    content = currentState.content,
                    privacy = currentState.privacy
                )
                postRepository.createPost(request)
            } else {
                // Convert Uris to files in background
                val files = withContext(Dispatchers.IO) {
                    currentState.selectedImages.mapNotNull { uriToFile(it) }
                }
                if (files.isEmpty()) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Could not access selected images"
                    )
                    return@launch
                }
                val mimeTypes = currentState.selectedImages.map { uri ->
                    contentResolver.getType(uri) ?: "image/jpeg"
                }
                val request = PostCreateRequestWithMedia(
                    postType = PostTypeEnum.IMAGE,
                    content = currentState.content,
                    privacy = currentState.privacy,
                    mediaFiles = files,
                    mimeTypes = mimeTypes
                )
                postRepository.createPost(request)
            }

            result.fold(
                onSuccess = { post ->
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

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("post_", ".tmp")
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
    val privacy: PrivacyB23Enum = PrivacyB23Enum.PUBLIC,
    val selectedImages: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val postCreated: Boolean = false
)
package com.cyberarcenal.huddle.ui.createstory

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.data.repositories.stories.StoriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class CreateStoryViewModel(
    private val storiesRepository: StoriesRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateStoryUiState())
    val uiState: StateFlow<CreateStoryUiState> = _uiState.asStateFlow()

    fun setSelectedImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun setCaption(caption: String) {
        _uiState.value = _uiState.value.copy(caption = caption)
    }

    fun createStory() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            val result = if (currentState.selectedImageUri != null) {
                val file = uriToFile(currentState.selectedImageUri!!)
                if (file == null) {
                    _uiState.value = currentState.copy(isLoading = false, error = "Could not access image file")
                    return@launch
                }
                val mimeType = contentResolver.getType(currentState.selectedImageUri!!) ?: "image/jpeg"
                
                storiesRepository.createStoryWithMedia(
                    storyType = StoryTypeEnum.IMAGE,
                    content = currentState.caption.takeIf { it.isNotBlank() },
                    mediaFile = file,
                    mimeType = mimeType,
                    expiresInHours = 24
                )
            } else {
                storiesRepository.createTextStory(
                    content = currentState.caption,
                    expiresInHours = 24
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.value = currentState.copy(isLoading = false, storyCreated = true)
                },
                onFailure = { error ->
                    Log.e("CreateStory", "Upload failed", error)
                    _uiState.value = currentState.copy(
                        isLoading = false, 
                        error = error.message ?: "Failed to parse response"
                    )
                }
            )
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("story_", ".tmp", File(System.getProperty("java.io.tmpdir")))
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            Log.e("CreateStory", "URI to File failed", e)
            null
        }
    }

    fun resetSuccess() { _uiState.value = _uiState.value.copy(storyCreated = false) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}

data class CreateStoryUiState(
    val selectedImageUri: Uri? = null,
    val caption: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val storyCreated: Boolean = false
)

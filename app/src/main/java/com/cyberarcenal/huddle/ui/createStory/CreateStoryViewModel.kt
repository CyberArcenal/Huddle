package com.cyberarcenal.huddle.ui.createStory

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CreateStoryViewModel(
    private val storyRepository: StoriesRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateStoryUiState())
    val uiState: StateFlow<CreateStoryUiState> = _uiState.asStateFlow()

    private val backgroundColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFCF6679),
        Color(0xFF018786), Color(0xFFB00020), Color(0xFF3700B3),
        Color(0xFFFF5722), Color(0xFF4CAF50), Color(0xFF2196F3)
    )
    private var colorIndex = 0

    fun setSelectedMediaUri(uri: Uri?) {
        val type = uri?.let { contentResolver.getType(it) }
        val storyType = when {
            type?.startsWith("video") == true -> StoryTypeEnum.VIDEO
            type?.startsWith("image") == true -> StoryTypeEnum.IMAGE
            else -> StoryTypeEnum.TEXT
        }
        _uiState.update { it.copy(selectedMediaUri = uri, storyType = storyType) }
    }

    fun setCaption(caption: String) {
        _uiState.update { it.copy(caption = caption) }
    }

    fun cycleBackgroundColor() {
        colorIndex = (colorIndex + 1) % backgroundColors.size
        _uiState.update { it.copy(backgroundColor = backgroundColors[colorIndex]) }
    }

    fun setPrivacy(privacy: PrivacyB23Enum) {
        _uiState.update { it.copy(privacy = privacy) }
    }

    fun setAllowReplies(allow: Boolean) {
        _uiState.update { it.copy(allowReplies = allow) }
    }

    fun createStory(context: Context) {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Copy file to internal storage for the Worker
            val internalFilePath = withContext(Dispatchers.IO) {
                currentState.selectedMediaUri?.let { uriToFile(it) }?.absolutePath
            }
            
            val mimeType = currentState.selectedMediaUri?.let { contentResolver.getType(it) }

            val inputData = workDataOf(
                StoryUploadWorker.KEY_CONTENT to currentState.caption,
                StoryUploadWorker.KEY_STORY_TYPE to currentState.storyType.value,
                StoryUploadWorker.KEY_MEDIA_PATH to internalFilePath,
                StoryUploadWorker.KEY_MIME_TYPE to mimeType,
                StoryUploadWorker.KEY_EXPIRES_IN to 24
            )

            val uploadWorkRequest = OneTimeWorkRequestBuilder<StoryUploadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueue(uploadWorkRequest)

            // Signal success to UI to close the screen immediately
            _uiState.update { it.copy(isLoading = false, storyCreated = true) }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val extension = when {
                contentResolver.getType(uri)?.contains("video") == true -> ".mp4"
                else -> ".jpg"
            }
            val tempFile = File.createTempFile("story_upload_", extension)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            Log.e("CreateStory", "URI to File failed", e)
            null
        }
    }

    fun resetSuccess() { _uiState.update { it.copy(storyCreated = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class CreateStoryUiState(
    val selectedMediaUri: Uri? = null,
    val storyType: StoryTypeEnum = StoryTypeEnum.TEXT,
    val caption: String = "",
    val backgroundColor: Color = Color(0xFF6200EE),
    val privacy: PrivacyB23Enum = PrivacyB23Enum.PUBLIC,
    val allowReplies: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val storyCreated: Boolean = false
)
package com.cyberarcenal.huddle.ui.createpost

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
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
    private val feedRepository: UserPostsRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    companion object {
        const val MAX_CONTENT_LENGTH = 1000
    }

    enum class Step {
        CREATE, PREVIEW
    }

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun setStep(step: Step) {
        _uiState.value = _uiState.value.copy(step = step)
    }

    fun onContentChange(content: String) {
        val truncated = content.take(MAX_CONTENT_LENGTH)
        _uiState.value = _uiState.value.copy(content = truncated)
    }

    fun onPrivacyChange(privacy: PrivacyB23Enum) {
        _uiState.value = _uiState.value.copy(privacy = privacy)
    }

    fun onMediaSelected(uris: List<Uri>) {
        val updatedList = _uiState.value.selectedMedia + uris
        _uiState.value = _uiState.value.copy(selectedMedia = updatedList)
    }

    fun removeMedia(uri: Uri) {
        val updatedList = _uiState.value.selectedMedia.filter { it != uri }
        _uiState.value = _uiState.value.copy(selectedMedia = updatedList)
    }

    fun createPost(context: Context) {
        val currentState = _uiState.value
        if (currentState.content.isBlank() && currentState.selectedMedia.isEmpty()) {
            _uiState.value = currentState.copy(error = "Post cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            // Determine post type
            var hasVideo = false
            val mimeTypes = currentState.selectedMedia.map { uri ->
                val type = contentResolver.getType(uri) ?: "image/jpeg"
                if (type.startsWith("video/")) hasVideo = true
                type
            }

            val postType = when {
                currentState.selectedMedia.isEmpty() -> PostTypeEnum.TEXT
                hasVideo -> PostTypeEnum.VIDEO
                else -> PostTypeEnum.IMAGE
            }

            // Copy files to internal storage for the Worker
            val internalFilePaths = withContext(Dispatchers.IO) {
                currentState.selectedMedia.mapNotNull { uri ->
                    uriToFile(uri)?.absolutePath
                }
            }

            val inputData = workDataOf(
                PostUploadWorker.KEY_CONTENT to currentState.content,
                PostUploadWorker.KEY_PRIVACY to currentState.privacy.value,
                PostUploadWorker.KEY_POST_TYPE to postType.value,
                PostUploadWorker.KEY_MEDIA_PATHS to internalFilePaths.toTypedArray(),
                PostUploadWorker.KEY_MIME_TYPES to mimeTypes.toTypedArray()
            )

            val uploadWorkRequest = OneTimeWorkRequestBuilder<PostUploadWorker>()
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

            // Signal success to UI to close the screen
            _uiState.value = currentState.copy(
                isLoading = false,
                postCreated = true
            )
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val type = contentResolver.getType(uri)
            val extension = when {
                type?.startsWith("video/") == true -> ".mp4"
                type == "image/png" -> ".png"
                type == "image/gif" -> ".gif"
                else -> ".jpg"
            }
            val tempFile = File.createTempFile("upload_", extension)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(postCreated = false)
    }
}

data class CreatePostUiState(
    val step: CreatePostViewModel.Step = CreatePostViewModel.Step.CREATE,
    val content: String = "",
    val privacy: PrivacyB23Enum = PrivacyB23Enum.PUBLIC,
    val selectedMedia: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val postCreated: Boolean = false
)

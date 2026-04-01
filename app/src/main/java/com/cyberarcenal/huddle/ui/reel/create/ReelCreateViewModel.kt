package com.cyberarcenal.huddle.ui.reel.create

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ReelCreateViewModel(
    private val contentResolver: ContentResolver,
    private val context: Context   // Added context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReelCreateUiState())
    val uiState: StateFlow<ReelCreateUiState> = _uiState.asStateFlow()

    fun setSelectedVideoUri(uri: Uri?) {
        _uiState.update { it.copy(selectedVideoUri = uri) }
    }

    fun setCaption(caption: String) {
        _uiState.update { it.copy(caption = caption) }
    }

    fun setPrivacy(privacy: PrivacyB23Enum) {
        _uiState.update { it.copy(privacy = privacy) }
    }

    fun setThumbnailUri(uri: Uri?) {
        _uiState.update { it.copy(thumbnailUri = uri) }
    }

    fun createReel() {
        val currentState = _uiState.value
        val videoUri = currentState.selectedVideoUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val videoPath = withContext(Dispatchers.IO) { uriToFile(videoUri, "video") }?.absolutePath
            val thumbnailPath = withContext(Dispatchers.IO) {
                currentState.thumbnailUri?.let { uriToFile(it, "image") }
            }?.absolutePath

            if (videoPath == null) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to process video file") }
                return@launch
            }

            // Generate a unique client ID to prevent duplicates on retry
            val clientId = UUID.randomUUID().toString()

            val inputData = workDataOf(
                ReelUploadWorker.KEY_CAPTION to currentState.caption,
                ReelUploadWorker.KEY_VIDEO_PATH to videoPath,
                ReelUploadWorker.KEY_THUMBNAIL_PATH to thumbnailPath,
                ReelUploadWorker.KEY_PRIVACY to currentState.privacy.value,
                ReelUploadWorker.KEY_CLIENT_ID to clientId
            )

            val uploadWorkRequest = OneTimeWorkRequestBuilder<ReelUploadWorker>()
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

            _uiState.update { it.copy(isLoading = false, reelCreated = true) }
        }
    }

    private fun uriToFile(uri: Uri, type: String): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val extension = if (type == "video") ".mp4" else ".jpg"
            val tempFile = File.createTempFile("reel_upload_", extension, context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            Log.e("ReelCreate", "URI to File failed", e)
            null
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(reelCreated = false) }
    }
}

data class ReelCreateUiState(
    val selectedVideoUri: Uri? = null,
    val thumbnailUri: Uri? = null,
    val caption: String = "",
    val privacy: PrivacyB23Enum = PrivacyB23Enum.PUBLIC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val reelCreated: Boolean = false
)
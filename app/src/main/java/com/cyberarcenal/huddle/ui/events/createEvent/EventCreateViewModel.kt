package com.cyberarcenal.huddle.ui.events.createEvent

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cyberarcenal.huddle.api.models.EventType8c2Enum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class EventCreateViewModel(
    private val contentResolver: ContentResolver,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventCreateUiState())
    val uiState: StateFlow<EventCreateUiState> = _uiState.asStateFlow()

    fun setTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun setDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun setLocation(location: String) { _uiState.update { it.copy(location = location) } }
    fun setStartTime(start: OffsetDateTime) { _uiState.update { it.copy(startTime = start) } }
    fun setEndTime(end: OffsetDateTime) { _uiState.update { it.copy(endTime = end) } }
    fun setEventType(type: EventType8c2Enum) { _uiState.update { it.copy(eventType = type) } }
    fun setGroup(groupId: Int?) { _uiState.update { it.copy(groupId = groupId) } }
    fun setMaxAttendees(max: Long?) { _uiState.update { it.copy(maxAttendees = max) } }
    fun addMedia(uris: List<Uri>) {
        _uiState.update { it.copy(selectedMedia = it.selectedMedia + uris) }
    }
    fun removeMedia(uri: Uri) {
        _uiState.update { it.copy(selectedMedia = it.selectedMedia.filter { it != uri }) }
    }

    fun setError(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    fun createEvent() {
        val state = _uiState.value
        if (state.title.isBlank() || state.location.isBlank() || state.startTime == null || state.endTime == null) {
            _uiState.update { it.copy(error = "Please fill all required fields") }
            return
        }


        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Copy media files to internal storage
            val mediaPaths = withContext(Dispatchers.IO) {
                state.selectedMedia.mapNotNull { uri -> uriToFile(uri)?.absolutePath }
            }
            val mimeTypes = withContext(Dispatchers.IO) {
                state.selectedMedia.mapNotNull { contentResolver.getType(it) }
            }

            val clientId = UUID.randomUUID().toString()

            val inputData = workDataOf(
                EventUploadWorker.KEY_TITLE to state.title,
                EventUploadWorker.KEY_DESCRIPTION to state.description,
                EventUploadWorker.KEY_LOCATION to state.location,
                EventUploadWorker.KEY_START_TIME to state.startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                EventUploadWorker.KEY_END_TIME to state.endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                EventUploadWorker.KEY_EVENT_TYPE to state.eventType?.value,
                EventUploadWorker.KEY_GROUP to (state.groupId ?: 0),
                EventUploadWorker.KEY_MAX_ATTENDEES to (state.maxAttendees ?: 0),
                EventUploadWorker.KEY_MEDIA_PATHS to mediaPaths.toTypedArray(),
                EventUploadWorker.KEY_MIME_TYPES to mimeTypes.toTypedArray(),
                EventUploadWorker.KEY_CLIENT_ID to clientId
            )

            val workRequest = OneTimeWorkRequestBuilder<EventUploadWorker>()
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

            WorkManager.getInstance(context).enqueue(workRequest)

            _uiState.update { it.copy(isLoading = false, eventCreated = true) }
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
            val tempFile = File.createTempFile("event_upload_", extension, context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(eventCreated = false) }
    }
}

data class EventCreateUiState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startTime: OffsetDateTime? = null,
    val endTime: OffsetDateTime? = null,
    val eventType: EventType8c2Enum? = null,
    val groupId: Int? = null,
    val maxAttendees: Long? = null,
    val selectedMedia: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventCreated: Boolean = false
)
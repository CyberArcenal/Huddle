package com.cyberarcenal.huddle.ui.events.createEvent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cyberarcenal.huddle.api.models.EventType8c2Enum
import com.cyberarcenal.huddle.data.repositories.EventRepository
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EventUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "event_upload_channel"
        const val NOTIFICATION_ID = 105
        const val STATUS_NOTIFICATION_ID = 106

        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_LOCATION = "location"
        const val KEY_START_TIME = "start_time"
        const val KEY_END_TIME = "end_time"
        const val KEY_EVENT_TYPE = "event_type"
        const val KEY_GROUP = "group"
        const val KEY_MAX_ATTENDEES = "max_attendees"
        const val KEY_MEDIA_PATHS = "media_paths"
        const val KEY_MIME_TYPES = "mime_types"
        const val KEY_CLIENT_ID = "client_id"
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val description = inputData.getString(KEY_DESCRIPTION) ?: ""
        val location = inputData.getString(KEY_LOCATION) ?: ""
        val startTime = inputData.getString(KEY_START_TIME) ?: return Result.failure()
        val endTime = inputData.getString(KEY_END_TIME) ?: ""
        val eventTypeValue = inputData.getString(KEY_EVENT_TYPE)

        val groupId = inputData.getInt(KEY_GROUP, 0).takeIf { it != 0 }
        val maxAttendees = inputData.getLong(KEY_MAX_ATTENDEES, 0).takeIf { it > 0 }
        val mediaPaths = inputData.getStringArray(KEY_MEDIA_PATHS) ?: emptyArray()
        val mimeTypes = inputData.getStringArray(KEY_MIME_TYPES) ?: emptyArray()
        val clientId = inputData.getString(KEY_CLIENT_ID)

        setForeground(createForegroundInfo("Creating your event..."))

        val repository = EventRepository()

        // Convert media files to MultipartBody.Part
        val mediaParts = mediaPaths.mapIndexedNotNull { index, path ->
            val file = File(path)
            if (!file.exists()) return@mapIndexedNotNull null
            val mimeType = mimeTypes.getOrNull(index) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("media", file.name, requestFile)
        }

        // Map event type if provided
        val eventTypeEnum = eventTypeValue?.let { EventType8c2Enum.decode(it) }

        val createResult = repository.createEvent(
            title = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            eventType = eventTypeEnum,
            group = groupId,
            maxAttendees = maxAttendees,
            media = mediaParts.takeIf { it.isNotEmpty() },
            clientId = clientId
        )

        return createResult.fold(
            onSuccess = { response ->
                if (response.status && response.data != null) {
                    val eventId = response.data.id
                    val processed = waitForProcessing(eventId, repository)
                    if (processed) {
                        showFinishedNotification("Your event is ready!")
                    } else {
                        showFinishedNotification("Event creation completed but processing timed out. It will appear shortly.")
                    }
                    Result.success()
                } else {
                    showFinishedNotification("Failed to create event: ${response.message}")
                    Result.retry()
                }
            },
            onFailure = { error ->
                showFinishedNotification("Failed to create event: ${error.message}")
                Result.retry()
            }
        )
    }

    private suspend fun waitForProcessing(eventId: Int, repository: EventRepository): Boolean {
        var attempts = 0
        val maxAttempts = 30 // 90 seconds with 3s delay
        val delaySeconds = 3L

        while (attempts < maxAttempts) {
            delay(delaySeconds * 1000)
            val statusResult = repository.checkCreateStatus(eventId)
            if (statusResult.isSuccess) {
                val status = statusResult.getOrNull()!!
                if (status.status && status.data != null) {
                    if (!status.data.processing) {
                        return true
                    }
                    updateProgressNotification(status.data.processing)
                }
            }
            attempts++
        }
        return false
    }

    private fun updateProgressNotification(isProcessing: Boolean) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Huddle")
            .setContentText(if (isProcessing) "Still processing your event..." else "Almost ready...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event Creation",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Huddle")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun showFinishedNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Huddle")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(STATUS_NOTIFICATION_ID, notification)
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
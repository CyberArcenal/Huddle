package com.cyberarcenal.huddle.ui.reel.create

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.data.repositories.ReelsRepository
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

fun mapStringToPrivacyEnum(privacyString: String): PrivacyB23Enum {
    return when (privacyString) {
        "public" -> PrivacyB23Enum.PUBLIC
        "followers" -> PrivacyB23Enum.FOLLOWERS
        "secret" -> PrivacyB23Enum.SECRET
        else -> PrivacyB23Enum.PUBLIC
    }
}

class ReelUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "reel_upload_channel"
        const val NOTIFICATION_ID = 103
        const val STATUS_NOTIFICATION_ID = 104

        const val KEY_CAPTION = "caption"
        const val KEY_VIDEO_PATH = "video_path"
        const val KEY_THUMBNAIL_PATH = "thumbnail_path"
        const val KEY_PRIVACY = "privacy"
        const val KEY_CLIENT_ID = "client_id"
    }

    override suspend fun doWork(): Result {
        val caption = inputData.getString(KEY_CAPTION) ?: ""
        val videoPath = inputData.getString(KEY_VIDEO_PATH) ?: return Result.failure()
        val thumbnailPath = inputData.getString(KEY_THUMBNAIL_PATH)
        val privacyValue = inputData.getString(KEY_PRIVACY) ?: PrivacyB23Enum.PUBLIC.value
        val clientId = inputData.getString(KEY_CLIENT_ID)

        setForeground(createForegroundInfo("Uploading your reel..."))
        val repository = ReelsRepository()

        return try {
            val videoFile = File(videoPath)

            // Calculate actual video duration
            val retriever = MediaMetadataRetriever()
            val durationInSeconds = try {
                retriever.setDataSource(videoPath)
                val timeMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                (timeMs?.toLong() ?: 0L) / 1000.0
            } catch (e: Exception) {
                0.0
            } finally {
                retriever.release()
            }

            val videoPart = MultipartBody.Part.createFormData(
                "media",
                videoFile.name,
                videoFile.asRequestBody("video/*".toMediaTypeOrNull())
            )

            val thumbnailPart = thumbnailPath?.let {
                val file = File(it)
                MultipartBody.Part.createFormData(
                    "thumbnail",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
            }

            // Create reel (with client_id for idempotency)
            val createResult = repository.createReel(
                caption = caption,
                media = videoPart,
                thumbnail = thumbnailPart,
                audio = null,   // audio not used in this version
                duration = durationInSeconds.toInt(),
                privacy = mapStringToPrivacyEnum(privacyValue),
                clientId = clientId
            )

            createResult.fold(
                onSuccess = { response ->
                    if (response.status && response.data != null) {
                        val reelId = response.data.id
                        // Wait for processing to complete
                        val processed = waitForProcessing(reelId, repository)
                        if (processed) {
                            showFinishedNotification("Your reel is ready!")
                        } else {
                            showFinishedNotification("Reel upload completed but processing timed out. It will appear shortly.")
                        }
                        Result.success()
                    } else {
                        showFinishedNotification("Upload failed: ${response.message}")
                        Result.retry()
                    }
                },
                onFailure = { error ->
                    showFinishedNotification("Failed to upload reel: ${error.message}")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            showFinishedNotification("Failed to upload reel: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun waitForProcessing(reelId: Int, repository: ReelsRepository): Boolean {
        var attempts = 0
        val maxAttempts = 30 // 30 * 3 sec = 90 seconds
        val delaySeconds = 3L

        while (attempts < maxAttempts) {
            delay(delaySeconds * 1000)
            val statusResult = repository.checkUploadStatus(reelId)
            if (statusResult.isSuccess) {
                val status = statusResult.getOrNull()!!
                if (status.status && status.data != null) {
                    if (!status.data.processing) {
                        // Processing done
                        return true
                    }
                    // Still processing: update notification
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
            .setContentText(if (isProcessing) "Still processing your reel..." else "Almost ready...")
            .setSmallIcon(R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reel Uploads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Huddle")
            .setContentText(message)
            .setSmallIcon(R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun showFinishedNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Huddle")
            .setContentText(message)
            .setSmallIcon(R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(STATUS_NOTIFICATION_ID, notification)
        // Remove the foreground notification
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
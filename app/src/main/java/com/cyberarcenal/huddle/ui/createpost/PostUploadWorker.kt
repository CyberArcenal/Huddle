package com.cyberarcenal.huddle.ui.createpost

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cyberarcenal.huddle.api.models.PostCreateResponse
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

class PostUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "post_upload_channel"
        const val NOTIFICATION_ID = 101
        const val STATUS_NOTIFICATION_ID = 102

        const val KEY_CONTENT = "content"
        const val KEY_PRIVACY = "privacy"
        const val KEY_POST_TYPE = "post_type"
        const val KEY_MEDIA_PATHS = "media_paths"
        const val KEY_MIME_TYPES = "mime_types"
        const val KEY_GROUP = "group"
        const val KEY_CLIENT_ID = "client_id"   // new
    }

    override suspend fun doWork(): Result {
        val content = inputData.getString(KEY_CONTENT) ?: ""
        val privacyValue = inputData.getString(KEY_PRIVACY) ?: "public"
        val postTypeValue = inputData.getString(KEY_POST_TYPE) ?: "image"
        val mediaPaths = inputData.getStringArray(KEY_MEDIA_PATHS) ?: emptyArray()
        val mimeTypes = inputData.getStringArray(KEY_MIME_TYPES) ?: emptyArray()
        val groupId = inputData.getInt(KEY_GROUP, 0).takeIf { it != 0 }
        val clientId = inputData.getString(KEY_CLIENT_ID) ?: UUID.randomUUID().toString()

        setForeground(createForegroundInfo("Uploading your post..."))

        val repository = UserPostsRepository()

        // Convert media files to MultipartBody.Part
        val mediaParts = mediaPaths.mapIndexedNotNull { index, path ->
            val file = File(path)
            if (!file.exists()) return@mapIndexedNotNull null
            val mimeType = mimeTypes.getOrNull(index) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("media", file.name, requestFile)
        }

        // Create post with client_id
        val createResult = repository.createPost(
            content = content,
            postType = getPostTypeEnumByString(postTypeValue),
            privacy = getPrivacyByString(privacyValue),
            groupId = groupId,
            mediaParts = mediaParts,
            mimeTypes = mimeTypes.toList(),
            clientId = clientId
        )

        return if (createResult.isSuccess) {
            val response = createResult.getOrNull()!!
            if (response.status && response.data != null) {
                val postId = response.data.id
                // Poll for processing status
                val finalStatus = waitForProcessing(postId, repository)
                if (finalStatus) {
                    showFinishedNotification("Your post is ready!")
                    Result.success()
                } else {
                    showFinishedNotification("Post upload completed but processing timed out. It will appear shortly.")
                    Result.success() // still success, just not ready yet
                }
            } else {
                showFinishedNotification("Upload failed: ${response.message}")
                Result.retry()
            }
        } else {
            val error = createResult.exceptionOrNull()?.message ?: "Unknown error"
            showFinishedNotification("Upload failed: $error")
            Result.retry()
        }
    }

    private suspend fun waitForProcessing(postId: Int, repository: UserPostsRepository): Boolean {
        var attempts = 0
        val maxAttempts = 30 // 30 * 3 sec = 90 seconds
        val delaySeconds = 3L

        while (attempts < maxAttempts) {
            delay(delaySeconds * 1000)
            val statusResult = repository.checkUploadStatus(postId)
            if (statusResult.isSuccess) {
                val status = statusResult.getOrNull()!!
                if (status.status && status.data != null) {
                    if (!status.data.processing) {
                        // Processing done
                        return true
                    }
                    // Still processing: update notification with progress
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
            .setContentText(if (isProcessing) "Still processing your post..." else "Almost ready...")
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
                "Post Uploads",
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
        // Also remove the foreground notification
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

// Helper functions remain the same
fun getPostTypeEnumByString(type: String?): PostTypeEnum {
    return when (type) {
        PostTypeEnum.TEXT.value -> PostTypeEnum.TEXT
        PostTypeEnum.IMAGE.value -> PostTypeEnum.IMAGE
        PostTypeEnum.VIDEO.value -> PostTypeEnum.VIDEO
        PostTypeEnum.POLL.value -> PostTypeEnum.POLL
        PostTypeEnum.SHARE.value -> PostTypeEnum.SHARE
        else -> PostTypeEnum.TEXT
    }
}

fun getPrivacyByString(privacy: String?): PrivacyB23Enum {
    return when (privacy) {
        PrivacyB23Enum.PUBLIC.value -> PrivacyB23Enum.PUBLIC
        PrivacyB23Enum.FOLLOWERS.value -> PrivacyB23Enum.FOLLOWERS
        PrivacyB23Enum.SECRET.value -> PrivacyB23Enum.SECRET
        else -> PrivacyB23Enum.PUBLIC
    }
}
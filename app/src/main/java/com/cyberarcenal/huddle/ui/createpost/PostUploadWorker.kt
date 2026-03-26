package com.cyberarcenal.huddle.ui.createpost

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.data.repositories.PostCreateRequestWithMedia
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import java.io.File

class PostUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "post_upload_channel"
        const val NOTIFICATION_ID = 101
        
        const val KEY_CONTENT = "content"
        const val KEY_PRIVACY = "privacy"
        const val KEY_POST_TYPE = "post_type"
        const val KEY_MEDIA_PATHS = "media_paths"
        const val KEY_MIME_TYPES = "mime_types"
    }

    override suspend fun doWork(): Result {
        val content = inputData.getString(KEY_CONTENT)
        val privacyValue = inputData.getString(KEY_PRIVACY) ?: PrivacyB23Enum.PUBLIC.value
        val postTypeValue = inputData.getString(KEY_POST_TYPE) ?: PostTypeEnum.TEXT.value
        val mediaPaths = inputData.getStringArray(KEY_MEDIA_PATHS)
        val mimeTypes = inputData.getStringArray(KEY_MIME_TYPES)

        val privacy = PrivacyB23Enum.values().find { it.value == privacyValue } ?: PrivacyB23Enum.PUBLIC
        val postType = PostTypeEnum.values().find { it.value == postTypeValue } ?: PostTypeEnum.TEXT

        setForeground(createForegroundInfo("Uploading your post..."))

        val repository = UserPostsRepository()
        val mediaFiles = mediaPaths?.map { File(it) }

        val request = PostCreateRequestWithMedia(
            content = content,
            postType = postType,
            privacy = privacy,
            mediaFiles = mediaFiles,
            mimeTypes = mimeTypes?.toList()
        )

        val result = repository.createPost(request)

        return if (result.isSuccess) {
            showFinishedNotification("Post uploaded successfully!")
            Result.success()
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            showFinishedNotification("Failed to upload post: $error")
            Result.retry()
        }
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

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}

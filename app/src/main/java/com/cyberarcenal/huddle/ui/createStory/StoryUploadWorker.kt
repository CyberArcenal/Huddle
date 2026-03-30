package com.cyberarcenal.huddle.ui.createStory

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import com.cyberarcenal.huddle.data.repositories.StoryCreateRequestWithMedia
import java.io.File

class StoryUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "story_upload_channel"
        const val NOTIFICATION_ID = 102

        const val KEY_CONTENT = "content"
        const val KEY_STORY_TYPE = "story_type"
        const val KEY_MEDIA_PATH = "media_path"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_EXPIRES_IN = "expires_in"
    }

    override suspend fun doWork(): Result {
        val content = inputData.getString(KEY_CONTENT)
        val storyTypeValue = inputData.getString(KEY_STORY_TYPE) ?: StoryTypeEnum.TEXT.value
        val mediaPath = inputData.getString(KEY_MEDIA_PATH)
        val mimeType = inputData.getString(KEY_MIME_TYPE)
        val expiresInHours = inputData.getInt(KEY_EXPIRES_IN, 24)

        val storyType = StoryTypeEnum.entries.find { it.value == storyTypeValue } ?: StoryTypeEnum.TEXT

        setForeground(createForegroundInfo("Uploading your story..."))

        val repository = StoriesRepository()
        val mediaFile = mediaPath?.let { File(it) }

        val request = StoryCreateRequestWithMedia(
            storyType = storyType,
            content = content,
            mediaFile = mediaFile,
            mimeType = mimeType,
            expiresInHours = expiresInHours
        )

        val result = repository.createStory(request)

        return if (result.isSuccess) {
            showFinishedNotification("Story uploaded successfully!")
            Result.success()
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            showFinishedNotification("Failed to upload story: $error")
            Result.retry()
        }
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Story Uploads",
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
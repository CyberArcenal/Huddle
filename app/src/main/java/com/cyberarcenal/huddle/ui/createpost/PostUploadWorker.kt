package com.cyberarcenal.huddle.ui.createpost

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cyberarcenal.huddle.api.models.PostCreateRequest
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
        const val KEY_GROUP = "group"
    }

    override suspend fun doWork(): Result {
        val content = inputData.getString(KEY_CONTENT) ?: ""
        val privacyValue = inputData.getString(KEY_PRIVACY) ?: "public"
        val postTypeValue = inputData.getString(KEY_POST_TYPE) ?: "image"
        val mediaPaths = inputData.getStringArray(KEY_MEDIA_PATHS) ?: emptyArray()
        val mimeTypes = inputData.getStringArray(KEY_MIME_TYPES) ?: emptyArray()
        val groupId = inputData.getInt(KEY_GROUP, 0).takeIf { it != 0 }

        setForeground(createForegroundInfo("Uploading your post..."))

        val repository = UserPostsRepository()

        // 1. Convert media files to MultipartBody.Part
        val mediaParts = mediaPaths.mapIndexedNotNull { index, path ->
            val file = File(path)
            if (!file.exists()) return@mapIndexedNotNull null

            val mimeType = mimeTypes.getOrNull(index) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("media", file.name, requestFile)
        }

        // 2. Create request using raw values (HINDI enum objects)
        val result = repository.createPost(
            content = content,
            postType = postTypeValue,        // String na "image", "video", etc.
            privacy = privacyValue,          // String na "public", "followers", etc.
            groupId = groupId,
            mediaParts = mediaParts,
            mimeTypes = mimeTypes.toList()
        )

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
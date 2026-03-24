package com.cyberarcenal.huddle.utils

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object FileUtils {
    data class MultipartResult(
        val part: MultipartBody.Part,
        val tempFile: File  // Para ma-delete after upload
    )

    suspend fun uriToMultipartPart(
        context: Context,
        uri: Uri,
        partName: String = "image"
    ): MultipartResult? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(partName, tempFile.name, requestFile)
            MultipartResult(part, tempFile)
        } catch (e: Exception) {
            null
        }
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            // For content:// Uris, copy to a temporary file
            if (uri.scheme == "content") {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } else {
                File(uri.path)
            }
        } catch (e: Exception) {
            null
        }
    }
}
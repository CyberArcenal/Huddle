package com.cyberarcenal.huddle.utils

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val width: Int = 0,
        val height: Int = 0,
        val fileSize: Long = 0
    )

    suspend fun validateImage(
        contentResolver: ContentResolver,
        uri: Uri,
        maxWidth: Int = 2048,
        maxHeight: Int = 2048,
        maxSizeMB: Int = 8
    ): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val fileSize = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
            val maxSizeBytes = maxSizeMB * 1024 * 1024
            if (fileSize > maxSizeBytes) {
                return@withContext ValidationResult(
                    isValid = false,
                    errorMessage = "Image too large (max ${maxSizeMB}MB)"
                )
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            val width = options.outWidth
            val height = options.outHeight

            if (width == 0 || height == 0) {
                return@withContext ValidationResult(
                    isValid = false,
                    errorMessage = "Invalid image format"
                )
            }

            if (width > maxWidth || height > maxHeight) {
                return@withContext ValidationResult(
                    isValid = false,
                    errorMessage = "Image dimensions too large (max ${maxWidth}x${maxHeight})"
                )
            }

            ValidationResult(
                isValid = true,
                width = width,
                height = height,
                fileSize = fileSize
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errorMessage = "Failed to read image: ${e.message}"
            )
        }
    }

    // ✅ This function is now `suspend` and can safely call other suspend functions
    suspend fun validateAndConvertToPart(
        context: android.content.Context,
        uri: Uri,
        partName: String = "image"
    ): FileUtils.MultipartResult? {
        // First validate
        val validation = validateImage(context.contentResolver, uri)
        if (!validation.isValid) {
            return null
        }
        // Then convert (uriToMultipartPart is suspend, so it's fine here)
        return FileUtils.uriToMultipartPart(context, uri, partName)
    }
}
// UserMediaRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserMediaRepository {
    private val api = ApiService.userMediaApi

    // ------------------------------------------------------------------------
    // Upload cover photo using multipart
    suspend fun uploadProfilePicture(file: File, mimeType: String = "image/jpeg"): Result<UserImageDisplay> {
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestFile)
        return safeApiCall { ApiService.profilePictureUploadApi.uploadProfilePicture(part) }
    }

    suspend fun uploadCoverPhoto(file: File, mimeType: String = "image/jpeg"): Result<UserImageDisplay> {
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestFile)
        return safeApiCall { ApiService.coverPhotoUploadApi.uploadCoverPhoto(part) }
    }

    // ------------------------------------------------------------------------
    // Other methods remain unchanged
    suspend fun getCoverPhoto(userId: Int, userId2: Int? = null): Result<UserImageMinimal> =
        safeApiCall { api.apiV1UsersMediaCoverPhotoRetrieve(userId, userId2) }

    suspend fun getProfilePicture(userId: Int, userId2: Int? = null): Result<UserImageMinimal> =
        safeApiCall { api.apiV1UsersMediaProfilePictureRetrieve(userId, userId2) }

    suspend fun removeCoverPhoto(): Result<UserImageMinimal> =
        safeApiCall { api.apiV1UsersMediaRemoveCoverPhotoCreate() }

    suspend fun removeProfilePicture(): Result<UserImageMinimal> =
        safeApiCall { api.apiV1UsersMediaRemoveProfilePictureCreate() }

    suspend fun getUserMediaGrid(userId: Int, page: Int, pageSize: Int): Result<PaginatedUserMediaGrid> =
        safeApiCall { api.apiV1UsersUsersMediaRetrieve(userId, page, pageSize) }

    suspend fun getMyMediaGrid(page: Int, pageSize: Int): Result<PaginatedUserMediaGrid> =
        safeApiCall { api.apiV1UsersMeMediaRetrieve(page, pageSize) }
}
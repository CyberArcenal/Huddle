// UserMediaRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody

class UserMediaRepository {
    private val api = ApiService.userMediaApi

    suspend fun uploadCoverPhoto(imageFile: MultipartBody.Part): Result<CoverPhotoUploadResponse> =
        safeApiCall { api.apiV1UsersMediaCoverPhotoCreate(imageFile) }

    suspend fun getCoverPhoto(userId: Int, userId2: Int? = null): Result<GetCoverPhotoResponse> =
        safeApiCall { api.apiV1UsersMediaCoverPhotoRetrieve(userId, userId2) }

    suspend fun uploadProfilePicture(
        imageFile: MultipartBody.Part,
        cropX: Int? = 0,
        cropY: Int? = 0,
        cropWidth: Int? = null,
        cropHeight: Int? = null
    ): Result<ProfilePictureUploadResponse> =
        safeApiCall { api.apiV1UsersMediaProfilePictureCreate(imageFile, cropX, cropY, cropWidth, cropHeight) }

    suspend fun getProfilePicture(userId: Int, userId2: Int? = null): Result<ProfilePictureResponse> =
        safeApiCall { api.apiV1UsersMediaProfilePictureRetrieve(userId, userId2) }

    suspend fun removeCoverPhoto(): Result<RemoveCoverPhotoResponse> =
        safeApiCall { api.apiV1UsersMediaRemoveCoverPhotoCreate() }

    suspend fun removeProfilePicture(): Result<RemoveProfilePictureResponse> =
        safeApiCall { api.apiV1UsersMediaRemoveProfilePictureCreate() }
}
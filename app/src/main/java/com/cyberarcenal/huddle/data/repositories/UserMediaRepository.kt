package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

interface CoverPhotoUploadApi {
    @Multipart
    @POST("api/v1/users/media/cover-photo/")
    suspend fun apiV1UsersMediaCoverPhotoCreate(
        @Part image: MultipartBody.Part,
        @Part("image_type") imageType: RequestBody,
        @Part("caption") caption: RequestBody? = null,
        @Part("privacy") privacy: RequestBody? = null,
        @Part("crop_x") cropX: RequestBody? = null,
        @Part("crop_y") cropY: RequestBody? = null,
        @Part("crop_width") cropWidth: RequestBody? = null,
        @Part("crop_height") cropHeight: RequestBody? = null
    ): Response<UserImageUploadResponse>
}

interface ProfilePictureUploadApi {
    @Multipart
    @POST("api/v1/users/media/profile-picture/")
    suspend fun apiV1UsersMediaProfilePictureCreate(
        @Part image: MultipartBody.Part,
        @Part("image_type") imageType: RequestBody,
        @Part("caption") caption: RequestBody? = null,
        @Part("privacy") privacy: RequestBody? = null,
        @Part("crop_x") cropX: RequestBody? = null,
        @Part("crop_y") cropY: RequestBody? = null,
        @Part("crop_width") cropWidth: RequestBody? = null,
        @Part("crop_height") cropHeight: RequestBody? = null
    ): Response<UserImageUploadResponse>
}

class UserMediaRepository {
    private val api = ApiService.userMediaApi
    private val profilePictureApi: ProfilePictureUploadApi = ApiService.profilePictureUploadApi
    private val coverPhotoApi: CoverPhotoUploadApi = ApiService.coverPhotoUploadApi

    suspend fun uploadProfilePicture(
        file: File,
        mimeType: String = "image/jpeg",
        caption: String? = null,
        privacy: PrivacyB23Enum? = null,
        cropX: Int? = null,
        cropY: Int? = null,
        cropWidth: Int? = null,
        cropHeight: Int? = null
    ): Result<UserImageUploadResponse> {
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestFile)

        return safeApiCall {
            profilePictureApi.apiV1UsersMediaProfilePictureCreate(
                image = part,
                imageType = ImageTypeEnum.PROFILE.value.toRequestBody("text/plain".toMediaTypeOrNull()),
                caption = caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
                privacy = privacy?.value?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropX = cropX?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropY = cropY?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropWidth = cropWidth?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropHeight = cropHeight?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            )
        }
    }

    suspend fun uploadCoverPhoto(
        file: File,
        mimeType: String = "image/jpeg",
        caption: String? = null,
        privacy: PrivacyB23Enum? = null,
        cropX: Int? = null,
        cropY: Int? = null,
        cropWidth: Int? = null,
        cropHeight: Int? = null
    ): Result<UserImageUploadResponse> {
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", file.name, requestFile)

        return safeApiCall {
            coverPhotoApi.apiV1UsersMediaCoverPhotoCreate(
                image = part,
                imageType = ImageTypeEnum.COVER.value.toRequestBody("text/plain".toMediaTypeOrNull()),
                caption = caption?.toRequestBody("text/plain".toMediaTypeOrNull()),
                privacy = privacy?.value?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropX = cropX?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropY = cropY?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropWidth = cropWidth?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                cropHeight = cropHeight?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            )
        }
    }

    suspend fun getCoverPhoto(userId: Int, userId2: Int? = null): Result<UserImageGetResponse> =
        safeApiCall { api.apiV1UsersMediaCoverPhotoRetrieve(userId, userId2) }

    suspend fun getProfilePicture(userId: Int, userId2: Int? = null): Result<UserImageGetResponse> =
        safeApiCall { api.apiV1UsersMediaProfilePictureRetrieve(userId, userId2) }

    suspend fun removeCoverPhoto(): Result<UserImageRemoveResponse> =
        safeApiCall { api.apiV1UsersMediaRemoveCoverPhotoCreate() }

    suspend fun removeProfilePicture(): Result<UserImageRemoveResponse> =
        safeApiCall { api.apiV1UsersMediaRemoveProfilePictureCreate() }

    suspend fun getUserMediaGrid(contentType: String?=null,userId: Int, page: Int, pageSize: Int): Result<UserMediaGridResponse> =
        safeApiCall { api.apiV1UsersUsersMediaRetrieve(userId, contentType, page, pageSize) }

    suspend fun getMyMediaGrid(contentType: String? = null, page: Int, pageSize: Int): Result<UserMediaGridResponse> =
        safeApiCall { api.apiV1UsersMeMediaRetrieve(contentType, page, pageSize) }
}
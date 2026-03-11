package com.cyberarcenal.huddle.data.repositories.users

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

class ProfileRepository {
    private val api = ApiService.v1Api
    private val multipartApi = ApiService.retrofit.create(ProfileMultipartApi::class.java)

    // ========== PROFILE ==========
    suspend fun getCurrentUserProfile(): Result<UserProfile> = safeApiCall {
        api.v1UsersProfileRetrieve()
    }

    suspend fun getUserProfile(userId: Int): Result<UserProfile> = safeApiCall {
        api.v1UsersProfileRetrieve2(userId)
    }

    suspend fun getUserPosts(userId: Int, page: Int, pageSize: Int): Result<PaginatedPostFeed> = safeApiCall {
        api.v1FeedPostsRetrieve(userId = userId, page = page, pageSize = pageSize)
    }

    suspend fun followUser(userId: Int?): Result<Any> = safeApiCall {
        api.v1UsersFollowCreate(FollowUserRequest(followingId = userId))
    }

    suspend fun unfollowUser(userId: Int?): Result<Any> = safeApiCall {
        api.v1UsersUnfollowCreate(UnfollowUserRequest(followingId = userId))
    }

    suspend fun updateUserProfile(update: UserProfileSchemaUpdateRequest): Result<V1UsersAdminUsersUpdate200Response> = safeApiCall {
        api.v1UsersProfileUpdate(update)
    }

    // ========== PROFILE PICTURE ==========
    suspend fun uploadProfilePicture(imagePart: MultipartBody.Part): Result<Any> = safeApiCall {
        multipartApi.uploadProfilePicture(imagePart)
    }

    suspend fun removeProfilePicture(): Result<Any> = safeApiCall {
        api.v1UsersMediaRemoveProfilePictureCreate()
    }

    suspend fun getProfilePictureUrl(userId: Int): Result<ProfilePictureResponse> = safeApiCall {
        api.v1UsersMediaProfilePictureRetrieve(userId)
    }

    // ========== COVER PHOTO ==========
    suspend fun uploadCoverPhoto(imagePart: MultipartBody.Part): Result<Any> = safeApiCall {
        multipartApi.uploadCoverPhoto(imagePart)
    }

    suspend fun removeCoverPhoto(): Result<Any> = safeApiCall {
        api.v1UsersMediaRemoveCoverPhotoCreate()
    }

    suspend fun getCoverPhotoUrl(userId: Int): Result<GetCoverPhotoResponse> = safeApiCall {
        api.v1UsersMediaCoverPhotoRetrieve(userId)
    }

    // ========== VALIDATION ==========
    suspend fun validateImage(imagePart: MultipartBody.Part): Result<Any> = safeApiCall {
        api.v1UsersMediaValidateImageCreate(imagePart)
    }
}

// Multipart interface (unchanged)
interface ProfileMultipartApi {
    @Multipart
    @POST("api/v1/users/media/profile-picture/")
    suspend fun uploadProfilePicture(
        @Part image: MultipartBody.Part
    ): Response<Any>

    @Multipart
    @POST("api/v1/users/media/cover-photo/")
    suspend fun uploadCoverPhoto(
        @Part image: MultipartBody.Part
    ): Response<Any>
}
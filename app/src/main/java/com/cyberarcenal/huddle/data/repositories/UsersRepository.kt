package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class UsersRepository {
    private val api = ApiService.usersApi

    suspend fun checkEmail(email: String): Result<CheckEmailResponse> =
        safeApiCall { api.apiV1UsersCheckEmailRetrieve(email) }

    suspend fun checkUsername(username: String): Result<CheckUsernameResponse> =
        safeApiCall { api.apiV1UsersCheckUsernameRetrieve(username) }

    suspend fun deactivate(request: UserDeactivateInputRequest): Result<UserStatusUpdateResponse> =
        safeApiCall { api.apiV1UsersDeactivateCreate(request) }

    suspend fun getProfile(): Result<UserProfile> =
        safeApiCall { api.apiV1UsersProfileRetrieve() }

    suspend fun getPublicProfile(userId: Int): Result<UserProfile> =
        safeApiCall { api.apiV1UsersProfileRetrieve2(userId) }

    suspend fun updateProfile(request: UserProfileSchemaUpdateRequest? = null): Result<UserProfileResponse> =
        safeApiCall { api.apiV1UsersProfileUpdate(request) }

    suspend fun register(request: UserRegisterRequest): Result<Map<String, Any>> =
        safeApiCall { api.apiV1UsersRegisterCreate(request) }

    suspend fun updateStatus(request: UserStatusRequest): Result<UserStatusUpdateResponse> =
        safeApiCall { api.apiV1UsersStatusUpdateCreate(request) }

    suspend fun verify(): Result<VerifyUserResponse> =
        safeApiCall { api.apiV1UsersVerifyCreate() }

    suspend fun resendEmailVerification(resendRequest: ResendRequest): Result<ResendVerificationResponse> =
        safeApiCall { api.apiV1UsersResendVerificationCreate(resendRequest) }

    suspend fun verifyEmail(verifyRequest: VerifyEmailRequest): Result<VerifyEmailResponse> = safeApiCall {
        api.apiV1UsersVerifyEmailCreate(verifyRequest)
    }

}
package com.cyberarcenal.huddle.data.repositories

import android.content.Context
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.local.HuddleDatabase
import com.cyberarcenal.huddle.data.local.entities.ProfileEntity
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UsersRepository(context: Context? = null) {
    private val api = ApiService.usersApi
    private val profileDao = context?.let { HuddleDatabase.getDatabase(it).profileDao() }

    suspend fun checkEmail(email: String): Result<CheckEmailResponse> =
        safeApiCall { api.apiV1UsersCheckEmailRetrieve(email) }

    suspend fun checkUsername(username: String): Result<CheckUsernameResponse> =
        safeApiCall { api.apiV1UsersCheckUsernameRetrieve(username) }

    suspend fun deactivate(request: UserDeactivateInputRequest): Result<UserDeactivateResponse> =
        safeApiCall { api.apiV1UsersDeactivateCreate(request) }

    suspend fun getProfile(): Result<UserProfileResponse> =
        safeApiCall { api.apiV1UsersProfileRetrieve() }.also { result ->
            result.onSuccess { response ->
                if (response.status) {
                    val user = response.data.user
                    user.id?.let { id ->
                        // Save to Room for caching
                        profileDao?.insertProfile(
                            ProfileEntity(
                                id = id,
                                username = user.username,
                                profilePictureUrl = user.profilePictureUrl,
                                coverPhotoUrl = user.coverPhotoUrl,
                                bio = user.bio,
                                rawData = user
                            )
                        )
                    }
                }
            }
        }

    fun getLocalProfile(userId: Int): Flow<UserProfile?>? {
        return profileDao?.getProfile(userId)?.map { it?.rawData }
    }

    suspend fun getPublicProfile(userId: Int): Result<UserProfileResponse> =
        safeApiCall { api.apiV1UsersProfileRetrieve2(userId) }.also { result ->
            result.onSuccess { response ->
                if (response.status) {
                    val user = response.data.user
                    user.id?.let { id ->
                        profileDao?.insertProfile(
                            ProfileEntity(
                                id = id,
                                username = user.username,
                                profilePictureUrl = user.profilePictureUrl,
                                coverPhotoUrl = user.coverPhotoUrl,
                                bio = user.bio,
                                rawData = user
                            )
                        )
                    }
                }
            }
        }


    /**
     * Observe profile from local database – ito ang single source of truth.
     * Kapag walang laman ang DB, mag-e-emit ng null.
     */
    fun observeProfile(userId: Int): Flow<UserProfile?>? {
        return profileDao?.getProfile(userId)?.map { entity -> entity?.rawData }
    }

    /**
     * Refresh profile: tawag sa network, i-save sa DB, at ibalik ang result.
     * Hindi na kailangan ng forceRefresh dahil palaging tatawag sa network.
     */
    suspend fun refreshProfile(userId: Int, context: Context): Result<UserProfile> {
        // Alamin kung sariling profile o hindi
        val currentUserId = getCurrentUserId(context) // implement this
        val isOwn = (userId == currentUserId)

        val apiResult = if (isOwn) {
            getProfile()
        } else {
            getPublicProfile(userId)
        }

        return apiResult.map { response ->
            response.data.user.also { user ->
                // Na-save na sa database sa loob ng getProfile/getPublicProfile
                // Pero siguraduhin natin na may id at na-save.
                user.id?.let { id ->
                    profileDao?.insertProfile(
                        ProfileEntity(
                            id = id,
                            username = user.username,
                            profilePictureUrl = user.profilePictureUrl,
                            coverPhotoUrl = user.coverPhotoUrl,
                            bio = user.bio,
                            rawData = user
                        )
                    )
                }
            }
        }
    }

    // Helper para makuha ang current user ID (galing sa TokenManager o sa parameter)
    private suspend fun getCurrentUserId(context: Context): Int? {
        // I-implement base sa iyong token management
        return TokenManager.getUser(context = context)?.id
    }

    suspend fun updateProfile(request: UserProfileSchemaUpdateRequest? = null): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersProfileUpdate(request) }

    suspend fun register(request: UserRegisterRequest): Result<UserRegisterResponse> =
        safeApiCall { api.apiV1UsersRegisterCreate(request) }

    suspend fun updateStatus(request: UserStatusRequest): Result<UserStatusUpdateResponse> =
        safeApiCall { api.apiV1UsersStatusUpdateCreate(request) }

    suspend fun verify(): Result<VerifyUserResponse> = safeApiCall { api.apiV1UsersVerifyCreate() }

    suspend fun resendEmailVerification(resendRequest: ResendRequest): Result<ResendVerificationResponse> =
        safeApiCall { api.apiV1UsersResendVerificationCreate(resendRequest) }

    suspend fun verifyEmail(verifyRequest: VerifyEmailRequest): Result<EmailVerificationResponse> =
        safeApiCall { api.apiV1UsersVerifyEmailCreate(verifyRequest) }


    // Idagdag ang mga sumusunod na function sa loob ng UsersRepository class

    suspend fun updateBio(request: UpdateBioUpdateBioInputRequest? = null): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateBioUpdate(request) }

    suspend fun updateDateOfBirth(request: UpdateDateOfBirthUpdateDateOfBirthInputRequest? = null): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateDateOfBirthUpdate(request) }

    suspend fun updateEmail(request: UpdateEmailInputRequest): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateEmailUpdate(request) }

    suspend fun updateFirstName(request: UpdateFirstNameUpdateFirstNameInputRequest): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateFirstNameUpdate(request) }

    suspend fun updateLastName(request: UpdateLastNameUpdateLastNameInputRequest): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateLastNameUpdate(request) }

    suspend fun updateLocation(request: UpdateLocationUpdateLocationInputRequest? = null): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateLocationUpdate(request) }

    suspend fun updatePhoneNumber(request: UpdatePhoneNumberUpdatePhoneNumberInputRequest? = null): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdatePhoneNumberUpdate(request) }

    suspend fun updateUsername(request: UpdateUsernameInputRequest): Result<UserUpdateResponse> =
        safeApiCall { api.apiV1UsersUpdateUsernameUpdate(request) }

    suspend fun getNameEditStatus(): Result<NameEditStatusResponse> =
        safeApiCall { api.apiV1UsersNameEditStatusRetrieve() }

    suspend fun changeFullName(request: UpdateFullNameInputRequest): Result<UpdateFullNameResponse> =
        safeApiCall {
            api.apiV1UsersUpdateFullNameUpdate(request)
        }

}

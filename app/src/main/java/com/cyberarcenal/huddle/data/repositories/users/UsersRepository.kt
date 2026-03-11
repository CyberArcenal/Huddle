package com.cyberarcenal.huddle.data.repositories.users

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody
import java.util.UUID

class UsersRepository {
    private val api = ApiService.v1Api

    // ========== AUTHENTICATION & REGISTRATION ==========

    suspend fun register(userCreate: UserCreateRequest): Result<UserProfile> = safeApiCall {
        api.v1UsersRegisterCreate(userCreate)
    }

    suspend fun login(loginRequest: LoginRequestRequest): Result<Map<String, Any>> = safeApiCall {
        api.v1UsersLoginCreate(loginRequest)
    }

    suspend fun refreshToken(refreshRequest: TokenRefreshRequestRequest): Result<TokenRefreshResponse>
            = safeApiCall {
        api.v1UsersTokenRefreshCreate(refreshRequest)
    }

    suspend fun logout(logoutRequest: LogoutRequestRequest): Result<LogoutResponse> = safeApiCall {
        api.v1UsersLoginLogoutCreate(logoutRequest)
    }

    suspend fun logoutAll(): Result<LogoutResponse> = safeApiCall {
        api.v1UsersLoginLogoutAllCreate()
    }

    // ========== 2FA ==========

    suspend fun verify2FA(verifyRequest: Verify2FARequestRequest): Result<Verify2FAResponse> =
        safeApiCall {
            api.v1UsersLoginVerify2faCreate(verifyRequest)
        }

    suspend fun resend2FA(resendRequest: Resend2FARequestRequest): Result<Resend2FAResponse> =
        safeApiCall {
            api.v1UsersLoginResend2faCreate(resendRequest)
        }

    suspend fun enable2FA(enableRequest: EnableTwoFactorRequest):
            Result<V1UsersSecurityDisable2faCreate200Response> = safeApiCall {
        api.v1UsersSecurityEnable2faCreate(enableRequest)
    }

    suspend fun disable2FA(disableRequest: DisableTwoFactorRequest):
            Result<V1UsersSecurityDisable2faCreate200Response> = safeApiCall {
        api.v1UsersSecurityDisable2faCreate(disableRequest)
    }

    suspend fun check2FA(): Result<V1UsersSecurityCheck2faRetrieve200Response> = safeApiCall {
        api.v1UsersSecurityCheck2faRetrieve()
    }

    // ========== PROFILE ==========

    suspend fun getCurrentUserProfile(): Result<UserProfile> = safeApiCall {
        api.v1UsersProfileRetrieve()
    }

    suspend fun getUserProfile(userId: Int): Result<UserProfile> = safeApiCall {
        api.v1UsersProfileRetrieve2(userId)
    }

    suspend fun updateProfile(userUpdate: UserProfileSchemaUpdateRequest):
            Result<V1UsersAdminUsersUpdate200Response> = safeApiCall {
        api.v1UsersProfileUpdate(userUpdate)
    }

    suspend fun updateUserStatus(userStatus: UserStatusRequest):
            Result<V1UsersDeactivateCreate200Response> = safeApiCall {
        api.v1UsersStatusUpdateCreate(userStatus)
    }

    suspend fun deactivateAccount(password: String, confirm: Boolean): Result<V1UsersDeactivateCreate200Response> = safeApiCall {
        val body = UserDeactivateInputRequest(password=password, confirm=confirm)
        api.v1UsersDeactivateCreate(body)
    }

    suspend fun verifyAccount(): Result<V1UsersVerifyCreate200Response> = safeApiCall {
        api.v1UsersVerifyCreate()
    }

    // ========== FOLLOWS ==========

    suspend fun followUser(followingId: Int): Result<Any> = safeApiCall {
        api.v1UsersFollowCreate(
            FollowUserRequest(
                followingId = followingId,
            )
        )
    }

    suspend fun unfollowUser(followingId: Int): Result<Any> = safeApiCall {
        api.v1UsersUnfollowCreate(UnfollowUserRequest(followingId = followingId))
    }

    suspend fun checkFollowStatus(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersFollowStatusRetrieve(userId)
    }

    suspend fun getFollowers(
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedFollowerList> = safeApiCall {
        if (userId != null) {
            api.v1UsersFollowersRetrieve2(userId, page, pageSize, userId)
        } else {
            api.v1UsersFollowersRetrieve(page, pageSize, userId)
        }
    }

    suspend fun getFollowing(
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedFollowingList> = safeApiCall {
        if (userId != null) {
            api.v1UsersFollowingRetrieve2(userId, page, pageSize, userId)
        } else {
            api.v1UsersFollowingRetrieve(page, pageSize, userId)
        }
    }

    suspend fun getFollowStats(userId: Int? = null): Result<FollowStats> = safeApiCall {
        if (userId != null) {
            api.v1UsersFollowStatsRetrieve2(userId, userId)
        } else {
            api.v1UsersFollowStatsRetrieve(userId)
        }
    }

    suspend fun getMutualFollows(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersMutualFollowsRetrieve(userId)
    }

    suspend fun getSuggestedUsers(limit: Int? = null): Result<Any> = safeApiCall {
        api.v1UsersSuggestedUsersRetrieve(limit)
    }

    // ========== MEDIA ==========

    suspend fun uploadProfilePicture(upload: MultipartBody.Part): Result<Any> = safeApiCall {
        api.v1UsersMediaProfilePictureCreate(upload)
    }

    suspend fun getProfilePictureUrl(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersMediaProfilePictureRetrieve(userId)
    }

    suspend fun uploadCoverPhoto(upload: MultipartBody.Part): Result<Any> = safeApiCall {
        api.v1UsersMediaCoverPhotoCreate(upload)
    }

    suspend fun getCoverPhotoUrl(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersMediaCoverPhotoRetrieve(userId)
    }

    suspend fun removeProfilePicture(): Result<Any> = safeApiCall {
        api.v1UsersMediaRemoveProfilePictureCreate()
    }

    suspend fun removeCoverPhoto(): Result<Any> = safeApiCall {
        api.v1UsersMediaRemoveCoverPhotoCreate()
    }

    suspend fun validateImage(imageValidationInput: MultipartBody.Part): Result<Any> =
        safeApiCall {
            api.v1UsersMediaValidateImageCreate(imageValidationInput)
        }

    // ========== PASSWORD MANAGEMENT ==========

    suspend fun changePassword(changeRequest: ChangePasswordRequest):
            Result<V1UsersSecurityChangePasswordCreate200Response> = safeApiCall {
        api.v1UsersSecurityChangePasswordCreate(changeRequest)
    }

    suspend fun checkPasswordStrength(checkRequest: PasswordStrengthCheckRequestRequest):
            Result<PasswordStrengthCheckResponse> = safeApiCall {
        api.v1UsersPasswordCheckStrengthCreate(checkRequest)
    }

    suspend fun requestPasswordReset(email: String): Result<PasswordResetRequestResponse> = safeApiCall {
        api.v1UsersPasswordResetCreate(PasswordResetRequestRequest(email))
    }

    suspend fun verifyPasswordReset(verifyRequest: PasswordResetVerifyRequestRequest): Result<PasswordResetVerifyResponse> = safeApiCall {
        api.v1UsersPasswordResetVerifyCreate(verifyRequest)
    }

    suspend fun completePasswordReset(completeRequest: PasswordResetCompleteRequestRequest): Result<PasswordResetCompleteResponse> = safeApiCall {
        api.v1UsersPasswordResetCompleteCreate(completeRequest)
    }

    suspend fun getPasswordHistory(): Result<PasswordHistoryResponse> = safeApiCall {
        api.v1UsersPasswordHistoryRetrieve()
    }

    // ========== SECURITY ==========

    suspend fun getSecuritySettings(): Result<Any> = safeApiCall {
        api.v1UsersSecuritySettingsRetrieve()
    }

    suspend fun updateSecuritySettings(settings: UpdateSecuritySettingsRequest):
            Result<V1UsersSecuritySettingsUpdate200Response> = safeApiCall {
        api.v1UsersSecuritySettingsUpdate(settings)
    }

    suspend fun getSecurityLogs(
        eventType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSecurityLog> = safeApiCall {
        api.v1UsersSecurityLogsRetrieve(eventType, page, pageSize)
    }

    suspend fun getLoginSessions(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedLoginSession> = safeApiCall {
        api.v1UsersSecuritySessionsRetrieve(page, pageSize)
    }

    suspend fun terminateSession(sessionId: UUID): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1UsersSecurityTerminateSessionCreate(TerminateSessionRequest(sessionId))
    }

    suspend fun terminateAllSessions(): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1UsersSecurityTerminateAllSessionsCreate()
    }

    suspend fun bulkTerminateSessions(sessionIds: List<java.util.UUID>, terminateAll: Boolean? = false): Result<V1UsersSecurityBulkTerminateSessionsCreate200Response> = safeApiCall {
        api.v1UsersSecurityBulkTerminateSessionsCreate(BulkTerminateSessionsRequest(sessionIds,
            terminateAll))
    }

    suspend fun getFailedLoginAttempts(): Result<FailedLoginAttemptsResponse> = safeApiCall {
        api.v1UsersSecurityFailedLoginsRetrieve()
    }

    suspend fun getSuspiciousActivities(limit: Int? = null): Result<V1UsersSecuritySuspiciousActivitiesRetrieve200Response> = safeApiCall {
        api.v1UsersSecuritySuspiciousActivitiesRetrieve(limit)
    }

    // ========== USER SEARCH ==========

    suspend fun searchUsers(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSearchResult> = safeApiCall {
        api.v1UsersSearchRetrieve(query, page, pageSize)
    }

    suspend fun advancedSearchUsers(
        createdAfter: String? = null,
        createdBefore: String? = null,
        email: String? = null,
        firstName: String? = null,
        isVerified: Boolean? = null,
        lastName: String? = null,
        orderBy: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        username: String? = null
    ): Result<PaginatedSearchResult> = safeApiCall {
        api.v1UsersSearchAdvancedRetrieve(
            createdAfter = createdAfter,
            createdBefore = createdBefore,
            email = email,
            firstName = firstName,
            isVerified = isVerified,
            lastName = lastName,
            orderBy = orderBy,
            page = page,
            pageSize = pageSize,
            username = username
        )
    }

    suspend fun autocompleteUsers(prefix: String): Result<V1UsersSearchAutocompleteRetrieve200Response> = safeApiCall {
        api.v1UsersSearchAutocompleteRetrieve(prefix)
    }

    suspend fun searchUsersByEmail(email: String): Result<Unit> = safeApiCall {
        api.v1UsersSearchByEmailRetrieve(email)
    }

    suspend fun globalSearch(query: String): Result<Any> = safeApiCall {
        api.v1UsersSearchGlobalRetrieve(query)
    }

    // ========== USER ACTIVITY ==========

    suspend fun getUserActivities(
        action: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserActivity> = safeApiCall {
        api.v1UsersActivityRetrieve(action, page, pageSize)
    }

    suspend fun getFollowingActivities(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserActivity> = safeApiCall {
        api.v1UsersActivityFollowingRetrieve(page, pageSize)
    }

    suspend fun getRecentActivities(
        action: String? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserActivity> = safeApiCall {
        api.v1UsersActivityRecentRetrieve(action, page, pageSize, userId)
    }

    suspend fun getActivitySummary(): Result<ActivitySummary> = safeApiCall {
        api.v1UsersActivitySummaryRetrieve()
    }

    suspend fun logActivity(request: LogActivityInputRequest): Result<UserActivity> = safeApiCall {
        api.v1UsersActivityLogCreate(request)
    }

    // ========== CHECK AVAILABILITY ==========

    suspend fun checkUsername(username: String): Result<V1UsersCheckUsernameRetrieve200Response> = safeApiCall {
        api.v1UsersCheckUsernameRetrieve(username)
    }

    suspend fun checkEmail(email: String): Result<V1UsersCheckEmailRetrieve200Response> = safeApiCall {
        api.v1UsersCheckEmailRetrieve(email)
    }
}
package com.cyberarcenal.huddle.data.repositories.users

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MultipartBody
import java.time.OffsetDateTime

class UsersRepository {
    private val api = ApiService.v1Api

    // ========== AUTHENTICATION & REGISTRATION ==========

    /**
     * Register a new user.
     */
    suspend fun register(userCreate: UserCreate): Result<UserProfile> = safeApiCall {
        api.v1UsersRegisterCreate(userCreate)
    }

    /**
     * Login with email and password.
     */
    suspend fun login(loginRequest: LoginRequest): Result<Map<String, Any>> = safeApiCall {
        api.v1UsersLoginCreate(loginRequest)
    }

    /**
     * Refresh access token.
     */
    suspend fun refreshToken(refreshRequest: TokenRefreshRequest): Result<TokenRefreshResponse> = safeApiCall {
        api.v1UsersTokenRefreshCreate(refreshRequest)
    }

    /**
     * Logout from current session.
     */
    suspend fun logout(logoutRequest: LogoutRequest): Result<LogoutResponse> = safeApiCall {
        api.v1UsersLoginLogoutCreate(logoutRequest)
    }

    /**
     * Logout from all sessions.
     */
    suspend fun logoutAll(): Result<LogoutResponse> = safeApiCall {
        api.v1UsersLoginLogoutAllCreate()
    }

    // ========== 2FA ==========

    /**
     * Verify 2FA OTP during login.
     */
    suspend fun verify2FA(verifyRequest: Verify2FARequest): Result<Verify2FAResponse> = safeApiCall {
        api.v1UsersLoginVerify2faCreate(verifyRequest)
    }

    /**
     * Resend 2FA OTP.
     */
    suspend fun resend2FA(resendRequest: Resend2FARequest): Result<Resend2FAResponse> = safeApiCall {
        api.v1UsersLoginResend2faCreate(resendRequest)
    }

    /**
     * Enable 2FA for current user.
     */
    suspend fun enable2FA(enableRequest: EnableTwoFactor): Result<V1UsersSecurityDisable2faCreate200Response> = safeApiCall {
        api.v1UsersSecurityEnable2faCreate(enableRequest)
    }

    /**
     * Disable 2FA for current user.
     */
    suspend fun disable2FA(disableRequest: DisableTwoFactor): Result<V1UsersSecurityDisable2faCreate200Response> = safeApiCall {
        api.v1UsersSecurityDisable2faCreate(disableRequest)
    }

    /**
     * Check if 2FA is enabled.
     */
    suspend fun check2FA(): Result<V1UsersSecurityCheck2faRetrieve200Response> = safeApiCall {
        api.v1UsersSecurityCheck2faRetrieve()
    }

    // ========== PROFILE ==========

    /**
     * Get current user's profile.
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile> = safeApiCall {
        api.v1UsersProfileRetrieve()
    }

    /**
     * Get user profile by ID.
     */
    suspend fun getUserProfile(userId: Int): Result<UserProfile> = safeApiCall {
        api.v1UsersProfileRetrieve2(userId)
    }

    /**
     * Update current user's profile.
     */
    suspend fun updateProfile(userUpdate: UserUpdate): Result<V1UsersAdminUsersUpdate200Response> = safeApiCall {
        api.v1UsersProfileUpdate(userUpdate)
    }

    /**
     * Update user status (admin or self for delete).
     */
    suspend fun updateUserStatus(userStatus: UserStatus): Result<V1UsersDeactivateCreate200Response> = safeApiCall {
        api.v1UsersStatusUpdateCreate(userStatus)
    }

    /**
     * Deactivate current user's account.
     */
    suspend fun deactivateAccount(password: String, confirm: Boolean): Result<V1UsersDeactivateCreate200Response> = safeApiCall {
        // The API expects a body with password and confirm fields.
        val body = UserDeactivateInput(password=password, confirm=confirm)
        api.v1UsersDeactivateCreate(body)
    }

    /**
     * Verify current user's account (after email confirmation).
     */
    suspend fun verifyAccount(): Result<V1UsersVerifyCreate200Response> = safeApiCall {
        api.v1UsersVerifyCreate()
    }

    // ========== FOLLOWS ==========

    /**
     * Follow a user.
     */
    suspend fun followUser(followingId: Int): Result<Any> = safeApiCall {
        api.v1UsersFollowCreate(
            FollowUser(
                id = 0, // dummy, server will ignore
                followingId = followingId,
                followingUsername = null, // optional, server may ignore
                createdAt = OffsetDateTime.now() // dummy, server will ignore
            )
        )
    }

    /**
     * Unfollow a user.
     */
    suspend fun unfollowUser(followingId: Int): Result<Any> = safeApiCall {
        api.v1UsersUnfollowCreate(UnfollowUser(followingId = followingId))
    }

    /**
     * Check if current user is following another user.
     */
    suspend fun checkFollowStatus(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersFollowStatusRetrieve(userId)
    }

    /**
     * Get followers of a user.
     */
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

    /**
     * Get users followed by a user.
     */
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

    /**
     * Get follow statistics (counts).
     */
    suspend fun getFollowStats(userId: Int? = null): Result<FollowStats> = safeApiCall {
        if (userId != null) {
            api.v1UsersFollowStatsRetrieve2(userId, userId)
        } else {
            api.v1UsersFollowStatsRetrieve(userId)
        }
    }

    /**
     * Get mutual followers between current user and another user.
     */
    suspend fun getMutualFollows(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersMutualFollowsRetrieve(userId)
    }

    /**
     * Get suggested users to follow.
     */
    suspend fun getSuggestedUsers(limit: Int? = null): Result<Any> = safeApiCall {
        api.v1UsersSuggestedUsersRetrieve(limit)
    }

    // ========== MEDIA ==========

    /**
     * Upload profile picture.
     * Note: This is a multipart request. The generated API uses @Body ProfilePictureUpload,
     * but that might not be correct for file upload. Check the actual implementation.
     * We'll assume the generated method works, but if not, we may need to adjust.
     */
    suspend fun uploadProfilePicture(upload: ProfilePictureUpload): Result<Any> = safeApiCall {
        api.v1UsersMediaProfilePictureCreate(upload)
    }

    /**
     * Get profile picture URL for a user.
     */
    suspend fun getProfilePictureUrl(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersMediaProfilePictureRetrieve(userId)
    }

    /**
     * Upload cover photo.
     */
    suspend fun uploadCoverPhoto(upload: CoverPhotoUpload): Result<Any> = safeApiCall {
        api.v1UsersMediaCoverPhotoCreate(upload)
    }

    /**
     * Get cover photo URL for a user.
     */
    suspend fun getCoverPhotoUrl(userId: Int): Result<Any> = safeApiCall {
        api.v1UsersMediaCoverPhotoRetrieve(userId)
    }

    /**
     * Remove profile picture.
     */
    suspend fun removeProfilePicture(): Result<Any> = safeApiCall {
        api.v1UsersMediaRemoveProfilePictureCreate()
    }

    /**
     * Remove cover photo.
     */
    suspend fun removeCoverPhoto(): Result<Any> = safeApiCall {
        api.v1UsersMediaRemoveCoverPhotoCreate()
    }

    /**
     * Validate an image before upload.
     */
    suspend fun validateImage(imageValidationInput: ImageValidationInput): Result<Any> = safeApiCall {
        // This might need multipart. The generated method has no parameters, so we'll leave as is.
        api.v1UsersMediaValidateImageCreate(imageValidationInput)
    }

    // ========== PASSWORD MANAGEMENT ==========

    /**
     * Change password.
     */
    suspend fun changePassword(changeRequest: ChangePassword): Result<V1UsersSecurityChangePasswordCreate200Response> = safeApiCall {
        api.v1UsersSecurityChangePasswordCreate(changeRequest)
    }

    /**
     * Check password strength.
     */
    suspend fun checkPasswordStrength(checkRequest: PasswordStrengthCheckRequest): Result<PasswordStrengthCheckResponse> = safeApiCall {
        api.v1UsersPasswordCheckStrengthCreate(checkRequest)
    }

    /**
     * Request password reset (send OTP).
     */
    suspend fun requestPasswordReset(email: String): Result<PasswordResetRequestResponse> = safeApiCall {
        api.v1UsersPasswordResetCreate(PasswordResetRequest(email))
    }

    /**
     * Verify password reset OTP and get checkpoint token.
     */
    suspend fun verifyPasswordReset(verifyRequest: PasswordResetVerifyRequest): Result<PasswordResetVerifyResponse> = safeApiCall {
        api.v1UsersPasswordResetVerifyCreate(verifyRequest)
    }

    /**
     * Complete password reset with new password.
     */
    suspend fun completePasswordReset(completeRequest: PasswordResetCompleteRequest): Result<PasswordResetCompleteResponse> = safeApiCall {
        api.v1UsersPasswordResetCompleteCreate(completeRequest)
    }

    /**
     * Get password change history.
     */
    suspend fun getPasswordHistory(): Result<PasswordHistoryResponse> = safeApiCall {
        api.v1UsersPasswordHistoryRetrieve()
    }

    // ========== SECURITY ==========

    /**
     * Get security settings.
     */
    suspend fun getSecuritySettings(): Result<Any> = safeApiCall {
        api.v1UsersSecuritySettingsRetrieve()
    }

    /**
     * Update security settings.
     */
    suspend fun updateSecuritySettings(settings: UpdateSecuritySettings): Result<V1UsersSecuritySettingsUpdate200Response> = safeApiCall {
        api.v1UsersSecuritySettingsUpdate(settings)
    }

    /**
     * Get security logs.
     */
    suspend fun getSecurityLogs(
        eventType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSecurityLog> = safeApiCall {
        api.v1UsersSecurityLogsRetrieve(eventType, page, pageSize)
    }

    /**
     * Get active login sessions.
     */
    suspend fun getLoginSessions(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedLoginSession> = safeApiCall {
        api.v1UsersSecuritySessionsRetrieve(page, pageSize)
    }

    /**
     * Terminate a specific session.
     */
    suspend fun terminateSession(sessionId: java.util.UUID): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1UsersSecurityTerminateSessionCreate(TerminateSession(sessionId))
    }

    /**
     * Terminate all sessions except current.
     */
    suspend fun terminateAllSessions(): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1UsersSecurityTerminateAllSessionsCreate()
    }

    /**
     * Terminate multiple sessions at once.
     */
    suspend fun bulkTerminateSessions(sessionIds: List<java.util.UUID>, terminateAll: Boolean? = false): Result<V1UsersSecurityBulkTerminateSessionsCreate200Response> = safeApiCall {
        api.v1UsersSecurityBulkTerminateSessionsCreate(BulkTerminateSessions(sessionIds, terminateAll))
    }

    /**
     * Get failed login attempts.
     */
    suspend fun getFailedLoginAttempts(): Result<FailedLoginAttemptsResponse> = safeApiCall {
        api.v1UsersSecurityFailedLoginsRetrieve()
    }

    /**
     * Get suspicious activities.
     */
    suspend fun getSuspiciousActivities(limit: Int? = null): Result<V1UsersSecuritySuspiciousActivitiesRetrieve200Response> = safeApiCall {
        api.v1UsersSecuritySuspiciousActivitiesRetrieve(limit)
    }

    // ========== USER SEARCH ==========

    /**
     * Basic user search.
     */
    suspend fun searchUsers(
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSearchResult> = safeApiCall {
        api.v1UsersSearchRetrieve(query, page, pageSize)
    }

    /**
     * Advanced user search with filters.
     */
    suspend fun advancedSearchUsers(
        query: String,
        isVerified: Boolean? = null,
        joinedAfter: String? = null,
        joinedBefore: String? = null,
        minFollowers: Int? = null,
        maxFollowers: Int? = null,
        status: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedSearchResult> = safeApiCall {
        api.v1UsersSearchAdvancedRetrieve(
            q = query,
            isVerified = isVerified,
            joinedAfter = joinedAfter,
            joinedBefore = joinedBefore,
            minFollowers = minFollowers,
            maxFollowers = maxFollowers,
            status = status,
            page = page,
            pageSize = pageSize
        )
    }

    /**
     * Autocomplete username/full name.
     */
    suspend fun autocompleteUsers(prefix: String): Result<V1UsersSearchAutocompleteRetrieve200Response> = safeApiCall {
        api.v1UsersSearchAutocompleteRetrieve(prefix)
    }

    /**
     * Search users by email (admin only).
     */
    suspend fun searchUsersByEmail(email: String): Result<Unit> = safeApiCall {
        api.v1UsersSearchByEmailRetrieve(email)
    }

    /**
     * Global search (users, posts, groups).
     */
    suspend fun globalSearch(query: String): Result<Any> = safeApiCall {
        api.v1UsersSearchGlobalRetrieve(query)
    }

    // ========== USER ACTIVITY ==========

    /**
     * Get current user's activity log.
     */
    suspend fun getUserActivities(
        action: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserActivity> = safeApiCall {
        api.v1UsersActivityRetrieve(action, page, pageSize)
    }

    /**
     * Get activities from followed users.
     */
    suspend fun getFollowingActivities(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserActivity> = safeApiCall {
        api.v1UsersActivityFollowingRetrieve(page, pageSize)
    }

    /**
     * Get recent activities (public or from followed users).
     */
    suspend fun getRecentActivities(
        action: String? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedUserActivity> = safeApiCall {
        api.v1UsersActivityRecentRetrieve(action, page, pageSize, userId)
    }

    /**
     * Get activity summary.
     */
    suspend fun getActivitySummary(): Result<ActivitySummary> = safeApiCall {
        api.v1UsersActivitySummaryRetrieve()
    }

    /**
     * Log an activity (internal use).
     */
    suspend fun logActivity(request: LogActivityInput): Result<UserActivity> = safeApiCall {
        api.v1UsersActivityLogCreate(request)
    }

    // ========== CHECK AVAILABILITY ==========

    /**
     * Check if username is available.
     */
    suspend fun checkUsername(username: String): Result<V1UsersCheckUsernameRetrieve200Response> = safeApiCall {
        api.v1UsersCheckUsernameRetrieve(username)
    }

    /**
     * Check if email is available.
     */
    suspend fun checkEmail(email: String): Result<V1UsersCheckEmailRetrieve200Response> = safeApiCall {
        api.v1UsersCheckEmailRetrieve(email)
    }
}
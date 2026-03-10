package com.cyberarcenal.huddle.data.repositories.users

import com.cyberarcenal.huddle.api.models.FollowUser
import com.cyberarcenal.huddle.api.models.PaginatedPostFeed
import com.cyberarcenal.huddle.api.models.UnfollowUser
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.network.ApiService
import retrofit2.Response
import java.time.OffsetDateTime

class ProfileRepository {
    private val api = ApiService.v1Api

    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return safeApiCall { api.v1UsersProfileRetrieve() }
    }

    suspend fun getUserProfile(userId: Int): Result<UserProfile> {
        return safeApiCall { api.v1UsersProfileRetrieve2(userId) }
    }

    suspend fun getUserPosts(userId: Int, page: Int, pageSize: Int): Result<PaginatedPostFeed> {
        return safeApiCall { api.v1FeedPostsRetrieve(userId = userId, page = page, pageSize = pageSize) }
    }

    suspend fun followUser(userId: Int): Result<Any> {
        return safeApiCall { api.v1UsersFollowCreate(FollowUser(
            followingId = userId,
            id = 0,
            followingUsername = "",
            createdAt = OffsetDateTime.now(),
        )) }
    }

    suspend fun unfollowUser(userId: Int): Result<Any> {
        return safeApiCall { api.v1UsersUnfollowCreate(UnfollowUser(followingId = userId)) }
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
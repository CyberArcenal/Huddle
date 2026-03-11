package com.cyberarcenal.huddle.data.repositories.stories

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

class StoriesRepository {
    private val api = ApiService.v1Api
    // Gagamit ng exposed retrofit instance mula sa ApiService
    private val multipartApi = ApiService.retrofit.create(StoriesMultipartApi::class.java)

    // ========== STORIES ==========

    suspend fun getStories(page: Int? = null, pageSize: Int? = null): Result<PaginatedStory> = safeApiCall {
        api.v1StoriesStoriesRetrieve(page, pageSize)
    }

    suspend fun createStory(storyCreate: StoryCreate): Result<Story> = safeApiCall {
        api.v1StoriesStoriesCreate(storyCreate)
    }

    suspend fun getStory(storyId: Int): Result<Story> = safeApiCall {
        api.v1StoriesStoriesRetrieve2(storyId)
    }

    suspend fun updateStory(storyId: Int, storyUpdate: StoryUpdate): Result<Story> = safeApiCall {
        api.v1StoriesStoriesUpdate(storyId, storyUpdate)
    }

    suspend fun deleteStory(storyId: Int): Result<V1StoriesStoriesDestroy200Response> = safeApiCall {
        api.v1StoriesStoriesDestroy(storyId)
    }

    suspend fun deactivateStory(storyId: Int): Result<V1StoriesStoriesDestroy200Response> = safeApiCall {
        api.v1StoriesStoriesDeactivateCreate(storyId)
    }

    suspend fun extendStory(storyId: Int, additionalHours: Int): Result<V1StoriesStoriesDestroy200Response> {
        val body = ExtendStoryInput(additionalHours = additionalHours)
        return safeApiCall { api.v1StoriesStoriesExtendCreate(storyId, body) }
    }

    // ========== STORY FEEDS ==========

    suspend fun getStoryFeed(includeOwn: Boolean? = null, limitPerUser: Int? = null, maxUsers: Int? = null): Result<List<StoryFeed>> = safeApiCall {
        api.v1StoriesStoriesFeedList(includeOwn, limitPerUser, maxUsers)
    }

    suspend fun getFollowingStories(limit: Int? = null): Result<List<StoryFeed>> = safeApiCall {
        api.v1StoriesStoriesFollowingList(limit)
    }

    suspend fun getUserStories(userId: Int, includeExpired: Boolean? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedStory> = safeApiCall {
        api.v1StoriesUsersStoriesRetrieve(userId, includeExpired, page, pageSize)
    }

    suspend fun getMyStories(includeExpired: Boolean? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedStory> = safeApiCall {
        api.v1StoriesMeStoriesRetrieve(includeExpired, page, pageSize)
    }

    // ========== STORY INTERACTIONS ==========

    suspend fun markStoryViewed(storyId: Int): Result<StoryView> {
        val create = StoryViewCreate(storyId = storyId)
        return safeApiCall { api.v1StoriesStoriesViewCreate(storyId, create) }
    }

    suspend fun getStoryViewCount(storyId: Int): Result<StoryViewCount> = safeApiCall {
        api.v1StoriesStoriesViewCountRetrieve(storyId)
    }

    suspend fun getStoryViewers(storyId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedStoryView> = safeApiCall {
        api.v1StoriesStoriesViewsRetrieve(storyId, page, pageSize)
    }

    suspend fun getRecentViewers(storyId: Int, hours: Int? = null, limit: Int? = null): Result<List<StoryRecentViewer>> = safeApiCall {
        api.v1StoriesStoriesRecentViewersList(storyId, hours, limit)
    }

    // ========== STORY STATISTICS & HIGHLIGHTS ==========

    suspend fun getMyStoryStats(): Result<StoryStats> = safeApiCall {
        api.v1StoriesStoriesStatsRetrieve()
    }

    suspend fun getMyViewingStats(): Result<Any> = safeApiCall {
        api.v1StoriesStoriesViewStatsRetrieve()
    }

    suspend fun getStoryHighlights(days: Int? = null, limit: Int? = null): Result<List<StoryHighlight>> = safeApiCall {
        api.v1StoriesStoriesHighlightsList(days, limit)
    }

    suspend fun getPopularStories(hours: Int? = null, limit: Int? = null): Result<List<Any>> = safeApiCall {
        api.v1StoriesStoriesPopularRetrieve(hours, limit)
    }

    suspend fun getStoryRecommendations(limit: Int? = null): Result<List<StoryRecommendation>> = safeApiCall {
        api.v1StoriesStoriesRecommendationsList(limit)
    }

    suspend fun getMutualViews(otherUserId: Int): Result<Any> = safeApiCall {
        api.v1StoriesUsersMutualViewsRetrieve(otherUserId)
    }

    // ========== ADMIN ==========

    suspend fun adminCleanupStories(deactivateOnly: Boolean? = null): Result<StoryCleanupResponse> {
        val body = CleanupStoriesInput(deactivateOnly = deactivateOnly)
        return safeApiCall { api.v1StoriesAdminStoriesCleanupCreate(body) }
    }

    // ========== UPLOAD LOGIC ==========

    /**
     * Create a story with media (image/video) using multipart upload.
     */
    suspend fun createStoryWithMedia(
        storyType: StoryTypeEnum,
        content: String?,
        mediaFile: File,
        mimeType: String,
        expiresInHours: Int? = 24
    ): Result<Story> = safeApiCall {
        val storyTypeBody = storyType.value.toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = content?.toRequestBody("text/plain".toMediaTypeOrNull())
        val expiresBody = expiresInHours?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        // File part name is "media_file" based on backend requirement
        val requestFile = mediaFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val mediaPart = MultipartBody.Part.createFormData(
            name = "media_file",
            filename = mediaFile.name,
            body = requestFile
        )

        multipartApi.createStory(
            storyType = storyTypeBody,
            content = contentBody,
            expiresInHours = expiresBody,
            media = mediaPart
        )
    }

    suspend fun createTextStory(
        content: String,
        expiresInHours: Int? = 24
    ): Result<Story> {
        val storyCreate = StoryCreate(
            storyType = StoryTypeEnum.TEXT,
            content = content,
            mediaFile = null, // Inayos base sa iyong instruction
            expiresInHours = expiresInHours
        )
        return safeApiCall { api.v1StoriesStoriesCreate(storyCreate) }
    }
}

interface StoriesMultipartApi {
    @Multipart
    @POST("api/v1/stories/stories/")
    suspend fun createStory(
        @Part("story_type") storyType: RequestBody,
        @Part("content") content: RequestBody?,
        @Part("expires_in_hours") expiresInHours: RequestBody?,
        @Part media: MultipartBody.Part
    ): Response<Story> // Ibalik sa Response<Story> para gamitin ang configured Retrofit converter
}

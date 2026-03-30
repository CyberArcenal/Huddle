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

/**
 * Custom request for creating stories that includes local file info.
 */
data class StoryCreateRequestWithMedia(
    val storyType: StoryTypeEnum,
    val content: String? = null,
    val mediaFile: File? = null,
    val expiresInHours: Int = 24,
    val mimeType: String? = null
)

interface StoryCreateApi {
    @Multipart
    @POST("api/v1/stories/stories/")
    suspend fun storiesCreate(
        @Part("story_type") storyType: RequestBody,
        @Part("content") content: RequestBody? = null,
        @Part mediaFile: MultipartBody.Part? = null,
        @Part("mimeTypes") mimeTypes: RequestBody? = null,
        @Part("expires_in_hours") expiresInHours: RequestBody? = null
    ): Response<Story>
}

class StoriesRepository {
    private val api = ApiService.storiesApi
    private val createStoryApi = ApiService.storyCreateApi

    suspend fun createStory(request: StoryCreateRequestWithMedia): Result<Story> = safeApiCall {
        val mediaPart = request.mediaFile?.asRequestBody((request.mimeType ?: "image/*")
            .toMediaTypeOrNull())?.let {
            MultipartBody.Part.createFormData(
                "media_file",
                request.mediaFile.name,
                it
            )
        }

        createStoryApi.storiesCreate(
            storyType = request.storyType.value.toRequestBody("text/plain".toMediaTypeOrNull()),
            content = request.content?.toRequestBody("text/plain".toMediaTypeOrNull()),
            mediaFile = mediaPart,
            mimeTypes = request.mimeType?.toRequestBody("text/plain".toMediaTypeOrNull()),
            expiresInHours = request.expiresInHours.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    suspend fun getStoryLists(page: Int?, pageSize: Int?): Result<PaginatedStory> = safeApiCall {
        api.apiV1StoriesStoriesRetrieve(page, pageSize)
    }

    suspend fun getStoryFeed(includeOwn: Boolean? = null): Result<List<StoryFeed>> =
        safeApiCall { api.apiV1StoriesStoriesFeedList(includeOwn) }

    suspend fun getMyStories(
        page: Int? = null,
        pageSize: Int? = null,
        includeExpired: Boolean? = null
    ) =
        safeApiCall { api.apiV1StoriesMeStoriesRetrieve(includeExpired, page, pageSize) }

    suspend fun getUserStories(
        userId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        includeExpired: Boolean? = null
    ) =
        safeApiCall { api.apiV1StoriesUsersStoriesRetrieve(userId, includeExpired, page, pageSize) }

    suspend fun getPopularStories(hours: Int? = null, limit: Int? = null) =
        safeApiCall { api.apiV1StoriesStoriesPopularList(hours, limit) }

    suspend fun getStoryRecommendations(limit: Int? = null) =
        safeApiCall { api.apiV1StoriesStoriesRecommendationsList(limit) }

    suspend fun getStoryStats() =
        safeApiCall { api.apiV1StoriesStoriesStatsRetrieve() }

    // Deactivate a story (soft delete)
    suspend fun deactivateStory(storyId: Int): Result<ApiV1StoriesStoriesDestroy200Response> =
        safeApiCall { api.apiV1StoriesStoriesDeactivateCreate(storyId) }

    // Permanently delete a story
    suspend fun deleteStoryPermanent(storyId: Int): Result<ApiV1StoriesStoriesDestroy200Response> =
        safeApiCall { api.apiV1StoriesStoriesDestroy(storyId) }

    // Extend story life by given hours
    suspend fun extendStory(
        storyId: Int,
        hours: Int? = null
    ): Result<ApiV1StoriesStoriesDestroy200Response> =
        safeApiCall {
            api.apiV1StoriesStoriesExtendCreate(
                storyId,
                ExtendStoryInputRequest(hours)
            )
        }

    // Get stories from followed users
    suspend fun getFollowingStories(limit: Int? = null): Result<List<StoryFeed>> =
        safeApiCall { api.apiV1StoriesStoriesFollowingList(limit) }

    // Get highlighted stories (most viewed)
    suspend fun getStoryHighlights(
        days: Int? = null,
        limit: Int? = null
    ): Result<List<StoryHighlight>> =
        safeApiCall { api.apiV1StoriesStoriesHighlightsList(days, limit) }

    // Get a single story by ID
    suspend fun getStory(storyId: Int): Result<Story> =
        safeApiCall { api.apiV1StoriesStoriesRetrieve2(storyId) }

    // Get stories filtered by type (image, video, text)
    suspend fun getStoriesByType(
        storyType: String,
        activeOnly: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedStory> =
        safeApiCall { api.apiV1StoriesStoriesTypeRetrieve(storyType, activeOnly, page, pageSize) }

    // Update a story (e.g., change caption)
    suspend fun updateStory(storyId: Int, request: StoryUpdateRequest? = null): Result<Story> =
        safeApiCall { api.apiV1StoriesStoriesUpdate(storyId, request) }

    // Get view count and unique viewers for a story
    suspend fun getStoryViewCount(storyId: Int): Result<StoryViewCount> =
        safeApiCall { api.apiV1StoriesStoriesViewCountRetrieve(storyId) }

    suspend fun getHighlights(): Result<List<StoryHighlight>> =
        safeApiCall { api.apiV1StoriesHighlightsList() }

    suspend fun createHighlight(request: StoryHighlightCreateRequest): Result<StoryHighlight> =
        safeApiCall {
            api.apiV1StoriesHighlightsCreate(request)
        }

    suspend fun updateHighlight(
        id: Int,
        request: StoryHighlightUpdateRequest
    ): Result<StoryHighlight> = safeApiCall {
        api.apiV1StoriesHighlightsUpdate(id, request)
    }

    suspend fun deleteHighlight(highlightId: Int): Result<Unit> =
        safeApiCall { api.apiV1StoriesHighlightsDestroy(highlightId) }

    suspend fun addStoriesToHighlight(
        highlightId: Int,
        body: StoryHighlightAddStoriesRequest
    ): Result<StoryHighlight> = safeApiCall {
        api.apiV1StoriesHighlightsAddStoriesCreate(highlightId, body)
    }

    suspend fun removeStoriesFromHighlight(
        highlightId: Int,
        body: StoryHighlightRemoveStoriesRequest
    ): Result<StoryHighlight> = safeApiCall {
        api.apiV1StoriesHighlightsRemoveStoriesCreate(highlightId, body)
    }

    suspend fun setHighlightCover(
        highlightId: Int,
        storyHighlightSetCoverRequest: StoryHighlightSetCoverRequest
    ): Result<StoryHighlight> = safeApiCall {
        api.apiV1StoriesHighlightsSetCoverCreate(highlightId, storyHighlightSetCoverRequest)
    }

    /**
     * Get a story highlight by its ID.
     */
    suspend fun getHighlight(highlightId: Int): Result<StoryHighlight> =
        safeApiCall { api.apiV1StoriesHighlightsRetrieve(highlightId) }
}

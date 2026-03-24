package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.ApiV1StoriesStoriesDestroy200Response
import com.cyberarcenal.huddle.api.models.ExtendStoryInputRequest
import com.cyberarcenal.huddle.api.models.MutualStoryViewsResponse
import com.cyberarcenal.huddle.api.models.PaginatedStory
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryCreateRequest
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.api.models.StoryHighlight
import com.cyberarcenal.huddle.api.models.StoryHighlightAddStoriesRequest
import com.cyberarcenal.huddle.api.models.StoryHighlightCreateRequest
import com.cyberarcenal.huddle.api.models.StoryHighlightRemoveStoriesRequest
import com.cyberarcenal.huddle.api.models.StoryHighlightSetCoverRequest
import com.cyberarcenal.huddle.api.models.StoryHighlightUpdateRequest
import com.cyberarcenal.huddle.api.models.StoryRecentViewer
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.api.models.StoryUpdateRequest
import com.cyberarcenal.huddle.api.models.StoryViewCount
import com.cyberarcenal.huddle.api.models.StoryViewCreateRequest
import com.cyberarcenal.huddle.api.models.StoryViewStatsResponse
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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

class StoriesRepository {
    private val api = ApiService.storiesApi
    private val createStoryApi = ApiService.storyCreateApi

    suspend fun createStory(request: StoryCreateRequestWithMedia): Result<Story> = safeApiCall {
        val response = if (request.mediaFile == null) {
            // Text-only story
            val apiRequest = StoryCreateRequest(
                storyType = request.storyType,
                content = request.content,
                expiresInHours = request.expiresInHours
            )
            api.apiV1StoriesStoriesCreate(apiRequest)
        } else {
            // Multipart story with media
            val storyTypeBody =
                request.storyType.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = request.content?.toRequestBody("text/plain".toMediaTypeOrNull())
            val expiresBody =
                request.expiresInHours.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val mediaPart = MultipartBody.Part.createFormData(
                "media_file",
                request.mediaFile.name,
                request.mediaFile.asRequestBody("image/*".toMediaTypeOrNull()) // Adjust if video
            )

            createStoryApi.createStoryMultipart(storyTypeBody, contentBody, mediaPart, expiresBody)
        }

        response
    }

    suspend fun getStoryLists(page: Int?, pageSize: Int?): Result<PaginatedStory> = safeApiCall {
        api.apiV1StoriesStoriesRetrieve(page, pageSize)
    }

    suspend fun getStoryFeed(includeOwn: Boolean? = null): Result<List<com.cyberarcenal.huddle.api.models.StoryFeed>> =
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

    suspend fun viewStory(request: StoryViewCreateRequest) =
        safeApiCall { api.apiV1StoriesStoriesViewCreate(request) }

    suspend fun getStoryViewers(storyId: Int, page: Int? = null, pageSize: Int? = null) =
        safeApiCall { api.apiV1StoriesStoriesViewsRetrieve(storyId, page, pageSize) }


    // StoriesRepository.kt - add these methods

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

    // Get recent viewers of a story (owner only)
    suspend fun getRecentViewers(
        storyId: Int,
        hours: Int? = null,
        limit: Int? = null
    ): Result<List<StoryRecentViewer>> =
        safeApiCall { api.apiV1StoriesStoriesRecentViewersList(storyId, hours, limit) }

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

    // Get statistics about the current user's story viewing habits
    suspend fun getStoryViewStats(): Result<StoryViewStatsResponse> =
        safeApiCall { api.apiV1StoriesStoriesViewStatsRetrieve() }

    // Get mutual story viewing data between current user and another user
    suspend fun getMutualStoryViews(otherUserId: Int): Result<MutualStoryViewsResponse> =
        safeApiCall { api.apiV1StoriesUsersMutualViewsRetrieve(otherUserId) }

    suspend fun createHighlights(request: StoryHighlightCreateRequest): Result<StoryHighlight> =
        safeApiCall {
            api.apiV1StoriesHighlightsCreate(request)
        }

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
}

// Custom API interface for Stories Multipart
interface StoryCreateApi {
    @Multipart
    @POST("api/v1/stories/stories/")
    suspend fun createStoryMultipart(
        @Part("story_type") storyType: okhttp3.RequestBody,
        @Part("content") content: okhttp3.RequestBody?,
        @Part mediaFile: MultipartBody.Part,
        @Part("expires_in_hours") expiresInHours: okhttp3.RequestBody
    ): Response<Story>
}

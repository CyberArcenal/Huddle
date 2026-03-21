package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.PaginatedStory
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryCreateRequest
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.api.models.StoryViewCreateRequest
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
            val storyTypeBody = request.storyType.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = request.content?.toRequestBody("text/plain".toMediaTypeOrNull())
            val expiresBody = request.expiresInHours.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            
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

    suspend fun getMyStories(page: Int? = null, pageSize: Int? = null, includeExpired: Boolean? = null) =
        safeApiCall { api.apiV1StoriesMeStoriesRetrieve(includeExpired, page, pageSize) }

    suspend fun getUserStories(userId: Int, page: Int? = null, pageSize: Int? = null, includeExpired: Boolean? = null) =
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

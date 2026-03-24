package com.cyberarcenal.huddle.network

import android.content.Context
import com.cyberarcenal.huddle.api.apis.*
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import com.cyberarcenal.huddle.api.models.TokenRefreshRequestRequest
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.os.Build
import com.cyberarcenal.huddle.api.models.UserImageDisplay
import com.cyberarcenal.huddle.data.repositories.ChatUploadApi
import com.cyberarcenal.huddle.data.repositories.StoryCreateApi
import com.cyberarcenal.huddle.data.repositories.UserCreatePostApi
import kotlinx.coroutines.runBlocking
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

object ApiService {
    private lateinit var appContext: Context

    // Tawagin ito sa iyong MainActivity o Application class
    fun init(context: Context) {
        appContext = context.applicationContext
    }


    private val BASE_URL: String by lazy {
        if (isEmulator()) {
            "http://10.0.2.2:8000/"
        } else {
            "http://127.0.0.1:8000/"
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }
    // 1. Client para sa Refresh Call lang (DAPAT walang authenticator para iwas infinite loop)
    private val refreshHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val refreshRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(refreshHttpClient)
        .addConverterFactory(GsonConverterFactory.create(Serializer.gson))
        .build()

    val tokenRefresh: TokenApi by lazy { refreshRetrofit.create(TokenApi::class.java) }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val token = TokenManager.accessToken
            val requestBuilder = original.newBuilder()

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
        .authenticator(object : Authenticator
        {
            override fun authenticate(route: Route?, response: Response): Request? {
                // 401 error caught here
                return runBlocking {
                    // 2. Kunin ang refresh token
                    val refreshToken = AuthManager.getRefreshToken(appContext) ?: return@runBlocking null

                    try {
                        // 3. IMPORTANT: Gamitin ang 'tokenRefresh' (yung walang authenticator client)
                        // At dahil Response wrapper ang gamit mo, kailangan ng .body()
                        val result = tokenRefresh.refreshCreate(
                            TokenRefreshRequestRequest(refreshToken)
                        )

                        if (result.isSuccessful) {
                            val body = result.body()
                            val newAccessToken = body?.access
                            val newRefreshToken = body?.refresh ?: refreshToken

                            if (!newAccessToken.isNullOrBlank()) {
                                // 4. I-save ang bagong tokens
                                AuthManager.saveTokens(
                                    appContext,
                                    newAccessToken,
                                    newRefreshToken
                                )

                                // 5. I-retry ang original request gamit ang bagong token
                                return@runBlocking response.request.newBuilder()
                                    .header("Authorization", "Bearer $newAccessToken")
                                    .build()
                            }
                        }

                        // Kung hindi successful ang response o null ang body
                        AuthManager.clearTokens(appContext)
                        null
                    } catch (e: Exception) {
                        // Refresh failed (expired refresh token), logout user
                        AuthManager.clearTokens(appContext)
                        null
                    }
                }
            }
        })
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Serializer.gson))
            .build()
    }

    // All generated API interfaces
    val feedApi: FeedApi by lazy { retrofit.create(FeedApi::class.java) }
    val adminLogApi: AdminLogApi by lazy { retrofit.create(AdminLogApi::class.java) }
    val adminViewsApi: AdminApi by lazy { retrofit.create(AdminApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }
    val chatUploadApi: ChatUploadApi by lazy { retrofit.create(ChatUploadApi::class.java) }
    val commentsApi: CommentsApi by lazy { retrofit.create(CommentsApi::class.java) }
    val conversationApi: ConversationApi by lazy { retrofit.create(ConversationApi::class.java) }
    val eventAnalyticsApi: EventAnalyticsApi by lazy { retrofit.create(EventAnalyticsApi::class.java) }
    val eventApi: EventApi by lazy { retrofit.create(EventApi::class.java) }
    val eventAttendanceApi: EventAttendanceApi by lazy { retrofit.create(EventAttendanceApi::class.java) }
    val followViewsApi: FollowApi by lazy { retrofit.create(FollowApi::class.java) }
    val searchsApi: SearchsApi by lazy { retrofit.create(SearchsApi::class.java) }
    val searchsHistoryApi: SearchsHistoryApi by lazy { retrofit.create(SearchsHistoryApi::class.java) }
    val groupSuggestionApi: GroupSuggestionApi by lazy { retrofit.create(GroupSuggestionApi::class.java) }
    val groupViewsApi: GroupApi by lazy { retrofit.create(GroupApi::class.java) }
    val loginApi: LoginApi by lazy { retrofit.create(LoginApi::class.java) }
    val loginCheckpointsApi: LoginCheckpointsApi by lazy { retrofit.create(LoginCheckpointsApi::class.java) }
    val logOutApi: LogOutApi by lazy { retrofit.create(LogOutApi::class.java) }
    val notificationsApi: NotificationsApi by lazy { retrofit.create(NotificationsApi::class.java) }
    val otpRequestsApi: OTPRequestsApi by lazy { retrofit.create(OTPRequestsApi::class.java) }
    val passwordRecoveryApi: PasswordRecoveryApi by lazy { retrofit.create(PasswordRecoveryApi::class.java) }
    val passwordResetApi: PasswordResetApi by lazy { retrofit.create(PasswordResetApi::class.java) }
    val platformAnalyticsApi: PlatformAnalyticsApi by lazy { retrofit.create(PlatformAnalyticsApi::class.java) }
    val reelsApi: ReelsApi by lazy { retrofit.create(ReelsApi::class.java) }
    val reportsApi: ReportsApi by lazy { retrofit.create(ReportsApi::class.java) }
    val sharePostsApi: SharePostsApi by lazy { retrofit.create(SharePostsApi::class.java) }
    val storiesApi: StoriesApi by lazy { retrofit.create(StoriesApi::class.java) }

    val storyCreateApi: StoryCreateApi by lazy {retrofit.create(StoryCreateApi::class.java)}
    val tokenApi: TokenApi by lazy { retrofit.create(TokenApi::class.java) }
    val userActivityApi: UserActivityApi by lazy { retrofit.create(UserActivityApi::class.java) }
    val userAnalyticsApi: UserAnalyticsApi by lazy { retrofit.create(UserAnalyticsApi::class.java) }
    val userMatchingApi: UserMatchingApi by lazy { retrofit.create(UserMatchingApi::class.java) }
    val userMediaApi: UserMediaApi by lazy { retrofit.create(UserMediaApi::class.java) }
    val userPostsApi: PostsApi by lazy { retrofit.create(PostsApi::class.java) }
    val userPreferencesApi: UserPreferencesApi by lazy { retrofit.create(UserPreferencesApi::class.java) }
    val createPostApi: UserCreatePostApi by lazy { retrofit.create(UserCreatePostApi::class.java) }
    val reactionsApi: ReactionsApi by lazy { retrofit.create(ReactionsApi::class.java) }
    val userSearchesApi: UserSearchsApi by lazy { retrofit.create(UserSearchsApi::class.java) }
    val userContentApi: UserContentApi by lazy {retrofit.create(UserContentApi::class.java)}
    val usersApi: UsersApi by lazy { retrofit.create(UsersApi::class.java) }
    val userSecurityApi: UserSecurityApi by lazy { retrofit.create(UserSecurityApi::class.java) }
    val coverPhotoUploadApi: CoverPhotoUploadApi by lazy { retrofit.create(CoverPhotoUploadApi::class.java) }
    val profilePictureUploadApi: ProfilePictureUploadApi by lazy { retrofit.create(ProfilePictureUploadApi::class.java) }
}



// ApiService.kt – add these interfaces at the end of the file

interface CoverPhotoUploadApi {
    @Multipart
    @POST("api/v1/users/media/cover-photo/")
    suspend fun uploadCoverPhoto(
        @Part image: MultipartBody.Part
    ): retrofit2.Response<UserImageDisplay>
}

interface ProfilePictureUploadApi {
    @Multipart
    @POST("api/v1/users/media/profile-picture/")
    suspend fun uploadProfilePicture(
        @Part image: MultipartBody.Part
    ): retrofit2.Response<UserImageDisplay>
}
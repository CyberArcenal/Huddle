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
import java.net.Proxy
import com.cyberarcenal.huddle.data.repositories.ChatUploadApi
import com.cyberarcenal.huddle.data.repositories.CoverPhotoUploadApi
import com.cyberarcenal.huddle.data.repositories.EventCreateApi
import com.cyberarcenal.huddle.data.repositories.GroupCreateApi
import com.cyberarcenal.huddle.data.repositories.MutingRepository
import com.cyberarcenal.huddle.data.repositories.ProfilePictureUploadApi
import com.cyberarcenal.huddle.data.repositories.ReelCreateApi
import com.cyberarcenal.huddle.data.repositories.StoryCreateApi
import com.cyberarcenal.huddle.data.repositories.UserCreatePostApi
import kotlinx.coroutines.runBlocking

object ApiService {
    private lateinit var appContext: Context

    // Tawagin ito sa iyong MainActivity o Application class
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val BASE_URL: String by lazy {
        if (isEmulator()) {
            "http://10.0.2.2:8000/"
        } else {
            "http://192.168.0.118:8000/"
        }
    }
    val WS_BASE_URL: String by lazy {
        if (isEmulator()) {
            "ws://10.0.2.2:8000/"
        } else {
            "ws://192.168.0.118:8000/"
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
        .proxy(Proxy.NO_PROXY)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val refreshRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(refreshHttpClient)
        .addConverterFactory(GsonConverterFactory.create(Serializer.gson))
        .build()

    val tokenRefresh: TokenApi by lazy { refreshRetrofit.create(TokenApi::class.java) }

    private val okHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(30, TimeUnit.SECONDS) // Increased for video uploads
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
                    val refreshToken = TokenManager.getRefreshToken(appContext) ?: return@runBlocking null

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
                                TokenManager.saveTokens(
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
                        TokenManager.clearAll(appContext)
                        null
                    } catch (e: Exception) {
                        // Refresh failed (expired refresh token), logout user
                        TokenManager.clearAll(appContext)
                        null
                    }
                }
            }
        })
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
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
    val followApi: FollowApi by lazy { retrofit.create(FollowApi::class.java) }
    val userSearchApi: UserSearchsApi by lazy { retrofit.create(UserSearchsApi::class.java) }
    val searchHistoryApi: SearchsHistoryApi by lazy { retrofit.create(SearchsHistoryApi::class.java) }
    val globalSearchApi: GlobalSearchApi by lazy { retrofit.create(GlobalSearchApi::class.java) }
    val dedicatedSearchApi: DedicatedSearchsApi by lazy { retrofit.create(DedicatedSearchsApi::class.java) }
    val groupSuggestionApi: GroupSuggestionApi by lazy { retrofit.create(GroupSuggestionApi::class.java) }
    val groupApi: GroupApi by lazy { retrofit.create(GroupApi::class.java) }
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
    val storyCreateApi: StoryCreateApi by lazy { retrofit.create(StoryCreateApi::class.java) }
    val tokenApi: TokenApi by lazy { retrofit.create(TokenApi::class.java) }
    val userActivityApi: UserActivityApi by lazy { retrofit.create(UserActivityApi::class.java) }
    val userAnalyticsApi: UserAnalyticsApi by lazy { retrofit.create(UserAnalyticsApi::class.java) }
    val userMatchingApi: UserMatchingApi by lazy { retrofit.create(UserMatchingApi::class.java) }
    val userMediaApi: UserMediaApi by lazy { retrofit.create(UserMediaApi::class.java) }
    val userPostsApi: PostsApi by lazy { retrofit.create(PostsApi::class.java) }
    val userPreferencesApi: UserPreferencesApi by lazy { retrofit.create(UserPreferencesApi::class.java) }
    val createPostApi: UserCreatePostApi by lazy { retrofit.create(UserCreatePostApi::class.java) }
    val reactionsApi: ReactionsApi by lazy { retrofit.create(ReactionsApi::class.java) }
    val userContentApi: UserContentApi by lazy { retrofit.create(UserContentApi::class.java) }
    val usersApi: UsersApi by lazy { retrofit.create(UsersApi::class.java) }
    val userSecurityApi: UserSecurityApi by lazy { retrofit.create(UserSecurityApi::class.java) }
    val coverPhotoUploadApi: CoverPhotoUploadApi by lazy { retrofit.create(CoverPhotoUploadApi::class.java) }
    val profilePictureUploadApi: ProfilePictureUploadApi by lazy { retrofit.create(ProfilePictureUploadApi::class.java) }
    val viewsApi: ViewsApi by lazy { retrofit.create(ViewsApi::class.java) }
    val bookmarksApi: BookmarksApi by lazy { retrofit.create(BookmarksApi::class.java) }
    val trendScoreApi: TrendScoreApi by lazy { retrofit.create(TrendScoreApi::class.java) }
    val notifyApi: NotifyApi by lazy {retrofit.create(NotifyApi::class.java)}
    val adminApi: AdminApi by lazy {retrofit.create(AdminApi::class.java)}
    val mutingApi: MutingApi by lazy {retrofit.create(MutingApi::class.java)}
    val personalityQuizApi: PersonalityQuizApi by lazy { retrofit.create(PersonalityQuizApi::class.java) }

    val eventCreateApi: EventCreateApi by lazy { retrofit.create(EventCreateApi::class.java) }

    // --- Bagong idinagdag para sa mga repository ---
    val blockingApi: BlockingApi by lazy { retrofit.create(BlockingApi::class.java) }
    val datingMessagesApi: DatingMessagesApi by lazy { retrofit.create(DatingMessagesApi::class.java) }
    val datingPreferencesApi: DatingPreferencesApi by lazy { retrofit.create(DatingPreferencesApi::class.java) }
    val friendshipsApi: FriendshipsApi by lazy { retrofit.create(FriendshipsApi::class.java) }
    val matchesApi: MatchesApi by lazy { retrofit.create(MatchesApi::class.java) }
    val mediaApi: MediaApi by lazy { retrofit.create(MediaApi::class.java) }
    val reelCreateApi: ReelCreateApi by lazy { retrofit.create(ReelCreateApi::class.java) }
    val createGroupApi: GroupCreateApi by lazy { retrofit.create(GroupCreateApi::class.java) }
    val templatesApi: EmailTemplatesApi by lazy { retrofit.create(EmailTemplatesApi::class.java)}
    val liveApi: LiveApi by lazy { retrofit.create(LiveApi::class.java) }
}




package com.cyberarcenal.huddle.ui.home

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.EventAnalyticsRepository
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.network.AuthManager
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.common.utils.ConfirmDialog
import com.cyberarcenal.huddle.ui.common.utils.ConfirmState
import com.cyberarcenal.huddle.ui.common.utils.rememberConfirmState
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.createStory.CreateStoryScreen
import com.cyberarcenal.huddle.ui.dating.DatingConversationScreen
import com.cyberarcenal.huddle.ui.dating.DatingProfileScreen
import com.cyberarcenal.huddle.ui.dating.DatingScreen
import com.cyberarcenal.huddle.ui.friends.FriendsScreen
import com.cyberarcenal.huddle.ui.home.components.HomeTopBar
import com.cyberarcenal.huddle.ui.home.components.ModernBottomNavigation
import com.cyberarcenal.huddle.ui.editprofile.EditProfileScreen
import com.cyberarcenal.huddle.ui.events.attendies.EventAttendeesScreen
import com.cyberarcenal.huddle.ui.events.createEvent.EventCreationScreen
import com.cyberarcenal.huddle.ui.events.eventDetail.EventDetailScreen
import com.cyberarcenal.huddle.ui.events.eventList.EventMainScreen
import com.cyberarcenal.huddle.ui.events.management.EventManagementScreen
import com.cyberarcenal.huddle.ui.groups.creategroup.GroupCreationScreen
import com.cyberarcenal.huddle.ui.groups.groupdetail.GroupDetailScreen
import com.cyberarcenal.huddle.ui.groups.management.GroupManagementScreen
import com.cyberarcenal.huddle.ui.groups.memberPreview.MemberPreviewScreen
import com.cyberarcenal.huddle.ui.highlight.HighlightCarouselScreen
import com.cyberarcenal.huddle.ui.profile.ProfileScreen
import com.cyberarcenal.huddle.ui.reel.create.ReelCreateScreen
import com.cyberarcenal.huddle.ui.reel.feed.ReelFeedScreen
import com.cyberarcenal.huddle.ui.search.SearchScreen
import com.cyberarcenal.huddle.ui.settings.ChangePasswordScreen
import com.cyberarcenal.huddle.ui.settings.DeactivateAccountScreen
import com.cyberarcenal.huddle.ui.settings.EditEmailScreen
import com.cyberarcenal.huddle.ui.settings.EditFieldScreen
import com.cyberarcenal.huddle.ui.settings.EditUsernameScreen
import com.cyberarcenal.huddle.ui.settings.MoreScreen
import com.cyberarcenal.huddle.ui.settings.ProfileDetailsScreen
import com.cyberarcenal.huddle.ui.settings.SecurityScreen
import com.cyberarcenal.huddle.ui.settings.SessionsScreen
import com.cyberarcenal.huddle.ui.settings.SettingsMainScreen
import com.cyberarcenal.huddle.ui.settings.TwoFactorScreen
import com.cyberarcenal.huddle.ui.storyviewer.StoryFeedViewerScreen
import com.cyberarcenal.huddle.ui.storyviewer.StoryListScreen
import com.cyberarcenal.huddle.ui.userpreference.UserPreferenceEditScreen
import com.cyberarcenal.huddle.ui.userpreference.UserPreferencesScreen
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.data.repositories.LiveRepository
import com.cyberarcenal.huddle.data.repositories.ReactionsRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.chat.conversations.ConversationListScreen
import com.cyberarcenal.huddle.ui.chat.conversations.StartChatScreen
import com.cyberarcenal.huddle.ui.chat.messages.ChatScreen
import com.cyberarcenal.huddle.ui.groups.GroupMainScreen
import com.cyberarcenal.huddle.ui.live.LiveStreamScreen
import com.cyberarcenal.huddle.ui.live.LiveViewModelFactory
import com.cyberarcenal.huddle.ui.live.StartLiveScreen
import com.cyberarcenal.huddle.ui.profile.components.PersonalityQuizScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val homeViewModel: HomeViewModel = viewModel()
    val context = LocalContext.current
    val bottomNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val backPressCount = remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Vibrator::class.java)
    val currentUser by homeViewModel.currentUser.collectAsState()
    val confirmState = rememberConfirmState()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val drawerItems = listOf(
        DrawerItem("Profile", Icons.Default.Person, "profile"),
        DrawerItem("Events", Icons.Default.Event, "events_main"),
        DrawerItem("Videos", Icons.Default.VideoLibrary, "reels"),
        DrawerItem("Saved", Icons.Default.Bookmark, "saved"),
        DrawerItem("Settings", Icons.Default.Settings, "settings"),
    )

    val gridItems = listOf(
        DrawerItem("Dating", Icons.Default.Favorite, "dating"),
        DrawerItem("FriendShips", Icons.Default.Person, "friendships"),
        DrawerItem("Events", Icons.Default.Event, "events_main"),
        DrawerItem("Videos", Icons.Default.VideoLibrary, "reels"),
        DrawerItem("Saved", Icons.Default.Bookmark, "saved"),
    )

    val showTopBar by remember(currentRoute) {
        derivedStateOf {
            val route = currentRoute.orEmpty()
            // Dito mo ilagay ang mga routes kung saan mo gustong I-HIDE ang TopBar
            val hideOnRoutes = listOf("create_post", "create_reel", "create_story", "story_list",
                "reels", "event", "preferences")
            val hideOnPrefixes = listOf("story_feed_viewer", "live", "event")

            !hideOnRoutes.contains(route) && hideOnPrefixes.none { route.startsWith(it) }
        }
    }

    val showBottomBar by remember(currentRoute) {
        derivedStateOf {
            val route = currentRoute.orEmpty()
            // Dito mo ilagay ang mga routes kung saan mo gustong I-HIDE ang BottomNav
            val hideOnRoutes = listOf(
                "create_post", "create_reel", "create_story", "create_group",
                "profile", "create_event", "edit_profile", "settings",
                "preferences", "conversations", "story_list", "event"
            )
            val hideOnPrefixes = listOf(
                "reels", "story", "story_feed_viewer", "highlight_carousel",
                "events_detail", "event_management", "event_attendees", "group",
                "group_management", "dating", "live", "conversations", "event"
            )

            !hideOnRoutes.contains(route) && hideOnPrefixes.none { route.startsWith(it) }
        }
    }

    LaunchedEffect(Unit) {
        val storedUser = TokenManager.getUser(context)
        if (storedUser != null && currentUser == null) {
            homeViewModel.setCurrentUserData(storedUser)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = DrawerDefaults.scrimColor,
        gesturesEnabled = showTopBar, // Kadalasan gesture ay active pag may topbar
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()
                    .width(300.dp),
                windowInsets = WindowInsets(0)
            ) {
                HomeDrawerContent(
                    currentUser = currentUser,
                    drawerItems = drawerItems,
                    gridItems = gridItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        coroutineScope.launch { drawerState.close() }
                        bottomNavController.navigate(route)
                    },
                    onLogout = {
                        coroutineScope.launch {
                            drawerState.close()
                            AuthManager.clearTokens(context)
                            TokenManager.updateToken(null)
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    },
                    confirmState = confirmState
                )
            }
        }) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface, topBar = {
        AnimatedVisibility(
            visible = showTopBar,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut()
        ) {
            HomeTopBar(
                navController = bottomNavController,
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToConversations = { bottomNavController.navigate("conversations") },
                onNavigateToCreatePost = { bottomNavController.navigate("create_post") },
                onNavigateToCreateStory = { bottomNavController.navigate("create_story") },
                onNavigateToReel = { bottomNavController.navigate("create_reel") },
                onNavigateToCreateEvent = { bottomNavController.navigate("create_event") },
                onNavigateToCreateGroup = { bottomNavController.navigate("create_group") },
                onNavigateToSearch = { bottomNavController.navigate("search") })
        }
    }, bottomBar = {
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut()
        ) {
            ModernBottomNavigation(
                navController = bottomNavController,
                currentUser = currentUser,
                onHomeReselect = {
                    if (currentRoute == "feed") {
                        homeViewModel.requestFeedRefresh()
                    }
                },
                onUnavailableClick = { },
                onMoreClick = {
                    coroutineScope.launch { drawerState.open() }
                })
        }
    }) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = bottomNavController,
                    startDestination = "feed",
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    composable("feed") {
                        HomeTabbedFeed(
                            navController = bottomNavController,
                            homeViewModel = homeViewModel,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "event_management/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val eventId =
                            backStackEntry.arguments?.getInt("eventId") ?: return@composable
                        EventManagementScreen(
                            eventId = eventId,
                            navController = bottomNavController,
                            eventRepository = EventRepository(),
                            attendanceRepository = EventAttendanceRepository(),
                            analyticsRepository = EventAnalyticsRepository(),
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "event_attendees/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val eventId =
                            backStackEntry.arguments?.getInt("eventId") ?: return@composable
                        EventAttendeesScreen(
                            eventId = eventId,
                            navController = bottomNavController,
                            attendanceRepository = EventAttendanceRepository(),
                            friendshipsRepository = FriendshipsRepository(),
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("create_event") {
                        EventCreationScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "event_detail/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val eventId =
                            backStackEntry.arguments?.getInt("eventId") ?: return@composable
                        EventDetailScreen(
                            eventId = eventId,
                            navController = bottomNavController,
                            eventRepository = EventRepository(),
                            attendanceRepository = EventAttendanceRepository(),
                            followRepository = FollowRepository(),
                            groupRepository = GroupRepository(),
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("events_main") {
                        EventMainScreen(
                            navController = bottomNavController,
                            eventRepository = EventRepository(),
                            attendanceRepository = EventAttendanceRepository(),
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("groups_main") {
                        GroupMainScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "create_post?groupId={groupId}", arguments = listOf(
                            navArgument("groupId") {
                                type = NavType.IntType
                                defaultValue = 0
                            })
                    ) {
                        CreatePostScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("search") { SearchScreen(globalSnackbarHostState = snackbarHostState) }
                    composable("create_story") {
                        CreateStoryScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("create_reel") {
                        ReelCreateScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("conversations") {
                        ConversationListScreen(
                            onNavigateToChat = { conversationId -> bottomNavController.navigate("chat/$conversationId") },
                            onNavigateToStartChat = {bottomNavController.navigate("start_chat")},
                        )
                    }

                    composable("start_chat") {
                        StartChatScreen(
                            onNavigateBack = { bottomNavController.popBackStack() },
                            onNavigateToChat = { id ->
                                bottomNavController.navigate("chat/$id") {
                                    popUpTo("conversations")
                                }
                            }
                        )
                    }

                    composable("chat/{conversationId}") { backStackEntry ->
                        val conversationId = backStackEntry.arguments?.getInt("conversationId");
                        if (conversationId !== null){
                            ChatScreen(
                                conversationId,
                                onBack = { bottomNavController.popBackStack() },
                            )
                        }

                    }

                    composable("group/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
                        if (groupId != null) {
                            GroupDetailScreen(
                                groupId = groupId,
                                navController = bottomNavController,
                                globalSnackbarHostState = snackbarHostState
                            )
                        }
                    }

                    composable("member_preview/{groupId}?name={name}&count={count}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
                            ?: return@composable
                        val groupName = backStackEntry.arguments?.getString("name")
                        val memberCount =
                            backStackEntry.arguments?.getString("count")?.toIntOrNull()
                        MemberPreviewScreen(
                            groupId = groupId,
                            groupName = groupName,
                            memberCount = memberCount,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("group_management/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
                            ?: return@composable
                        GroupManagementScreen(
                            groupId = groupId,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("create_group") {
                        GroupCreationScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("friendships") {
                        FriendsScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("preferences") {
                        UserPreferencesScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("preferences/edit/{categoryName}") { backStackEntry ->
                        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                        UserPreferenceEditScreen(
                            navController = bottomNavController,
                            categoryName = categoryName,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "reels/{reelId}?userId={userId}",
                        arguments = listOf(
                            navArgument("reelId") { type = NavType.IntType },
                            navArgument("userId") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->
                        val reelId = backStackEntry.arguments?.getInt("reelId") ?: 0
                        val userIdArg = backStackEntry.arguments?.getInt("userId") ?: -1
                        val userId = if (userIdArg == -1) null else userIdArg

                        ReelFeedScreen(
                            navController = bottomNavController,
                            currentUser = currentUser,
                            userId = userId,
                            initialReelId = reelId,
                            globalSnackbarHostState = snackbarHostState,
                        )
                    }
                    composable("reels") {
                        ReelFeedScreen(
                            navController = bottomNavController,
                            currentUser = currentUser,
                            globalSnackbarHostState = snackbarHostState,
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            userId = null,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState,
                        )
                    }

                    composable(
                        route = "profile/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getInt("userId")
                        ProfileScreen(
                            userId = userId,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState,
                        )
                    }

                    composable("edit_profile") {
                        EditProfileScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("settings") {
                        SettingsMainScreen(
                            navController = bottomNavController,
                            mainNav = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_profile_details") {
                        ProfileDetailsScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_security") {
                        SecurityScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_change_password") {
                        ChangePasswordScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_2fa") {
                        TwoFactorScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_sessions") {
                        SessionsScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_more") {
                        MoreScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("settings_deactivate_account") {
                        DeactivateAccountScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("edit_username") {
                        EditUsernameScreen(
                            bottomNavController, globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("edit_email") {
                        EditEmailScreen(bottomNavController, globalSnackbarHostState = snackbarHostState)
                    }
                    composable("edit_field/{fieldName}/{currentValue}") { backStackEntry ->
                        val fieldName = backStackEntry.arguments?.getString("fieldName") ?: ""
                        val currentValue = backStackEntry.arguments?.getString("currentValue") ?: ""
                        EditFieldScreen(
                            navController = bottomNavController,
                            fieldName = fieldName,
                            currentValue = currentValue,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "story_feed_viewer/{startIndex}/{sessionId}", arguments = listOf(
                            navArgument("startIndex") { type = NavType.IntType },
                            navArgument("sessionId") {
                                type = NavType.StringType
                            })
                    ) { backStackEntry ->
                        val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        StoryFeedViewerScreen(
                            startIndex = startIndex,
                            sessionId = sessionId,
                            navController = navController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("highlight_carousel/{index}/{sessionId}") { backStackEntry ->
                        val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        HighlightCarouselScreen(
                            sessionId = sessionId,
                            startIndex = index,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable("personality_test") {
                        PersonalityQuizScreen(
                            onDismiss = { bottomNavController.popBackStack() },
                            onComplete = { bottomNavController.popBackStack() },
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("dating") {
                        DatingScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable(
                        "dating_conversation/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
                        DatingConversationScreen(
                            userId = userId,
                            viewModel = viewModel(),
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                        route = "dating_profile/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
                        DatingProfileScreen(
                            userId = userId,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable(
                            route = "live/{liveId}",
                            arguments = listOf(navArgument("liveId") { type = NavType.IntType })

                        ) {
                            val liveId = it.arguments?.getInt("liveId") ?: return@composable
                            LiveStreamScreen(
                                liveId = liveId,
                                navController = bottomNavController,
                                viewModel = viewModel(factory = LiveViewModelFactory(LiveRepository(), CommentsRepository(), ReactionsRepository())),
                                snackbarHostState = snackbarHostState
                            )

                    }

                    composable("story_list") {
                        StoryListScreen(navController = bottomNavController)
                    }

                    composable("start_live") {
                        StartLiveScreen(
                            viewModel = viewModel(factory = LiveViewModelFactory(LiveRepository(), CommentsRepository(), ReactionsRepository())),
                            navController = bottomNavController
                        )
                    }

                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                    snackbar = { data ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val isError = data.visuals.message.contains(
                                    "error", ignoreCase = true
                                ) || data.visuals.message.contains(
                                    "failed", ignoreCase = true
                                )

                                Icon(
                                    imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Text(
                                    text = data.visuals.message,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                if (data.visuals.actionLabel != null) {
                                    TextButton(
                                        onClick = { data.performAction() },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = data.visuals.actionLabel!!,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    })
            }
        }
    }

    // At root, handle double‑tap
    BackHandler {
        if (!bottomNavController.popBackStack()) {
            if (backPressCount.intValue == 0) {
                backPressCount.intValue = 1
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(
                            50, VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION") vibrator?.vibrate(50)
                }
                coroutineScope.launch {
                    delay(2000)
                    backPressCount.intValue = 0
                }
            } else {
                (context as? Activity)?.finish()
            }
        }
    }


    ConfirmDialog(
        showDialog = confirmState.showDialog,
        onDismiss = { confirmState.hide() },
        onConfirm = confirmState.onConfirm,
        title = confirmState.title,
        message = confirmState.message,
        confirmText = confirmState.confirmText,
        dismissText = confirmState.dismissText,
        isConfirmDangerous = confirmState.isDangerous
    )
}

@Composable
private fun HomeDrawerContent(
    currentUser: UserProfile?,
    drawerItems: List<DrawerItem>,
    gridItems: List<DrawerItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    onLogout: () -> Unit,
    confirmState: ConfirmState
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            if (currentUser?.coverPhotoUrl != null) {
                AsyncImage(
                    model = currentUser.coverPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
                        )
                    )
                )
            )

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                AsyncImage(
                    model = currentUser?.profilePictureUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUser?.firstName ?: currentUser?.username ?: "Huddle User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary // O kaya Color.White kung gusto mo fixed white sa dark gradient
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        drawerItems.take(2).forEach { item ->
            NavigationDrawerItem(
                icon = {
                Icon(
                    item.icon, contentDescription = null, modifier = Modifier.size(24.dp)
                )
            },
                label = { Text(item.title, style = MaterialTheme.typography.labelLarge) },
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).height(56.dp),
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), thickness = 0.5.dp
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationDrawerItem(
                            icon = {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                            label = {
                                Text(
                                    item.title, style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = isSelected,
                            onClick = { onItemClick(item.route) },
                            modifier = Modifier.weight(1f).height(72.dp),
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    if (rowItems.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NavigationDrawerItem(
            icon = {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
            label = { Text("Settings", style = MaterialTheme.typography.labelLarge) },
            selected = currentRoute == "settings",
            onClick = { onItemClick("settings") },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).height(56.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = {
                confirmState.show(
                    title = "Logout",
                    message = "Are you sure you want to logout?",
                    confirmText = "Logout",
                    isDangerous = true,
                    onConfirm = {
                        onLogout()
                        confirmState.hide()
                    })
            }, modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Text("Logout", color = MaterialTheme.colorScheme.error)
        }
    }
}

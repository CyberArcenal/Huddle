package com.cyberarcenal.huddle.ui.home

import android.app.Activity
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Message
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.data.repositories.EventAnalyticsRepository
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.createStory.CreateStoryScreen
import com.cyberarcenal.huddle.ui.friends.FriendsScreen
import com.cyberarcenal.huddle.ui.home.components.HomeTopBar
import com.cyberarcenal.huddle.ui.home.components.ModernBottomNavigation
import com.cyberarcenal.huddle.ui.editprofile.EditProfileScreen
import com.cyberarcenal.huddle.ui.events.attendies.EventAttendeesScreen
import com.cyberarcenal.huddle.ui.events.createEvent.EventCreationScreen
import com.cyberarcenal.huddle.ui.events.eventDetail.EventDetailScreen
import com.cyberarcenal.huddle.ui.events.eventList.EventMainScreen
import com.cyberarcenal.huddle.ui.events.management.EventManagementScreen
import com.cyberarcenal.huddle.ui.groups.GroupMainScreen
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
import com.cyberarcenal.huddle.ui.storyviewer.StoryViewerScreen
import com.cyberarcenal.huddle.ui.userpreference.UserPreferenceEditScreen
import com.cyberarcenal.huddle.ui.userpreference.UserPreferencesScreen
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

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Show bottom nav + top bar only on main screens

    val drawerItems = listOf(
        DrawerItem("Profile", Icons.Default.Person, "profile"),
        DrawerItem("Dating", Icons.Default.Favorite, "dating"),
        DrawerItem("Events", Icons.Default.Event, "events_main"),
        DrawerItem("Videos", Icons.Default.VideoLibrary, "videos"),   // future
        DrawerItem("Saved", Icons.Default.Bookmark, "saved"),
        DrawerItem("Settings", Icons.Default.Settings, "settings"),
    )

    val gridItems = listOf(
        DrawerItem("Dating", Icons.Default.Favorite, "dating"),
        DrawerItem("Events", Icons.Default.Event, "events_main"),
        DrawerItem("Videos", Icons.Default.VideoLibrary, "reels"),
        DrawerItem("Saved", Icons.Default.Bookmark, "saved"),
    )

    val shouldShowBottomAndTopBar =
        currentRoute != "create_post" && currentRoute != "create_reel" && !currentRoute.orEmpty()
            .startsWith("reels") && !currentRoute.orEmpty()
            .startsWith("story") && !currentRoute.orEmpty()
            .startsWith("story_feed_viewer") && !currentRoute.orEmpty()
            .startsWith("edit_profile") && !currentRoute.orEmpty()
            .startsWith("settings") && !currentRoute.orEmpty()
            .startsWith("preferences") && !currentRoute.orEmpty()
            .startsWith("profile") && !currentRoute.orEmpty()
            .startsWith("groups_main") && !currentRoute.orEmpty()
            .startsWith("create_story") && !currentRoute.orEmpty()
            .startsWith("highlight_carousel") && !currentRoute.orEmpty()
            .startsWith("create_group") && !currentRoute.orEmpty()
            .startsWith("create_post") && !currentRoute.orEmpty()
            .startsWith("create_event") && !currentRoute.orEmpty()
            .startsWith("events_main") && !currentRoute.orEmpty()
            .startsWith("events_detail") && !currentRoute.orEmpty()
            .startsWith("event_management")
                && !currentRoute.orEmpty().startsWith("event_attendees")
                && !currentRoute.orEmpty().startsWith("group")
                && !currentRoute.orEmpty().startsWith("grou_management")


    LaunchedEffect(Unit) {
        val storedUser = TokenManager.getUser(context)
        homeViewModel.setCurrentUserData(storedUser)
    }

    ModalNavigationDrawer(
        drawerState = drawerState, scrimColor = Color.Black.copy(alpha = 0.32f), drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 5.dp),
                modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()
                    .width(300.dp) // slightly wider for better layout
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp) // Bottom padding for drawer content
                ) {
                    // ---------- HEADER: cover photo + avatar + full name ----------
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    ) {
                        // Cover photo
                        if (currentUser?.coverPhotoUrl != null) {
                            AsyncImage(
                                model = currentUser?.coverPhotoUrl,
                                contentDescription = "Cover photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }

                        // Gradient overlay for better text visibility
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent, Color.Black.copy(alpha = 0.6f)
                                    ), startY = 0f, endY = Float.POSITIVE_INFINITY
                                )
                            )
                        )

                        // Avatar and name at bottom
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        ) {
                            AsyncImage(
                                model = currentUser?.profilePictureUrl,
                                contentDescription = "Profile picture",
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = buildString {
                                    append(currentUser?.firstName ?: "")
                                    if (!currentUser?.lastName.isNullOrBlank()) {
                                        append(" ")
                                        append(currentUser?.lastName)
                                    }
                                    if (currentUser?.firstName.isNullOrBlank()) {
                                        append(currentUser?.username ?: "User")
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------- PROFILE item (first) ----------
                    NavigationDrawerItem(
                        icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                        label = {
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        selected = currentRoute == "profile",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            bottomNavController.navigate("profile")
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.4f
                            ),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.Black.copy(alpha = 0.7f),
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(56.dp)
                    )

                    // ---------- SETTINGS item (second) ----------
                    NavigationDrawerItem(
                        icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                        label = {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        selected = currentRoute == "settings",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            bottomNavController.navigate("settings")
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.4f
                            ),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.Black.copy(alpha = 0.7f),
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(56.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray.copy(alpha = 0.4f)
                    )

                    // ---------- GRID for the remaining items ----------
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
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            coroutineScope.launch { drawerState.close() }
                                            when (item.route) {
                                                "dating" -> {
                                                    Toast.makeText(
                                                        context,
                                                        "Dating feature coming soon",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                "saved" -> {
                                                    Toast.makeText(
                                                        context,
                                                        "Saved posts coming soon",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                else -> bottomNavController.navigate(item.route)
                                            }
                                        },
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                            ),
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedContainerColor = Color.Transparent,
                                            unselectedTextColor = Color.Black.copy(alpha = 0.7f),
                                            unselectedIconColor = Color.Gray
                                        ),
                                        modifier = Modifier.weight(1f).height(72.dp)
                                    )
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp)) // Extra padding before footer

                    // Footer version
                    Text(
                        text = "Huddle v1.2.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }) {
        Scaffold(containerColor = Color.White, topBar = {
            if (shouldShowBottomAndTopBar) {
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
            if (shouldShowBottomAndTopBar) {
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
            // Gagamit ng Box para ma-overlay ang SnackbarHost nang direkta sa ilalim ng screen, independent sa Scaffold slot
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = bottomNavController,
                    startDestination = "feed",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    // Main Feed with Tabs
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
                    ) { backStackEntry ->
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
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Conversations Coming Soon",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    composable(
                        route = "chat/{conversationId}",
                        arguments = listOf(navArgument("conversationId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val conversationId = backStackEntry.arguments?.getInt("conversationId")

                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Chat Room: $conversationId",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Messaging Feature Coming Soon",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    composable("group/{groupId}") { backStackEntry ->
                        val groupId =
                            backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: null
                        if (groupId !== null) {
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


                    // Profile of a specific user by ID
                    composable("friends") {
                        FriendsScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
                    composable(
                        "preferences",
                    ) {

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

                    composable("reels/{reelId}") { backStackEntry ->
                        val reelId =
                            backStackEntry.arguments?.getString("reelId")?.toIntOrNull() ?: 0
                        ReelFeedScreen(
                            navController = bottomNavController,
                            currentUser = currentUser,
                            initialReelId = reelId, globalSnackbarHostState = snackbarHostState,
                        )
                    }
                    // In HomeScreen.kt, inside NavHost
                    composable("reels") {
                        ReelFeedScreen(
                            navController = bottomNavController,
                            currentUser = currentUser,
                            globalSnackbarHostState = snackbarHostState,
                        )
                    }


                    // Profile of the current logged-in user
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

                    // Edit Profile Screen
                    composable("edit_profile") {
                        EditProfileScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    // Settings main
                    composable("settings") {
                        SettingsMainScreen(
                            navController = bottomNavController,
                            mainNav = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// Profile Details
                    composable("settings_profile_details") {
                        ProfileDetailsScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// Security
                    composable("settings_security") {
                        SecurityScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// Change Password
                    composable("settings_change_password") {
                        ChangePasswordScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// Two-Factor
                    composable("settings_2fa") {
                        TwoFactorScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// Sessions
                    composable("settings_sessions") {
                        SessionsScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// More
                    composable("settings_more") {
                        MoreScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }
// Deactivate Account
                    composable("settings_deactivate_account") {
                        DeactivateAccountScreen(
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("edit_username") {
                        EditUsernameScreen(navController, globalSnackbarHostState = snackbarHostState)
                    }
                    composable("edit_email") {
                        EditEmailScreen(navController, globalSnackbarHostState = snackbarHostState)
                    }
                    composable("edit_field/{fieldName}/{currentValue}") { backStackEntry ->
                        val fieldName = backStackEntry.arguments?.getString("fieldName") ?: ""
                        val currentValue = backStackEntry.arguments?.getString("currentValue") ?: ""
                        EditFieldScreen(
                            navController = navController,
                            fieldName = fieldName,
                            currentValue = currentValue,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("story/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                        StoryViewerScreen(
                            userId = userId,
                            navController = bottomNavController,
                            globalSnackbarHostState = snackbarHostState
                        )
                    }


                    // In your NavHost
                    composable("story_feed_viewer/{index}") { backStackEntry ->
                        val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        StoryFeedViewerScreen(
                            index, bottomNavController, globalSnackbarHostState = snackbarHostState
                        )
                    }

                    composable("highlight_carousel/{index}") { backStackEntry ->
                        val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        HighlightCarouselScreen(
                            index, bottomNavController, globalSnackbarHostState = snackbarHostState
                        )
                    }

                }

                // GLOBAL SNACKBAR HOST - Naka-overlay sa pinaka-ibaba
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ), // FIXED: Ginamit ang start/end sa halip na horizontal
                    snackbar = { data ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF323232).copy(alpha = 0.9f), // Semi-transparent dark
                                contentColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Icon base sa message content
                                val isError = data.visuals.message.contains(
                                    "error", ignoreCase = true
                                ) || data.visuals.message.contains(
                                    "failed", ignoreCase = true
                                )

                                Icon(
                                    imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (isError) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
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


    BackHandler {
        if (!bottomNavController.popBackStack()) {
            // At root, handle double‑tap
            if (backPressCount.intValue == 0) {
                backPressCount.intValue = 1
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                vibrator?.vibrate(50)
                coroutineScope.launch {
                    delay(2000)
                    backPressCount.intValue = 0
                }
            } else {
                (context as? Activity)?.finish()
            }
        }
    }
}

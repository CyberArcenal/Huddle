package com.cyberarcenal.huddle.ui.home

import android.app.Activity
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.createStory.CreateStoryScreen
import com.cyberarcenal.huddle.ui.friends.FriendsScreen
import com.cyberarcenal.huddle.ui.home.components.HomeTopBar
import com.cyberarcenal.huddle.ui.home.components.ModernBottomNavigation
import com.cyberarcenal.huddle.ui.editprofile.EditProfileScreen
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
import com.cyberarcenal.huddle.ui.settings.SettingsScreen
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

    val backPressCount = remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Vibrator::class.java)

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBars = currentRoute != "create_post" &&
            currentRoute != "create_reel" &&
            !currentRoute.orEmpty().startsWith("reels") &&
            !currentRoute.orEmpty().startsWith("story") &&
            !currentRoute.orEmpty().startsWith("story_feed_viewer") &&
            currentRoute != "edit_profile" &&
            currentRoute != "settings" &&
            currentRoute != "preferences"
            && !currentRoute.orEmpty().startsWith("profile")
            && !currentRoute.orEmpty().startsWith("create_story")
            && !currentRoute.orEmpty().startsWith("highlight_carousel")
            && !currentRoute.orEmpty().startsWith("create_group")
            && !currentRoute.orEmpty().startsWith("create_post")

    Scaffold(
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    // Inalis ang windowInsetsPadding dito para sumadsad sa ilalim
                    .imePadding(),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        // Alisin ang rounded corners
                        shape = RectangleShape,
                        // Gamitin ang primary o inverseSurface pero walang elevation
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        // ITO ANG PINAKAMAHALAGA: Alisin ang default 8dp-12dp margin ng Snackbar
                        modifier = Modifier.padding(0.dp)
                    )
                }
            )
        },
        topBar = {
            if (shouldShowBars) {
                HomeTopBar(
                    navController = bottomNavController,
                    onNavigateToNotifications = { navController.navigate("notifications") },
                    onNavigateToConversations = { bottomNavController.navigate("conversations") },
                    onNavigateToCreatePost = { bottomNavController.navigate("create_post") },
                    onNavigateToCreateStory = { bottomNavController.navigate("create_story") },
                    onNavigateToReel = { bottomNavController.navigate("create_reel") },
                    onNavigateToCreateEvent = {},
                    onNavigateToCreateGroup = { bottomNavController.navigate("create_group")}
                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                ModernBottomNavigation(
                    navController = bottomNavController,
                    {
                        if (currentRoute == "feed") {
                            homeViewModel.requestFeedRefresh()
                        }
                    },
                    onUnavailableClick = { }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "feed",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main Feed with Tabs
            composable("feed") {
                HomeTabbedFeed(navController = bottomNavController, homeViewModel)
            }

            composable("groups_main") {
                GroupMainScreen(navController = navController)
            }

            composable(
                route = "create_post?groupId={groupId}",
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                CreatePostScreen(navController = bottomNavController)
            }
            composable("search") { SearchScreen() }
            composable("create_story") { CreateStoryScreen(navController = bottomNavController) }
            composable("create_reel") { ReelCreateScreen(navController = bottomNavController) }
            composable("conversations") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
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
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: null
                if (groupId !== null) {
                    GroupDetailScreen(groupId = groupId, navController = bottomNavController)
                }
            }

            composable("member_preview/{groupId}?name={name}&count={count}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
                    ?: return@composable
                val groupName = backStackEntry.arguments?.getString("name")
                val memberCount = backStackEntry.arguments?.getString("count")?.toIntOrNull()
                MemberPreviewScreen(
                    groupId = groupId,
                    groupName = groupName,
                    memberCount = memberCount,
                    navController = navController
                )
            }

            composable("group_management/{groupId}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
                    ?: return@composable
                GroupManagementScreen(
                    groupId = groupId,
                    navController = navController
                )
            }

            composable("create_group") {
                GroupCreationScreen(navController = navController)
            }


            // Profile of a specific user by ID
            composable("friends") {
                FriendsScreen(navController = bottomNavController)
            }
            composable(
                "preferences",
            ) {

                UserPreferencesScreen(navController = bottomNavController)
            }
            composable("preferences/edit/{categoryName}") { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                UserPreferenceEditScreen(
                    navController = bottomNavController,
                    categoryName = categoryName
                )
            }

            composable("reels/{reelId}") { backStackEntry ->
                val reelId = backStackEntry.arguments?.getString("reelId")?.toIntOrNull() ?: 0
                ReelFeedScreen(
                    navController = bottomNavController,
                    initialReelId = reelId,
                )
            }
            // In HomeScreen.kt, inside NavHost
            composable("reels") {
                ReelFeedScreen(
                    navController = bottomNavController,
                )
            }


            // Profile of the current logged-in user
            composable("profile") {
                ProfileScreen(
                    userId = null,
                    navController = bottomNavController
                )
            }

            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId")
                ProfileScreen(
                    userId = userId,
                    navController = bottomNavController
                )
            }

            // Edit Profile Screen
            composable("edit_profile") {
                EditProfileScreen(navController = bottomNavController)
            }

            // Settings Screen
            composable("settings") {
                SettingsScreen(navController = bottomNavController)
            }

            composable("story/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                StoryViewerScreen(userId = userId, navController = bottomNavController)
            }


            // In your NavHost
            composable("story_feed_viewer/{index}") { backStackEntry ->
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                StoryFeedViewerScreen(index, navController)
            }

            composable("highlight_carousel/{index}") { backStackEntry ->
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                HighlightCarouselScreen(index, navController)
            }

        }
    }


    BackHandler {
        if (!bottomNavController.popBackStack()) {
            // At root, handle double‑tap
            if (backPressCount.value == 0) {
                backPressCount.value = 1
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                vibrator?.vibrate(50)
                coroutineScope.launch {
                    delay(2000)
                    backPressCount.value = 0
                }
            } else {
                (context as? Activity)?.finish()
            }
        }
    }
}

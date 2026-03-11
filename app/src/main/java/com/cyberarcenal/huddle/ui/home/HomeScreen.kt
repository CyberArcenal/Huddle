package com.cyberarcenal.huddle.ui.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyberarcenal.huddle.ui.chat.ChatScreen
import com.cyberarcenal.huddle.ui.conversations.ConversationsScreen
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.createstory.CreateStoryScreen
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.feed.FeedViewModel
import com.cyberarcenal.huddle.ui.home.components.HomeTopBar
import com.cyberarcenal.huddle.ui.home.components.ModernBottomNavigation
import com.cyberarcenal.huddle.ui.profile.EditProfileScreen
import com.cyberarcenal.huddle.ui.profile.ProfileScreen
import com.cyberarcenal.huddle.ui.search.SearchScreen
import com.cyberarcenal.huddle.ui.settings.SettingsScreen
import com.cyberarcenal.huddle.ui.storyviewer.StoryViewerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val bottomNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Itago ang bars sa specific screens
    val shouldShowBars = currentRoute != "create_post" &&
            currentRoute != "create_story" &&
            !currentRoute.orEmpty().startsWith("story") &&
            currentRoute != "edit_profile" &&
            currentRoute != "settings" &&
            !currentRoute.orEmpty().startsWith("profile")

    val feedViewModel: FeedViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (shouldShowBars) {
                HomeTopBar(
                    navController = navController,
                    onNavigateToNotifications = { navController.navigate("notifications") },
                    onNavigateToConversations = {
                        try {
                            bottomNavController.navigate("conversations")
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Navigation error: ${e.message}")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
                ) {
                    ModernBottomNavigation(
                        navController = bottomNavController,
                        onHomeReselect = { feedViewModel.requestScrollToTop() },
                        onUnavailableClick = { }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "feed",
            modifier = Modifier.padding(
                top = if (shouldShowBars) innerPadding.calculateTopPadding() else 0.dp,
                bottom = 0.dp
            )
        ) {
            composable("feed") { FeedScreen(navController = bottomNavController, viewModel = feedViewModel) }
            composable("search") { SearchScreen() }

            // Profiles
            composable("profile") { ProfileScreen(userId = null, navController = bottomNavController) }
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId")
                ProfileScreen(userId = userId, navController = bottomNavController)
            }

            // Edit Profile & Settings
            composable("edit_profile") { EditProfileScreen(navController = bottomNavController) }
            composable("settings") { SettingsScreen(navController = bottomNavController) }

            composable("create_post") { CreatePostScreen(navController = bottomNavController) }
            composable("create_story") { CreateStoryScreen(navController = bottomNavController) }

            composable("reels") { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Reels") } }
            composable("story/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                StoryViewerScreen(userId = userId, navController = bottomNavController)
            }

            composable("conversations") { ConversationsScreen(navController = bottomNavController) }
            composable(
                route = "chat/{conversationId}",
                arguments = listOf(navArgument("conversationId") { type = NavType.IntType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getInt("conversationId")
                if (conversationId != null) {
                    ChatScreen(navController = bottomNavController, conversationId = conversationId)
                }
            }
        }
    }
}
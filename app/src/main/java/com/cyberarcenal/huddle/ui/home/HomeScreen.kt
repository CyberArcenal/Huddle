package com.cyberarcenal.huddle.ui.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.createstory.CreateStoryScreen
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.feed.FeedType
import com.cyberarcenal.huddle.ui.feed.FeedViewModel
import com.cyberarcenal.huddle.ui.feed.FeedViewModelFactory
import com.cyberarcenal.huddle.ui.friends.FriendsScreen
import com.cyberarcenal.huddle.ui.home.components.HomeTopBar
import com.cyberarcenal.huddle.ui.home.components.ModernBottomNavigation
import com.cyberarcenal.huddle.ui.profile.EditProfileScreen
import com.cyberarcenal.huddle.ui.profile.ProfileScreen
import com.cyberarcenal.huddle.ui.search.SearchScreen
import com.cyberarcenal.huddle.ui.settings.SettingsScreen
import com.cyberarcenal.huddle.ui.storyviewer.StoryViewerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val bottomNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBars = currentRoute != "create_post" &&
            currentRoute != "create_story" &&
            !currentRoute.orEmpty().startsWith("story") &&
            currentRoute != "edit_profile" &&
            currentRoute != "settings" &&
            !currentRoute.orEmpty().startsWith("profile")

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (shouldShowBars) {
                HomeTopBar(
                    navController = navController,
                    onNavigateToNotifications = { navController.navigate("notifications") },
                    onNavigateToConversations = { bottomNavController.navigate("conversations") }
                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                ModernBottomNavigation(
                    navController = bottomNavController,
                    onHomeReselect = { },
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
                HomeTabbedFeed(navController = bottomNavController)
            }
            composable("create_post") {
                CreatePostScreen(navController = bottomNavController)
            }
            composable("search") { SearchScreen() }
            composable("create_story") { CreateStoryScreen(navController = bottomNavController) }
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

            composable("story/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                StoryViewerScreen(userId = userId, navController = bottomNavController)
            }
        }
    }
}

@Composable
fun HomeTabbedFeed(navController: NavController) {
    val tabs = listOf("Home", "Discover", "Friends", "Following", "Groups")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Replace TabRow with ScrollableTabRow
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else Color.Gray
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val feedType = when (page) {
                0 -> FeedType.HOME // 'Friends' logic in backend
                1 -> FeedType.DISCOVER
                2 -> FeedType.FRIENDS
                3 -> FeedType.FOLLOWING
                4 -> FeedType.GROUPS
                else -> FeedType.HOME
            }

            val viewModel: FeedViewModel = viewModel(
                key = "feed_${feedType.name}",
                factory = FeedViewModelFactory(
                    postRepository = UserPostsRepository(),
                    commentRepository = CommentsRepository(),
                    reactionsRepository = UserReactionsRepository(),
                    storyFeedRepository = StoriesRepository(),
                    feedType = feedType,
                    feedRepository = FeedRepository(),
                )
            )

            FeedScreen(
                navController = navController,
                viewModel = viewModel,
                feedType = feedType
            )
        }
    }
}
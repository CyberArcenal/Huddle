package com.cyberarcenal.huddle.ui.home
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.ui.chat.ChatScreen
import com.cyberarcenal.huddle.ui.conversations.ConversationsScreen
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.createstory.CreateStoryScreen
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.feed.FeedViewModel
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
    val scope = rememberCoroutineScope()

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Itago ang bars sa specific screens
    val shouldShowBars = currentRoute != "create_post" &&
                        currentRoute != "create_story" &&
                        currentRoute?.startsWith("story") != true &&
                        currentRoute != "edit_profile" &&
                        currentRoute != "settings"

    val feedViewModel: FeedViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (shouldShowBars) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp,
                                lineHeight = 25.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("notifications") }) {
                            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            try {
                                bottomNavController.navigate("conversations")
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Navigation error: ${e.message}")
                            }
                        }) {
                            Icon(imageVector = Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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

@Composable
fun ModernBottomNavigation(
    navController: NavController,
    onHomeReselect: () -> Unit,
    onUnavailableClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("feed", Icons.Outlined.Home, Icons.Filled.Home, R.string.nav_home),
        BottomNavItem("search", Icons.Outlined.Search, Icons.Filled.Search, R.string.nav_explore),
        BottomNavItem("reels", Icons.Outlined.PlayCircle, Icons.Filled.PlayCircle, R.string.nav_reels),
        BottomNavItem("profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_profile)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute == item.route) {
                                if (item.route == "feed") onHomeReselect()
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelRes),
                        modifier = Modifier.size(26.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    @androidx.annotation.StringRes val labelRes: Int
)

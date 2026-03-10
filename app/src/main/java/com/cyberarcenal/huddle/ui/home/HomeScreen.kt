package com.cyberarcenal.huddle.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.ui.createpost.CreatePostScreen
import com.cyberarcenal.huddle.ui.events.createevent.CreateEventScreen
import com.cyberarcenal.huddle.ui.events.eventdetail.EventDetailScreen
import com.cyberarcenal.huddle.ui.events.eventslist.EventsListScreen
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.groups.creategroup.CreateGroupScreen
import com.cyberarcenal.huddle.ui.groups.groupdetail.GroupDetailScreen
import com.cyberarcenal.huddle.ui.groups.groupslist.GroupsListScreen
import com.cyberarcenal.huddle.ui.profile.ProfileScreen
import com.cyberarcenal.huddle.ui.search.SearchScreen
import com.cyberarcenal.huddle.ui.storyviewer.StoryViewerScreen
import com.cyberarcenal.huddle.ui.theme.Gradients
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val bottomNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pre‑fetch string resources so they are available during composition
    val notificationMessage = stringResource(R.string.notifications_coming_soon)
    val messagesMessage = stringResource(R.string.messages_coming_soon)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Notification button – now navigates to the NotificationsScreen
                    IconButton(
                        onClick = {
                            navController.navigate("notifications")
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.cd_notifications),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    // Messages button – still shows a snackbar (placeholder)
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(message = messagesMessage)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = stringResource(R.string.cd_messages),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
            ) {
                ModernBottomNavigation(
                    navController = bottomNavController,
                    onUnavailableClick = { route ->
                        // If a bottom tab is clicked but not yet implemented, show a snackbar
                        scope.launch {
                            snackbarHostState.showSnackbar("$route coming soon")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "feed",
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            composable("feed") { FeedScreen(navController = bottomNavController) }
            composable("search") { SearchScreen() }
            composable("profile") { ProfileScreen(userId = null, navController = bottomNavController) }
            composable("create_post") { CreatePostScreen(navController = bottomNavController) }
            composable("reels") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Reels coming soon")
                }
            }
            composable("story/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                StoryViewerScreen(userId = userId, navController = bottomNavController)
            }
            composable("groups") { GroupsListScreen(navController = navController) }
            composable("groupdetail/{groupId}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull()
                if (groupId != null) {
                    GroupDetailScreen(navController = navController, groupId = groupId)
                }
            }
            composable("creategroup") { CreateGroupScreen(navController = navController) }
            composable("events") { EventsListScreen(navController = navController) }
            composable("eventdetail/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")?.toIntOrNull()
                if (eventId != null) {
                    EventDetailScreen(navController = navController, eventId = eventId)
                }
            }
            composable("createevent") { CreateEventScreen(navController = navController) }
        }
    }
}

@Composable
fun ModernBottomNavigation(
    navController: NavController,
    onUnavailableClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("feed", Icons.Outlined.Home, Icons.Filled.Home, R.string.nav_home),
        BottomNavItem("search", Icons.Outlined.Search, Icons.Filled.Search, R.string.nav_explore),
        BottomNavItem("add", Icons.Default.Add, Icons.Default.Add, R.string.nav_post),
        BottomNavItem("reels", Icons.Outlined.PlayCircle, Icons.Filled.PlayCircle, R.string.nav_reels),
        BottomNavItem("profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_profile)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
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

                if (item.route == "add") {
                    // FAB-like create post button (always available)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Gradients.buttonGradient)
                            .clickable {
                                if (currentRoute != "create_post") {
                                    navController.navigate("create_post") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(item.labelRes),
                            tint = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                when (item.route) {
                                    // Routes that are actually defined in NavHost
                                    "feed", "search", "profile", "reels" -> {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    // Any other route (should not happen, but safe fallback)
                                    else -> onUnavailableClick(item.route)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.labelRes),
                            modifier = Modifier.size(26.dp),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Gray.copy(alpha = 0.5f)
                            }
                        )
                    }
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
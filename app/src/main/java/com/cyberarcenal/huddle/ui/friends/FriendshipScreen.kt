package com.cyberarcenal.huddle.ui.friends

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.user.UserItem
import com.cyberarcenal.huddle.utils.formatRelativeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendshipScreen(
    viewModel: FriendshipViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar((actionState as ActionState.Success).message)
                viewModel.resetActionState()
            }
            is ActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as ActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FriendshipHeader(
                onSearch = { /* TODO: Implement Search */ },
                onAddFriendClick = { /* TODO: Implement Add Friend */ },
                requestCount = (uiState as? FriendshipUiState.Success)?.incomingRequests?.size ?: 0
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FriendshipTabs(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            Crossfade(targetState = uiState, label = "tab_fade") { state ->
                when (state) {
                    is FriendshipUiState.Loading -> LoadingState()
                    is FriendshipUiState.Error -> ErrorState(state.message) { viewModel.loadAllData() }
                    is FriendshipUiState.Success -> {
                        when (selectedTab) {
                            0 -> ConnectionsTab(state, viewModel, navController)
                            1 -> RequestsTab(state, viewModel, navController)
                            2 -> SuggestionsTab(state, viewModel, navController)
                            3 -> FollowersTabPage(null, viewModel, navController)
                            4 -> FollowingTab(null, viewModel, navController)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendshipHeader(
    onSearch: (String) -> Unit,
    onAddFriendClick: () -> Unit,
    requestCount: Int
) {
    TopAppBar(
        title = {
            Text("Friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            Box {
                IconButton(onClick = onAddFriendClick) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
                }
                if (requestCount > 0) {
                    Badge(
                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(requestCount.toString(), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    )
}

@Composable
private fun FriendshipTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Connections", "Requests", "Suggestions", "Followers", "Following")

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            )
        }
    }
}

@Composable
fun ConnectionsTab(
    state: FriendshipUiState.Success,
    viewModel: FriendshipViewModel,
    navController: NavController
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.pinnedFriends.isNotEmpty()) {
            item {
                Text(
                    "Pinned",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.pinnedFriends) { pinned ->
                        PinnedFriendItem(pinned) { 
                            pinned.friend?.id?.let { navController.navigate("profile/$it") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (state.friends.isEmpty()) {
            item { EmptyState("No connections yet", "Discover people") { viewModel.selectTab(2) } }
        } else {
            item {
                Text(
                    "All Connections",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
            items(state.friends) { friendship ->
                FriendListItem(
                    friendship = friendship,
                    onItemClick = { friendship.friend?.id?.let { navController.navigate("profile/$it") } },
                    onMessageClick = { /* TODO: Open Chat */ },
                    onTogglePin = { friendship.id?.let { viewModel.togglePinFriend(it, friendship.tag) } }
                )
            }
        }
    }
}

@Composable
fun SuggestionsTab(
    state: FriendshipUiState.Success,
    viewModel: FriendshipViewModel,
    navController: NavController
) {
    val matches by viewModel.matchingManager.matches.collectAsState()
    val followStatuses by viewModel.followManager.followStatuses.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.suggestions.isNotEmpty()) {
            item {
                Text("Suggested Connections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.suggestions) { suggestion ->
                        suggestion.user?.let { user ->
                            user.id?.let { userId ->
                                UserItem(
                                    user = user,
                                    isVertical = true,
                                    onFollowClick = { viewModel.followManager.toggleFollow(userId, followStatuses[userId] ?: user.isFollowing ?: false, user.username ?: "") },
                                    onItemClick = { navController.navigate("profile/$userId") },
                                    isFollowing = followStatuses[userId] ?: user.isFollowing ?: false,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
            }
        }

        if (matches.isNotEmpty()) {
            item {
                Text("Best Matches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(matches) { match ->
                match.user?.id?.let { userId ->
                    MatchItem(match) { navController.navigate("profile/$userId") }
                }
            }
        }
    }
}

@Composable
fun PinnedFriendItem(pinned: FriendshipMinimal, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.width(72.dp)
    ) {
        AsyncImage(
            model = pinned.friend?.profilePictureUrl,
            contentDescription = null,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(pinned.friend?.username ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FriendListItem(
    friendship: FriendshipMinimal,
    onItemClick: () -> Unit,
    onMessageClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onItemClick() },
        headlineContent = { Text(friendship.friend?.fullName ?: friendship.friend?.username ?: "User", fontWeight = FontWeight.Bold) },
        supportingContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (friendship.tag == TagEnum.PINNED) {
                    Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("Friend since ${formatRelativeDate(friendship.createdAt)}", fontSize = 12.sp) 
            }
        },
        leadingContent = {
            AsyncImage(
                model = friendship.friend?.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onMessageClick) { Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onTogglePin) {
                    Icon(Icons.Default.PushPin, null, tint = if (friendship.tag == TagEnum.PINNED) MaterialTheme.colorScheme.primary else Color.LightGray)
                }
            }
        }
    )
}

@Composable
fun MatchItem(match: UserMatchScore, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = match.user?.profilePictureUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(match.user?.fullName ?: match.user?.username ?: "", fontWeight = FontWeight.Bold)
                Text("${match.score}% Compatibility", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable fun LoadingState() { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(strokeWidth = 3.dp) } }
@Composable fun ErrorState(msg: String, onRetry: () -> Unit) { 
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
@Composable fun EmptyState(msg: String, cta: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.PeopleOutline, null, Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(msg, color = Color.Gray, fontWeight = FontWeight.Medium)
        if (cta != null) { Button(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 12.dp)) { Text(cta) } }
    }
}

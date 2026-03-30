package com.cyberarcenal.huddle.ui.friends

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.common.user.UserAvatar
import com.cyberarcenal.huddle.ui.common.user.DynamicIslandFollowButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingTab(
    userId: Int?,
    viewModel: FriendshipViewModel,
    navController: NavController
) {
    val followStatuses by viewModel.followManager.followStatuses.collectAsState()
    val loadingUserIds by viewModel.followManager.loadingUserIds.collectAsState()
    
    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val followingItems = remember(userId) {
        viewModel.followManager.getFollowingFlow(userId)
    }.collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header & Summary Strip
        FollowingHeader(
            count = followingItems.itemCount,
            showOnlyActive = showOnlyActive,
            onActiveToggle = { showOnlyActive = it },
            onSearch = { searchQuery = it }
        )

        // Filter Chips
        FilterChipRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // Following List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(followingItems.itemCount) { index ->
                followingItems[index]?.let { user ->
                    FollowingUserItem(
                        user = user,
                        isFollowing = followStatuses[user.id] ?: user.isFollowing ?: true,
                        isLoading = loadingUserIds[user.id] ?: false,
                        onFollowToggle = {
                            user.id?.let { id ->
                                viewModel.followManager.toggleFollow(
                                    id, 
                                    followStatuses[id] ?: user.isFollowing ?: true,
                                    user.username ?: ""
                                )
                            }
                        },
                        onMessageClick = { /* Open Chat */ },
                        onMoreClick = { /* Show Menu */ },
                        onItemClick = { user.id?.let { navController.navigate("profile/$it") } }
                    )
                }
            }

            // Paging Load States
            when (followingItems.loadState.append) {
                is LoadState.Loading -> {
                    item { LoadingIndicator() }
                }
                is LoadState.Error -> {
                    item { ErrorRetry(onRetry = { followingItems.retry() }) }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FollowingHeader(
    count: Int,
    showOnlyActive: Boolean,
    onActiveToggle: (Boolean) -> Unit,
    onSearch: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Following",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count users",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = { /* Open Search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { /* Open Filters */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Strip
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Show only active users",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = showOnlyActive,
                    onCheckedChange = onActiveToggle,
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Active", "Muted", "Verified", "Recent")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
private fun FollowingUserItem(
    user: UserMinimal,
    isFollowing: Boolean,
    isLoading: Boolean,
    onFollowToggle: () -> Unit,
    onMessageClick: () -> Unit,
    onMoreClick: () -> Unit,
    onItemClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Active Indicator
            Box {
                UserAvatar(
                    username = user.username,
                    profilePictureUrl = user.profilePictureUrl,
                    size = 54.dp,
                    shape = CircleShape
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (user.isFollowing == true) "Follows you" else "Last active 2h ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                DynamicIslandFollowButton(
                    isFollowing = isFollowing,
                    isLoading = isLoading,
                    onClick = onFollowToggle,
                    height = 32.dp,
                    modifier = Modifier.widthIn(min = 85.dp)
                )

                IconButton(onClick = onMessageClick) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Message",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    FollowingMoreMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onViewProfile = onItemClick,
                        onMute = { /* TODO */ },
                        onRemoveFollow = onFollowToggle,
                        onBlock = { /* TODO */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowingMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onMute: () -> Unit,
    onRemoveFollow: () -> Unit,
    onBlock: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DropdownMenuItem(
            text = { Text("View Profile") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            onClick = {
                onViewProfile()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Mute") },
            leadingIcon = { Icon(Icons.Default.VolumeOff, null) },
            onClick = {
                onMute()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Remove Follow") },
            leadingIcon = { Icon(Icons.Default.PersonRemove, null) },
            onClick = {
                onRemoveFollow()
                onDismiss()
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        DropdownMenuItem(
            text = { Text("Block", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onBlock()
                onDismiss()
            }
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp))
    }
}

@Composable
private fun ErrorRetry(onRetry: () -> Unit) {
    Button(onClick = onRetry, Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Retry Loading More")
    }
}

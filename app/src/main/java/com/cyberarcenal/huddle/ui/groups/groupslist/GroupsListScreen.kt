package com.cyberarcenal.huddle.ui.groups.groupslist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.Group
import com.cyberarcenal.huddle.data.repositories.groups.GroupsRepository
import com.cyberarcenal.huddle.ui.theme.Gradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListScreen(
    navController: NavController,
    viewModel: GroupsListViewModel = viewModel(
        factory = GroupsListViewModelFactory(GroupsRepository())
    )
) {
    val query by viewModel.searchQuery.collectAsState()
    val privacyFilter by viewModel.privacyFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val groups = viewModel.groupsFlow.collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search groups...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            // Privacy filter chips
            FilterChipsRow(
                selectedFilter = privacyFilter,
                onFilterSelected = viewModel::setPrivacyFilter
            )

            // Error display
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Groups list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    count = groups.itemCount,
                    key = { index -> groups[index]?.id ?: index }
                ) { index ->
                    val group = groups[index]
                    group?.let {
                        GroupItem(
                            group = it,
                            onClick = { viewModel.navigateToGroupDetail(navController, it.id) }
                        )
                    }
                }

                // Loading indicators
                groups.apply {
                    when (loadState.refresh) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is LoadState.Error -> {
                            val error = (loadState.refresh as LoadState.Error).error
                            item {
                                Text(
                                    text = "Error: ${error.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    when (loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LinearProgressIndicator()
                                }
                            }
                        }
                        is LoadState.Error -> {
                            val error = (loadState.append as LoadState.Error).error
                            item {
                                Text(
                                    text = "Error loading more: ${error.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }

                // Empty state
                if (groups.itemCount == 0 && groups.loadState.refresh is LoadState.NotLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No groups found")
                        }
                    }
                }
            }

            // Pull‑to‑refresh handled by LazyList? We can add SwipeRefresh if needed, but for now rely on top refresh button.
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: PrivacyFilter,
    onFilterSelected: (String?) -> Unit
) {
    val filters = listOf(
        "All" to null,
        "Public" to "public",
        "Private" to "private",
        "Secret" to "secret"
    )

    ScrollableTabRow(
        selectedTabIndex = filters.indexOfFirst { it.second == (selectedFilter as? PrivacyFilter.Specific)?.privacy }
            .coerceAtLeast(0),
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        edgePadding = 16.dp,
        indicator = {}
    ) {
        filters.forEach { (label, value) ->
            val selected = if (value == null) selectedFilter is PrivacyFilter.All
            else selectedFilter is PrivacyFilter.Specific && selectedFilter.privacy == value

            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(value) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun GroupItem(
    group: Group,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group.profilePicture?.toString() ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = @androidx.compose.runtime.Composable {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(12.dp)
                    )
                } as Painter?
            )
        },
        headlineContent = {
            Text(
                text = group.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrivacyIcon(privacy = group.privacy?.value)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${group.memberCount} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        trailingContent = {
            // Join/View button – can be added later
            Button(
                onClick = { /* join logic */ },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                enabled = !group.isMember?.toBooleanStrictOrNull()!! ?: true
            ) {
                Text(if (group.isMember == "true") "View" else "Join", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun PrivacyIcon(privacy: String?) {
    val (icon, description) = when (privacy) {
        "public" -> Icons.Outlined.Public to "Public"
        "private" -> Icons.Outlined.Lock to "Private"
        "secret" -> Icons.Outlined.VisibilityOff to "Secret"
        else -> Icons.Outlined.Public to "Unknown"
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = Modifier.size(16.dp),
        tint = Color.Gray
    )
}

// Factory for ViewModel
class GroupsListViewModelFactory(
    private val groupsRepository: GroupsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupsListViewModel(groupsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
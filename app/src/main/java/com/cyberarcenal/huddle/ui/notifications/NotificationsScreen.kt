package com.cyberarcenal.huddle.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.Notification
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications = viewModel.notificationsFlow.collectAsLazyPagingItems()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val markAllResult by viewModel.markAllResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle mark all result
    LaunchedEffect(markAllResult) {
        when (markAllResult) {
            is MarkAllResult.Success -> {
                snackbarHostState.showSnackbar("All notifications marked as read")
                viewModel.clearMarkAllResult()
            }
            is MarkAllResult.Error -> {
                snackbarHostState.showSnackbar((markAllResult as MarkAllResult.Error).message)
                viewModel.clearMarkAllResult()
            }
            else -> {}
        }
    }

    // Swipe refresh state
    val isRefreshing = notifications.loadState.refresh is LoadState.Loading
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    // Mark all as read button (only show if there are unread)
                    if (unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllNotificationsRead() }) {
                            Icon(
                                Icons.Default.MarkEmailRead,
                                contentDescription = "Mark all as read"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { notifications.refresh() }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Unread count header (optional)
                    if (unreadCount > 0) {
                        item {
                            Text(
                                text = "You have $unreadCount unread notification${if (unreadCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Notifications list
                    items(
                        count = notifications.itemCount,
                        key = { index -> notifications[index]?.id ?: index }
                    ) { index ->
                        val notification = notifications[index]
                        notification?.let {
                            NotificationItem(
                                notification = it,
                                onMarkRead = { viewModel.markNotificationRead(it.id) }
                            )
                        }
                    }

                    // Loading more indicator
                    if (notifications.loadState.append is LoadState.Loading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }

                    // Error loading more
                    val appendError = notifications.loadState.append as? LoadState.Error
                    if (appendError != null) {
                        item {
                            Text(
                                text = "Error loading more: ${appendError.error.message}",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Empty state
                    if (notifications.itemCount == 0 && notifications.loadState.refresh is LoadState.NotLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No notifications")
                            }
                        }
                    }
                }
            }

            // Full‑screen loading for first page
            when (notifications.loadState.refresh) {
                is LoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LoadState.Error -> {
                    val error = (notifications.loadState.refresh as LoadState.Error).error
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${error.message}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { notifications.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead == true)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.message ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatRelativeTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (notification.isRead == false) {
                IconButton(onClick = onMarkRead) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Mark as read",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(dateTime: OffsetDateTime): String {
    val now = OffsetDateTime.now(ZoneId.systemDefault())
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    val hours = ChronoUnit.HOURS.between(dateTime, now)
    val days = ChronoUnit.DAYS.between(dateTime, now)

    return when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "now"
    }
}
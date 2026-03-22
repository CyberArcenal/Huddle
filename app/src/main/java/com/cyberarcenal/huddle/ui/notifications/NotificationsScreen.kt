package com.cyberarcenal.huddle.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.Notification
import java.time.OffsetDateTime
import java.time.ZoneId
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

    // Handle mark all result
    LaunchedEffect(markAllResult) {
        when (markAllResult) {
            is MarkAllResult.Success -> {
                snackbarHostState.showSnackbar("All notifications marked as read")
                notifications.refresh()
                viewModel.clearMarkAllResult()
            }
            is MarkAllResult.Error -> {
                snackbarHostState.showSnackbar((markAllResult as MarkAllResult.Error).message)
                viewModel.clearMarkAllResult()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllNotificationsRead() }) {
                            Icon(
                                Icons.Default.MarkEmailRead,
                                contentDescription = "Mark all as read",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Notifications list
                items(
                    count = notifications.itemCount,
                    key = { index -> 
                        val notification = notifications[index]
                        if (notification != null) "notification_${notification.id}" else "notification_placeholder_$index"
                    }
                ) { index ->
                    val notification = notifications[index]
                    notification?.let {
                        NotificationItem(
                            notification = it,
                            onClick = {
                                if (it.isRead == false) {
                                    viewModel.markNotificationRead(it.id)
                                }
                                // Navigate based on notification type if available
                            }
                        )
                    }
                }

                // Loading more indicator
                if (notifications.loadState.append is LoadState.Loading) {
                    item(key = "notifications_append_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Empty state
                if (notifications.itemCount == 0 && notifications.loadState.refresh is LoadState.NotLoading) {
                    item(key = "notifications_empty") {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No notifications yet", color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Full‑screen loading for first page
            if (notifications.loadState.refresh is LoadState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            
            // Initial load error
            if (notifications.loadState.refresh is LoadState.Error) {
                val error = (notifications.loadState.refresh as LoadState.Error).error
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${error.message}", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { notifications.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val isUnread = notification.isRead == false
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.White)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification Icon / Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isUnread) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.Black,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatRelativeTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.2f),
            modifier = Modifier.padding(start = 70.dp)
        )
    }
}

private fun formatRelativeTime(dateTime: OffsetDateTime?): String {
    val now = OffsetDateTime.now(ZoneId.systemDefault())
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    val hours = ChronoUnit.HOURS.between(dateTime, now)
    val days = ChronoUnit.DAYS.between(dateTime, now)

    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

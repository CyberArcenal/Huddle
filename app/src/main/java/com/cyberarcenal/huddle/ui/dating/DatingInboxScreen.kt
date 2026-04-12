package com.cyberarcenal.huddle.ui.dating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatingInboxScreen(
    viewModel: DatingViewModel,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val inboxState by viewModel.inboxState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    fun refresh() {
        if (isRefreshing) return
        coroutineScope.launch {
            isRefreshing = true
            viewModel.loadInbox()
            // Give some time for the API call to complete
            delay(500)
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = inboxState) {
            is DatingUiState.Loading -> {
                if (!isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            is DatingUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${state.message}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadInbox() }) { Text("Retry") }
                }
            }
            is DatingUiState.Success -> {
                @Suppress("UNCHECKED_CAST")
                val conversations = state.data as List<ConversationPartner>

                // PullToRefreshBox wrapper
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { refresh() },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (conversations.isEmpty()) {
                        EmptyInboxContent()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(conversations, key = { it.userId }) { conv ->
                                InboxItem(
                                    conv = conv,
                                    onClick = {
                                        navController.navigate("dating_conversation/${conv.userId}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyInboxContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When you match with someone, you'll see your conversations here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InboxItem(conv: ConversationPartner, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            AsyncImage(
                model = conv.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                placeholder = painterResource(id = R.drawable.profile),
                error = painterResource(id = R.drawable.profile)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Message content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conv.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = conv.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Relative timestamp
                Text(
                    text = DateUtils.toRelativeTime(conv.lastMessageTime?.let { OffsetDateTime.parse(it) }),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Unread badge
            if (conv.unreadCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(conv.unreadCount.toString())
                }
            }
        }
    }
}

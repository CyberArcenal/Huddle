package com.cyberarcenal.huddle.ui.dating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.DatingMessageDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatingSentScreen(
    viewModel: DatingViewModel,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val sentState by viewModel.sentState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    fun refresh() {
        if (isRefreshing) return
        coroutineScope.launch {
            isRefreshing = true
            viewModel.loadSentMessages()
            delay(500)
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = sentState) {
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
                    Button(onClick = { viewModel.loadSentMessages() }) { Text("Retry") }
                }
            }
            is DatingUiState.Success -> {
                @Suppress("UNCHECKED_CAST")
                val messages = state.data as List<DatingMessageDetail>

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { refresh() },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (messages.isEmpty()) {
                        EmptySentContent()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages, key = { it.id ?: it.hashCode() }) { msg ->
                                SentMessageItem(
                                    msg = msg,
                                    onClick = {
                                        // Navigate to conversation with the receiver
                                        msg.receiver?.id?.let { receiverId ->
                                            navController.navigate("dating_conversation/$receiverId")
                                        }
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
fun EmptySentContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No sent messages",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Messages you send will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SentMessageItem(msg: DatingMessageDetail, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Receiver name with indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "To: ${msg.receiver?.username ?: "Unknown"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                // Read status indicator
                if (msg.isRead == true) {
                    Text(
                        text = "Read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                } else if (msg.isRead == false) {
                    Text(
                        text = "Delivered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = msg.content ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Relative timestamp
            Text(
                text = DateUtils.toRelativeTime(msg.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
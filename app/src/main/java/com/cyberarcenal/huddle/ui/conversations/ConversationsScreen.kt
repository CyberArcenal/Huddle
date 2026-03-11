package com.cyberarcenal.huddle.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.Conversation
import com.cyberarcenal.huddle.data.repositories.messaging.MessagingRepository
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navController: NavController,
    viewModel: ConversationsViewModel = viewModel(
        factory = ConversationsViewModelFactory(MessagingRepository())
    )
) {
    val conversations = viewModel.conversationsFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                actions = {
                    IconButton(onClick = { /* new conversation */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "New message")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* new conversation */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            items(
                count = conversations.itemCount,
                key = { index -> conversations[index]?.id ?: index }
            ) { index ->
                val conversation = conversations[index]
                conversation?.let {
                    ConversationItem(
                        conversation = it,
                        onClick = { navController.navigate("chat/${it.id}") }
                    )
                }
            }

            // Loading states
            conversations.apply {
                when (loadState.refresh) {
                    is LoadState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        val error = (loadState.refresh as LoadState.Error).error
                        item {
                            Text(
                                "Error: ${error.message}",
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
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                LinearProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        val error = (loadState.append as LoadState.Error).error
                        item {
                            Text(
                                "Error loading more: ${error.message}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val otherParticipants = conversation.participantsDetails
        ?.filter { it.id != 0 } // TODO: replace with current user ID
        ?.take(3)
        ?.joinToString { it.username.toString() } ?: "Unknown"

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.name?.take(1)?.uppercase()
                        ?: otherParticipants.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        headlineContent = {
            Text(
                text = conversation.name ?: otherParticipants,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            // FIXED: lastMessage is a String, not a Message object
            Text(
                text = conversation.lastMessage ?: "No messages yet",
                maxLines = 1,
                color = Color.Gray
            )
        },
        trailingContent = {
            Text(
                text = formatTime(conversation.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    )
}

private fun formatTime(dateTime: OffsetDateTime?): String {
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

// Factory
class ConversationsViewModelFactory(
    private val messagingRepository: MessagingRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationsViewModel(messagingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

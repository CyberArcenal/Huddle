package com.cyberarcenal.huddle.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.FriendshipMinimal
import com.cyberarcenal.huddle.ui.common.user.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsTab(
    state: FriendshipUiState.Success,
    viewModel: FriendshipViewModel,
    navController: NavController
) {
    val requestType by viewModel.requestType.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Segmented Control: Incoming | Outgoing
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SegmentedButton(
                selected = requestType == "incoming",
                onClick = { viewModel.setRequestType("incoming") },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Incoming")
            }
            SegmentedButton(
                selected = requestType == "outgoing",
                onClick = { viewModel.setRequestType("outgoing") },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Outgoing")
            }
        }

        val displayRequests = if (requestType == "incoming") state.incomingRequests else state.outgoingRequests

        if (displayRequests.isEmpty()) {
            EmptyRequestsState(requestType)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayRequests, key = { it.id ?: it.hashCode() }) { request ->
                    RequestListItem(
                        request = request,
                        isIncoming = requestType == "incoming",
                        onAccept = { request.id?.let { viewModel.friendshipManager.acceptRequest(it) } },
                        onDecline = { request.id?.let { viewModel.friendshipManager.declineRequest(it) } },
                        onCancel = { request.friend?.id?.let { viewModel.friendshipManager.removeFriend(it) } },
                        onViewProfile = { request.friend?.id?.let { navController.navigate("profile/$it") } }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestListItem(
    request: FriendshipMinimal,
    isIncoming: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onViewProfile() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                username = request.friend?.username,
                profilePictureUrl = request.friend?.profilePictureUrl,
                size = 56.dp,
                shape = CircleShape
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.friend?.fullName ?: request.friend?.username ?: "User",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isIncoming) "Sent you a request" else "Waiting for response",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isIncoming) {
                Row {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsState(type: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (type == "incoming") Icons.Default.PersonAdd else Icons.Default.Outbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (type == "incoming") "No pending requests" else "No outgoing requests",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

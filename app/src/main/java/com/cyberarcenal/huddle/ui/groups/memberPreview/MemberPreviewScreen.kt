package com.cyberarcenal.huddle.ui.groups.memberPreview

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.GroupMemberMinimal
import com.cyberarcenal.huddle.api.models.RoleEnum
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPreviewScreen(
    groupId: Int,
    groupName: String?,
    memberCount: Int?,
    navController: NavController,
    viewModel: MemberPreviewViewModel = viewModel(
        factory = MemberPreviewViewModelFactory(
            groupId = groupId,
            groupRepository = GroupRepository(),
            followRepository = FollowRepository()
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val members = viewModel.membersPagingFlow.collectAsLazyPagingItems()
    val actionState by viewModel.actionState.collectAsState()
    val followStatuses by viewModel.followStatuses.collectAsState()

    // Show snackbar on action state changes
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> snackbarHostState.showSnackbar((actionState as ActionState.Success).message)
            is ActionState.Error -> snackbarHostState.showSnackbar((actionState as ActionState.Error).message)
            else -> {}
        }
        viewModel.resetActionState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members Preview") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Navigate back to group detail with members tab selected
                            navController.popBackStack()
                            // Optionally: use a shared view model to set selected tab
                        }
                    ) {
                        Text("View All")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                members.loadState.refresh is LoadState.Loading && members.itemCount == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                members.loadState.refresh is LoadState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed to load members", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { members.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                members.itemCount == 0 && members.loadState.refresh is LoadState.NotLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("No members found")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header with member count
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Members",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = memberCount?.let { "$it members" } ?: "Members",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(members.itemCount) { index ->
                            val member = members[index]
                            member?.let {
                                MemberPreviewCard(
                                    member = it,
                                    isFollowing = followStatuses[it.user?.id] ?: (it.user?.isFollowing ?: false),
                                    onFollowToggle = { userId, currentIsFollowing ->
                                        coroutineScope.launch {
                                            viewModel.toggleFollow(
                                                userId = userId,
                                                currentIsFollowing = currentIsFollowing,
                                                username = it.user?.username ?: "user"
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        if (members.loadState.append is LoadState.Loading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        if (members.loadState.append is LoadState.Error) {
                            item {
                                Text(
                                    text = "Failed to load more members",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
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
private fun MemberPreviewCard(
    member: GroupMemberMinimal,
    isFollowing: Boolean,
    onFollowToggle: (userId: Int, currentIsFollowing: Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            AsyncImage(
                model = member.user?.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.profile)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.user?.fullName ?: member.user?.username ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Role badge
                    when (member.role) {
                        RoleEnum.ADMIN -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        RoleEnum.MODERATOR -> {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Moderator",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }

                // Personality type if available
                member.user?.personalityType?.value?.let { personality ->
                    Text(
                        text = personality,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Joined date
                member.joinedAt?.let { joinedAt ->
                    Text(
                        text = "Joined ${DateUtils.formatDateTime(
                            LocalContext.current,
                            joinedAt.toInstant().toEpochMilli(),
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
                        )}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Follow button
            val userId = member.user?.id ?: return@Card
            Button(
                onClick = { onFollowToggle(userId, isFollowing) },
                modifier = Modifier.width(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isFollowing) "Following" else "Follow")
            }
        }
    }
}
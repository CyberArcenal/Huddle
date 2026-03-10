package com.cyberarcenal.huddle.ui.groups.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
import com.cyberarcenal.huddle.api.models.GroupMember
import com.cyberarcenal.huddle.api.models.RoleEnum
import com.cyberarcenal.huddle.data.repositories.groups.GroupsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    groupId: Int,
    viewModel: GroupDetailViewModel = viewModel(
        factory = GroupDetailViewModelFactory(groupId, GroupsRepository())
    )
) {
    val group by viewModel.groupState.collectAsState()
    val groupLoading by viewModel.groupLoading.collectAsState()
    val groupError by viewModel.groupError.collectAsState()
    val isMember by viewModel.isCurrentUserMember.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val isCreator by viewModel.isCreator.collectAsState()
    val actionState by viewModel.memberActionState.collectAsState()

    val members = viewModel.membersFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show action result snackbar
    LaunchedEffect(actionState) {
        when (actionState) {
            is MemberActionState.Success -> {
                snackbarHostState.showSnackbar((actionState as MemberActionState.Success).message)
                viewModel.clearActionState()
            }
            is MemberActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as MemberActionState.Error).message)
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit/delete for creator/admin
                    if (isCreator || userRole == RoleEnum.ADMIN) {
                        IconButton(onClick = { /* navigate to edit group */ }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (group != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isMember) {
                            Button(
                                onClick = { viewModel.leaveGroup() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                enabled = actionState !is MemberActionState.Loading
                            ) {
                                if (actionState is MemberActionState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("Leave Group")
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.joinGroup() },
                                enabled = actionState !is MemberActionState.Loading
                            ) {
                                if (actionState is MemberActionState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("Join Group")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group header
            group?.let { g ->
                item {
                    GroupHeader(
                        group = g,
                        onPrivacyClick = { /* maybe show info */ }
                    )
                }

                // Members header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Members (${g.memberCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCreator || userRole == RoleEnum.ADMIN) {
                            IconButton(onClick = { /* navigate to add member */ }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add member")
                            }
                        }
                    }
                }
            }

            // Members list
            items(
                count = members.itemCount,
                key = { index -> members[index]?.userId ?: index }
            ) { index ->
                val member = members[index]
                member?.let {
                    MemberItem(
                        member = it,
                        isCurrentUserAdmin = userRole == RoleEnum.ADMIN || isCreator,
                        currentUserId = 0, // Need current user ID – pass from somewhere
                        onRoleChange = { newRole ->
                            viewModel.changeMemberRole(it.userId, newRole)
                        },
                        onRemove = {
                            viewModel.removeMember(it.userId)
                        }
                    )
                }
            }

            // Loading states
            members.apply {
                when (loadState.refresh) {
                    is LoadState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        val error = (loadState.refresh as LoadState.Error).error
                        item {
                            Text(
                                text = "Error loading members: ${error.message}",
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
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
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

            // Empty members
            if (group != null && members.itemCount == 0 && members.loadState.refresh is LoadState.NotLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No members yet")
                    }
                }
            }
        }

        // Full‑screen loading/error for group
        if (groupLoading && group == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        groupError?.let { error ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = viewModel::refresh) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(
    group: com.cyberarcenal.huddle.api.models.Group,
    onPrivacyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group.profilePicture?.toString())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = @androidx.compose.runtime.Composable {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(16.dp)
                    )
                } as Painter?
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrivacyIcon(privacy = group.privacy?.value)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group.privacy?.value?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group.description,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun MemberItem(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    currentUserId: Int,
    onRoleChange: (RoleEnum) -> Unit,
    onRemove: () -> Unit
) {
    val isSelf = member.userId == currentUserId
    val canManage = isCurrentUserAdmin && !isSelf

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("") // member doesn't have avatar URL in GroupMember model – we need user avatar, but not available
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = @androidx.compose.runtime.Composable {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(8.dp)
                    )
                } as Painter?
            )
        },
        headlineContent = {
            Text(
                text = member.username,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = member.role?.value?.replaceFirstChar { it.uppercase() } ?: "Member",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        },
        trailingContent = {
            if (canManage) {
                Row {
                    IconButton(onClick = { /* show role options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Manage")
                    }
                    // Dropdown menu for role change would be better, but for simplicity we'll just show an icon.
                    // In a real app, you'd implement a dropdown.
                }
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
class GroupDetailViewModelFactory(
    private val groupId: Int,
    private val groupsRepository: GroupsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(groupId, groupsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
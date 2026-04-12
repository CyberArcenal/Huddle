package com.cyberarcenal.huddle.ui.groups.management

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.groups.management.components.AnalyticsTab
import com.cyberarcenal.huddle.ui.groups.management.components.ContentAndPostsTab
import com.cyberarcenal.huddle.ui.groups.management.components.EventsManagementTab
import com.cyberarcenal.huddle.ui.groups.management.components.GroupInfoTab
import com.cyberarcenal.huddle.ui.groups.management.components.MembersAndRolesTab
import com.cyberarcenal.huddle.ui.groups.management.components.MembershipRequestsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    groupId: Int,
    navController: NavController,
    viewModel: GroupManagementViewModel = viewModel(
        factory = GroupManagementViewModelFactory(
            groupId = groupId,
            groupRepository = GroupRepository(),
            postRepository = UserPostsRepository(),
            eventRepository = EventRepository(),
            followRepository = FollowRepository()
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val group by viewModel.group.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Info", "Requests", "Members", "Posts", "Events", "Analytics"
    )

    // Paging items
    val members = viewModel.membersPagingFlow.collectAsLazyPagingItems()
    val posts = viewModel.postsPagingFlow.collectAsLazyPagingItems()
    val events = viewModel.eventsPagingFlow.collectAsLazyPagingItems()
    val requests = viewModel.joinRequestsFlow.collectAsLazyPagingItems()

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> globalSnackbarHostState.showSnackbar((actionState as ActionState.Success).message)
            is ActionState.Error -> globalSnackbarHostState.showSnackbar((actionState as ActionState.Error).message)
            else -> {}
        }
        viewModel.resetActionState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage ${group?.name ?: "Group"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content
            when (selectedTab) {
                0 -> GroupInfoTab(group = group, viewModel = viewModel)
                1 -> MembershipRequestsTab(requests = requests, viewModel = viewModel)
                2 -> MembersAndRolesTab(members = members, viewModel = viewModel)
                3 -> ContentAndPostsTab(posts = posts, viewModel = viewModel)
                4 -> EventsManagementTab(events = events, viewModel = viewModel)
                5 -> AnalyticsTab(viewModel = viewModel)
            }
        }
    }
}
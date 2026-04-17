package com.cyberarcenal.huddle.ui.groups.groupdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.MediaDetailDialog
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.feed.components.PostOptionsBottomSheet
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.AboutContent
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.ActionRow
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.EmptyState
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.ErrorMessage
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.EventItem
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.GroupHeader
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.LoadingIndicator
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.MemberItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    navController: NavController,
    viewModel: GroupDetailViewModel = viewModel(
        factory = GroupDetailViewModelFactory(
            groupId = groupId,
            groupRepository = GroupRepository(),
            eventRepository = EventRepository(),
            commentRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository()
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val loadingUsers = remember { mutableStateMapOf<Int, Boolean>() }
    var activeMediaDetail by remember { mutableStateOf<MediaDetailData?>(null) }

    val group by viewModel.group.collectAsState()
    val isMember by viewModel.isMember.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    // Paging flows
    val posts = viewModel.postsPagingFlow.collectAsLazyPagingItems()
    val events = viewModel.eventsPagingFlow.collectAsLazyPagingItems()
    val members = viewModel.membersPagingFlow.collectAsLazyPagingItems()

    // Comment and options state from ViewModel managers
    val commentSheetState by viewModel.commentSheetState.collectAsState()
    val optionsSheetState by viewModel.optionsSheetState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val groupMembershipStatuses by viewModel.groupMembershipStatuses.collectAsState()
    val joiningGroupIds by viewModel.joiningGroupIds.collectAsState()

    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "Events", "Members", "About")
    val listState = rememberLazyListState()

    // Pull to refresh
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoading ||
            posts.loadState.refresh is LoadState.Loading ||
            events.loadState.refresh is LoadState.Loading ||
            members.loadState.refresh is LoadState.Loading

    // Snackbar
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> globalSnackbarHostState.showSnackbar((actionState as ActionState.Success).message)
            is ActionState.Error -> globalSnackbarHostState.showSnackbar((actionState as ActionState.Error).message)
            else -> {}
        }
    }

    // Media detail dialog
    activeMediaDetail?.let {
        MediaDetailDialog(
            media = it,
            onDismiss = { activeMediaDetail = null },
            onReactionClick = { data -> viewModel.sendReaction(data) },
            onCommentClick = { contentType, id, stats ->
                activeMediaDetail = null
                viewModel.openCommentSheet(contentType, id, stats)
            },
            onShare = viewModel::sharePost
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Share group */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && isMember) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_post?groupId=$groupId") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
            if (selectedTab == 1 && isMember) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_event?groupId=$groupId") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Event")
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    viewModel.refresh()
                    posts.refresh()
                    events.refresh()
                    members.refresh()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header
                item {
                    GroupHeader(
                        group = group,
                        isMember = isMember,
                        onJoinClick = { viewModel.joinGroup() },
                        onLeaveClick = { viewModel.leaveGroup() },
                        onInviteClick = { viewModel.inviteFriends() }
                    )
                }

                // Action row
                item {
                    ActionRow(
                        isMember = isMember,
                        onCreatePostClick = { navController.navigate("create_post?groupId=$groupId") },
                        onCreateEventClick = { navController.navigate("create_event?groupId=$groupId") },
                        onInviteClick = { viewModel.inviteFriends() }
                    )
                }

                // Sticky tabs
                stickyHeader {
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
                }

                // Tab content
                when (selectedTab) {
                    0 -> {
                        // Posts tab
                        if (posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0) {
                            item { LoadingIndicator() }
                        } else if (posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading) {
                            item { EmptyState("No posts yet. Be the first to share something!") }
                        } else {
                            items(
                                count = posts.itemCount,
                                key = posts.itemKey { item ->
                                    when (item.type) {
                                        UnifiedContentItemTypeEnum.POST -> "post_${(item.item as? PostFeed)?.id ?: item.hashCode()}"
                                        else -> "${item.type}_${item.hashCode()}"
                                    }
                                },
                                contentType = posts.itemContentType { it.type.name }
                            ) { index ->
                                val row = posts[index]
                                row?.let {
                                    UnifiedFeedRow(
                                        row = it,
                                        navController = navController,
                                        onReactionClick = { data -> viewModel.sendReaction(data) },
                                        onCommentClick = { contentType, id, stats ->
                                            viewModel.openCommentSheet(
                                                contentType,
                                                id, stats
                                            )
                                        },
                                        onMoreClick = { data ->
                                            if (data is PostFeed) viewModel.openOptionsSheet(data)
                                        },
                                        onImageClick = { data -> activeMediaDetail = data },
                                        onGroupJoinClick = {},
                                        onFollowClick = { userMinimal ->
                                            userMinimal.id?.let { userId ->
                                                loadingUsers[userId] = true
                                                viewModel.toggleFollow(
                                                    userId = userId,
                                                    currentIsFollowing = userMinimal.isFollowing
                                                        ?: false,
                                                    username = userMinimal.username ?: "user"
                                                )
                                            }
                                        },
                                        onShare = { shareData -> viewModel.sharePost(shareData) },
                                        isPaused = commentSheetState != null,
                                        followStatuses = viewModel.followStatuses.value,
                                        loadingUsers = loadingUsers,
                                        groupMembershipStatuses = groupMembershipStatuses,
                                        joiningGroupIds = joiningGroupIds,
                                        onGroupClick = {navController.navigate("group/${it}")}
                                    )
                                    HorizontalDivider()
                                }
                            }
                            if (posts.loadState.append is LoadState.Loading) {
                                item { LoadingIndicator() }
                            }
                            if (posts.loadState.append is LoadState.Error) {
                                item { ErrorMessage("Failed to load posts", onRetry = { posts.retry() }) }
                            }
                        }
                    }
                    1 -> {
                        // Events tab
                        if (events.loadState.refresh is LoadState.Loading && events.itemCount == 0) {
                            item { LoadingIndicator() }
                        } else if (events.itemCount == 0 && events.loadState.refresh is LoadState.NotLoading) {
                            item { EmptyState("No upcoming events.") }
                        } else {
                            items(events.itemCount) { index ->
                                val event = events[index]
                                event?.let {
                                    if (it.id !==null){
                                        EventItem(
                                            event = it,
                                            onEventClick = { navController.navigate("event_detail/${it.id}") },
                                            onRsvpClick = { viewModel.rsvpToEvent(it.id) }
                                        )
                                    }

                                }
                                HorizontalDivider()
                            }
                            if (events.loadState.append is LoadState.Loading) {
                                item { LoadingIndicator() }
                            }
                            if (events.loadState.append is LoadState.Error) {
                                item { ErrorMessage("Failed to load events", onRetry = { events.retry() }) }
                            }
                        }
                    }
                    2 -> {
                        // Members tab
                        // Inside the Members tab (selectedTab == 2) in GroupDetailScreen.kt
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = {
                                    navController.navigate("member_preview/$groupId?name=${group?.name}&count=${group?.memberCount}")
                                }) {
                                    Text("View All")
                                }
                            }
                        }
                        if (members.loadState.refresh is LoadState.Loading && members.itemCount == 0) {
                            item { LoadingIndicator() }
                        } else if (members.itemCount == 0 && members.loadState.refresh is LoadState.NotLoading) {
                            item { EmptyState("No members found.") }
                        } else {
                            items(members.itemCount) { index ->
                                val member = members[index]
                                member?.let {
                                    MemberItem(
                                        member = it,
                                        isCurrentUserAdmin = group?.memberRole == "admin",
                                        onRoleChange = { newRole ->
                                            viewModel.changeMemberRole(member.user?.id ?: return@MemberItem, newRole)
                                        },
                                        onRemoveMember = { viewModel.removeMember(member.user?.id ?: return@MemberItem) }
                                    )
                                }
                            }
                            if (members.loadState.append is LoadState.Loading) {
                                item { LoadingIndicator() }
                            }
                            if (members.loadState.append is LoadState.Error) {
                                item { ErrorMessage("Failed to load members", onRetry = { members.retry() }) }
                            }
                        }
                    }
                    3 -> {
                        // About tab
                        item {
                            AboutContent(group = group)
                        }
                    }
                }
            }
        }
    }

    // Bottom sheets
    if (commentSheetState != null) {
        CommentBottomSheet(
            comments = comments,
            replies = replies,
            expandedReplies = expandedReplies,
            currentUserId = null, // You can get from TokenManager if needed
            isLoadingMore = isLoadingMore,
            onLoadMore = viewModel::loadMoreComments,
            onToggleReplyExpanded = viewModel::toggleReplyExpansion,
            onLoadReplies = viewModel::loadReplies,
            onReactToComment = { id, reactionType ->
                viewModel.sendReaction(ReactionCreateRequest("comment", id, reactionType))
            },
            onReplyToComment = viewModel::addReply,
            onReportComment = { _ -> },
            onDismiss = viewModel::dismissCommentSheet,
            onSendComment = viewModel::addComment,
            onDeleteComment = viewModel::deleteComment,
            actionState = actionState,
            errorMessage = commentsError
        )
    }

    if (optionsSheetState != null) {
        PostOptionsBottomSheet(
            post = optionsSheetState!!.post,
            isCurrentUser = optionsSheetState!!.post.user?.id == null, // Use actual current user id if available
            onDismiss = viewModel::dismissOptionsSheet,
            onDelete = { /* TODO: implement delete */ },
            onReport = { _, _ -> }
        )
    }
}
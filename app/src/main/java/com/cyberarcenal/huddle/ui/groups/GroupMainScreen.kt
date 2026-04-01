package com.cyberarcenal.huddle.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.MediaDetailDialog
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.feed.components.PostOptionsBottomSheet
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.EmptyState
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.LoadingIndicator
import com.cyberarcenal.huddle.ui.home.components.CreatePostRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMainScreen(
    navController: NavController,
    viewModel: GroupMainViewModel = viewModel(
        factory = GroupMainViewModelFactory(
            groupRepository = GroupRepository(),
            commentRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository(),
            userMediaRepository = UserMediaRepository(),
            postRepository = UserPostsRepository(),
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val groups by viewModel.groups.collectAsState()
    val discoveryGroups by viewModel.discoveryGroups.collectAsState()
    val isDiscoveryLoading by viewModel.isDiscoveryLoading.collectAsState()
    val isLoadingGroups by viewModel.isLoadingGroups.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val followStatuses by viewModel.followStatuses.collectAsState()
    val loadingUsers by viewModel.loadingUserIds.collectAsState()

    val selectedGroup = groups.find { it.id == selectedGroupId }
    val isMemberOfSelectedGroup = selectedGroup?.isMember ?: false

    val feedItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = feedItems.loadState.refresh is LoadState.Loading || isLoadingGroups

    val pagerState = rememberPagerState(pageCount = { 2 })

    // Snackbar for actions
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> snackbarHostState.showSnackbar((actionState as ActionState.Success).message)
            is ActionState.Error -> snackbarHostState.showSnackbar((actionState as ActionState.Error).message)
            else -> {}
        }
    }

    // Comment and options states
    val commentSheetState by viewModel.commentSheetState.collectAsState()
    val optionsSheetState by viewModel.optionsSheetState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val groupMembershipStatuses by viewModel.groupMembershipStatuses.collectAsState()
    val joiningGroupIds by viewModel.joiningGroupIds.collectAsState()

    var activeMediaDetail by remember { mutableStateOf<MediaDetailData?>(null) }


    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.id

    LaunchedEffect(Unit) {
        val storedUser = TokenManager.getUser(context)
        viewModel.setCurrentUserId(storedUser?.id)
        viewModel.setCurrentUserData(storedUser)
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
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Groups", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("create_group") }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Feed") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Discovery") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0 && selectedGroupId != null && isMemberOfSelectedGroup) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_post?groupId=$selectedGroupId") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                ),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> {
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                feedItems.refresh()
                                viewModel.refresh()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Group filter bar
                            item {
                                GroupFilterBar(
                                    groups = groups,
                                    selectedGroupId = selectedGroupId,
                                    isLoading = isLoadingGroups,
                                    onGroupSelected = { groupId -> viewModel.selectGroup(groupId) },
                                    modifier = Modifier.fillMaxWidth().background(Color.White)
                                )
                            }

                            // Post composer (only for specific group and member)
                            if (selectedGroupId != null && isMemberOfSelectedGroup) {
                                item {
                                    CreatePostRow(
                                        profilePictureUrl = null,
                                        onRowClick = { navController.navigate("create_post?groupId=$selectedGroupId") }
                                    )
                                }
                            }

                            // Feed content
                            if (feedItems.loadState.refresh is LoadState.Loading && feedItems.itemCount == 0) {
                                item { LoadingIndicator() }
                            } else if (feedItems.itemCount == 0 && feedItems.loadState.refresh is LoadState.NotLoading) {
                                item {
                                    EmptyState(
                                        message = when (selectedGroupId) {
                                            null -> "No posts from your groups yet."
                                            else -> "No posts in this group yet."
                                        }
                                    )
                                }
                            } else {
                                items(
                                    count = feedItems.itemCount,
                                    key = feedItems.itemKey { row ->
                                        if (row == null) {
                                            "null_row_${System.identityHashCode(row)}"
                                        } else {
                                            val type = row.type
                                            val typeName = type?.name ?: "null"
                                            when {
                                                row.item != null -> "item_${typeName}_${System.identityHashCode(row.item)}"
                                                row.items != null -> "items_${typeName}_${System.identityHashCode(row.items)}"
                                                else -> "empty_${typeName}_${System.identityHashCode(row)}"
                                            }
                                        }
                                    },
                                    contentType = feedItems.itemContentType { it.type?.name ?: "OTHER" }
                                )  { index ->
                                    val row = feedItems[index]
                                    row?.let {
                                        UnifiedFeedRow(
                                            row = it,
                                            navController = navController,
                                            onReactionClick = { data -> viewModel.sendReaction(data) },
                                            onCommentClick = { contentType, id, stats ->
                                                viewModel.openCommentSheet(contentType, id, stats)
                                            },
                                            onMoreClick = { data ->
                                                if (data is PostFeed) viewModel.openOptionsSheet(
                                                    data
                                                )
                                            },
                                            onImageClick = { data -> activeMediaDetail = data },
                                            onGroupJoinClick = {},
                                            onFollowClick = { userMinimal ->
                                                userMinimal.id?.let { userId ->
                                                    viewModel.toggleFollow(
                                                        userId = userId,
                                                        currentIsFollowing = userMinimal.isFollowing
                                                            ?: false,
                                                        username = userMinimal.username ?: "user"
                                                    )
                                                }
                                            },
                                            onShare = { shareData -> viewModel.sharePost(shareData) },
                                            followStatuses = followStatuses,
                                            loadingUsers = loadingUsers,
                                            groupMembershipStatuses = groupMembershipStatuses,
                                            joiningGroupIds = joiningGroupIds,
                                        )
                                    }
                                }

                                if (feedItems.loadState.append is LoadState.Loading) {
                                    item { LoadingIndicator() }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    DiscoveryTab(
                        groups = discoveryGroups,
                        isLoading = isDiscoveryLoading,
                        onJoinClick = { viewModel.joinGroup(it) },
                        onGroupClick = { navController.navigate("group_detail/$it") }
                    )
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
            currentUserId = currentUserId,
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
            isCurrentUser = optionsSheetState!!.post.user?.id == currentUserId,
            onDismiss = viewModel::dismissOptionsSheet,
            onDelete = { /* TODO: implement delete */ },
            onReport = { _, _ -> }
        )
    }
}

@Composable
fun DiscoveryTab(
    groups: List<GroupMinimal>,
    isLoading: Boolean,
    onJoinClick: (Int) -> Unit,
    onGroupClick: (Int) -> Unit
) {
    if (isLoading && groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No groups discovered yet.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Discover New Groups",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
            }
            items(groups) { group ->
                DiscoveryGroupCard(
                    group = group,
                    onJoinClick = { group.id?.let { onJoinClick(it) } },
                    onGroupClick = { group.id?.let { onGroupClick(it) } }
                )
            }
        }
    }
}

@Composable
fun DiscoveryGroupCard(
    group: GroupMinimal,
    onJoinClick: () -> Unit,
    onGroupClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGroupClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = group.profilePicture?.toString(),
                contentDescription = group.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.group),
                error = painterResource(R.drawable.group)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = group.name ?: "Unknown Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = group.shortDescription ?: "No description available",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            if (group.isMember == true) {
                Text(
                    "Joined",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Button(
                    onClick = onJoinClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("Join")
                }
            }
        }
    }
}

@Composable
fun GroupFilterBar(
    groups: List<GroupMinimal>,
    selectedGroupId: Int?,
    isLoading: Boolean,
    onGroupSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isLoading && groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You are not a member of any group yet.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(
                modifier = modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    GroupFilterChip(
                        name = "All",
                        imageUrl = null,
                        isSelected = selectedGroupId == null,
                        onClick = { onGroupSelected(null) }
                    )
                }
                items(groups) { group ->
                    GroupFilterChip(
                        name = group.name ?: "",
                        imageUrl = group.profilePicture?.toString(),
                        isSelected = selectedGroupId == group.id,
                        onClick = { onGroupSelected(group.id) }
                    )
                }
            }
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@Composable
fun GroupFilterChip(
    name: String,
    imageUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .padding(if (isSelected) 3.dp else 0.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.group),
                    error = painterResource(R.drawable.group)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (name == "All") "All" else name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
        )
    }
}

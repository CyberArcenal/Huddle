package com.cyberarcenal.huddle.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
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
import com.cyberarcenal.huddle.ui.feed.safeConvertReactionTypeToRequest
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.EmptyState
import com.cyberarcenal.huddle.ui.groups.groupdetail.components.ErrorMessage
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
            reactionsRepository = UserReactionsRepository(),
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

    val listState = rememberLazyListState()

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
            onCommentClick = { contentType, id ->
                activeMediaDetail = null
                viewModel.openCommentSheet(contentType, id)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (selectedGroupId != null && isMemberOfSelectedGroup) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_post?groupId=$selectedGroupId") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        }
    ) { paddingValues ->
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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Group filter bar
                item {
                    GroupFilterBar(
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        isLoading = isLoadingGroups,
                        onGroupSelected = { groupId -> viewModel.selectGroup(groupId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Post composer (only for specific group and member)
                if (selectedGroupId != null && isMemberOfSelectedGroup) {
                    item {
                        CreatePostRow(
                            profilePictureUrl = null, // optional: can fetch current user's profile picture
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
                            when (row.type) {
                                UnifiedContentItemTypeEnum.POST -> "post_${(row.item as? PostFeed)?.id ?: row.hashCode()}"
                                else -> "${row.type}_${row.hashCode()}"
                            }
                        },
                        contentType = feedItems.itemContentType { it.type.name }
                    ) { index ->
                        val row = feedItems[index]
                        row?.let {
                            UnifiedFeedRow(
                                row = it,
                                navController = navController,
                                onReactionClick = { data -> viewModel.sendReaction(data) },
                                onCommentClick = { contentType, id ->
                                    viewModel.openCommentSheet(contentType, id)
                                },
                                onMoreClick = { data ->
                                    if (data is PostFeed) viewModel.openOptionsSheet(data)
                                },
                                onImageClick = { data -> activeMediaDetail = data },
                                onGroupJoinClick = {},
                                onFollowClick = { userMinimal ->
                                    userMinimal.id?.let { userId ->
                                        viewModel.toggleFollow(
                                            userId = userId,
                                            currentIsFollowing = userMinimal.isFollowing ?: false,
                                            username = userMinimal.username ?: "user"
                                        )
                                    }
                                },
                                onShare = { shareData -> viewModel.sharePost(shareData) },
                                followStatuses = followStatuses,
                                loadingUsers = loadingUsers
                            )
                        }
                    }

                    if (feedItems.loadState.append is LoadState.Loading) {
                        item { LoadingIndicator() }
                    }
                    if (feedItems.loadState.append is LoadState.Error) {
                        item {
                            ErrorMessage(
                                message = "Failed to load more posts",
                                onRetry = { feedItems.retry() }
                            )
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
                Text("You are not a member of any group yet.")
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
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = CircleShape
                )
                .clip(CircleShape)
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
        Text(
            text = name,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(70.dp)
        )
    }
}
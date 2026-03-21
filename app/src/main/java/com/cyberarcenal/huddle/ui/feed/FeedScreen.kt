package com.cyberarcenal.huddle.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.RowTypeEnum
import com.cyberarcenal.huddle.api.models.ShareFeed
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.data.repositories.FeedRepository
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import com.cyberarcenal.huddle.data.repositories.UserReactionsRepository
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.EventsRow
import com.cyberarcenal.huddle.ui.common.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.PostItem
import com.cyberarcenal.huddle.ui.common.ShareItem
import com.cyberarcenal.huddle.ui.feed.components.*
import com.cyberarcenal.huddle.ui.home.components.CreatePostRow
import com.cyberarcenal.huddle.ui.profile.components.FullscreenImageDialog
import com.cyberarcenal.huddle.ui.storyviewer.StoriesRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    feedType: FeedType,
    viewModel: FeedViewModel = viewModel(
        factory = FeedViewModelFactory(
            feedType = feedType,
            postRepository = UserPostsRepository(),
            feedRepository = FeedRepository(),
            commentRepository = CommentsRepository(),
            reactionsRepository = UserReactionsRepository(),
            storyFeedRepository = StoriesRepository()
        )
    )
) {
    val context = LocalContext.current
    val feedItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentUserId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        viewModel.setCurrentUserId(currentUserId)
    }

    val stories by viewModel.stories.collectAsState()
    val isLoadingStories by viewModel.storiesLoading.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = feedItems.loadState.refresh is LoadState.Loading || isLoadingStories

    val commentSheetState by viewModel.commentSheetState.collectAsState()
    val optionsSheetState by viewModel.optionsSheetState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> snackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadStories()
    }

    if (selectedImageUrl != null) {
        FullscreenImageDialog(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    feedItems.refresh()
                    viewModel.loadStories()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                item {
                    StoriesRow(
                        stories = stories,
                        onCreateStoryClick = {
                            navController.navigate("create_story")
                        },
                        onStoryClick = { storyFeed ->
                            navController.navigate("story/${storyFeed.user?.id}")
                        }
                    )
                }

                item {
                    CreatePostRow(
                        profilePictureUrl = null,
                        onRowClick = {
                            navController.navigate("create_post")
                        }
                    )
                }

                items(
                    count = feedItems.itemCount,
                    key = { index -> index }, // key per row lang, safe fallback
                    contentType = { index -> feedItems[index]?.rowType?.name ?: "unknown" }
                ) { index ->
                    val row = feedItems[index]
                    row?.let {
                        when (it.rowType) {
                            RowTypeEnum.POSTS -> {
                                it.items?.forEach { post ->
                                    val postFeed = post as PostFeed
                                    key(postFeed.id ?: postFeed.hashCode()) {
                                        FeedItemFrame(
                                            user = postFeed.user,
                                            createdAt = postFeed.createdAt,
                                            statistics = postFeed.statistics,
                                            headerSuffix = "",
                                            onReactionClick = { reactionType ->
                                                viewModel.sendPostReaction(postFeed.id!!, reactionType)
                                            },
                                            onCommentClick = { viewModel.openCommentSheet(postFeed.id) },
                                            onShareClick = { /* handle share */ },
                                            onMoreClick = { viewModel.openOptionsSheet(postFeed) },
                                            onProfileClick = { userId -> navController.navigate("profile/$userId") },
                                            content = {
                                                PostItem(
                                                    post = postFeed,
                                                    onImageClick = { url -> selectedImageUrl = url }
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            RowTypeEnum.SHARES -> {
                                it.items?.forEach { share ->
                                    val shareFeed = share as ShareFeed
                                    key(shareFeed.id ?: shareFeed.hashCode()) {
                                        ShareItem(
                                            share = shareFeed,
                                            onProfileClick = { userId -> navController.navigate("profile/$userId") },
                                            onImageClick = { url -> selectedImageUrl = url }
                                        )
                                    }
                                }
                            }

                            RowTypeEnum.EVENTS -> {
                                EventsRow(
                                    it.title,
                                    it.items as List<EventList>,

                                    onEventClick = { event ->
                                        navController.navigate("event/${event.id}")
                                    },
                                    onShowMoreClick = {
                                        navController.navigate("events")
                                    },
                                )
                            }

                            else -> Unit
                        }
                    }
                }



                if (feedItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (commentSheetState != null) {
        CommentBottomSheet(
            postId = commentSheetState!!.postId,
            comments = comments,
            replies = replies,
            expandedReplies = expandedReplies,
            currentUserId = currentUserId,
            isLoadingMore = isLoadingMore,
            onLoadMore = viewModel::loadMoreComments,
            onToggleReplyExpanded = viewModel::toggleReplyExpansion,
            onLoadReplies = viewModel::loadReplies,
            onReactToComment = { id, reactionType ->
                viewModel.sendPostReaction(id, reactionType, contentType = "feed.comment")
            },
            onReplyToComment = viewModel::addReply,
            onReportComment = { commentId ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Reported comment $commentId (not implemented)")
                }
            },
            onDismiss = { viewModel.dismissCommentSheet() },
            onSendComment = { content -> viewModel.addComment(content) },
            onDeleteComment = { commentId -> viewModel.deleteComment(commentId) },
            actionState = actionState,
            errorMessage = commentsError
        )
    }

    if (optionsSheetState != null) {
        PostOptionsBottomSheet(
            post = optionsSheetState!!.post,
            isCurrentUser = optionsSheetState!!.post.user?.id == currentUserId,
            onDismiss = { viewModel.dismissOptionsSheet() },
            onDelete = { viewModel.deletePost(it) },
            onReport = { postId, reason -> viewModel.reportPost(postId, reason) }
        )
    }
}
package com.cyberarcenal.huddle.ui.feed

import android.util.Log
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
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.*
import com.cyberarcenal.huddle.ui.feed.components.*
import com.cyberarcenal.huddle.ui.home.components.CreatePostRow
import com.cyberarcenal.huddle.ui.profile.components.FullscreenImageDialog
import com.cyberarcenal.huddle.ui.storyviewer.StoriesRow
import com.google.gson.Gson
import kotlinx.coroutines.launch

// Helper to convert a generic Any (usually LinkedTreeMap) to a specific class
inline fun <reified T> safeConvertTo(item: Any, tag: String = "Convert"): T? {
    return try {
        val gson = Gson()
        val json = gson.toJson(item)
        gson.fromJson(json, T::class.java)
    } catch (e: Exception) {
        Log.e(tag, "Failed to convert item to ${T::class.simpleName}: ${e.message}")
        null
    }
}

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
                item(key = "stories_row") {
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

                item(key = "create_post_row") {
                    CreatePostRow(
                        profilePictureUrl = null,
                        onRowClick = {
                            navController.navigate("create_post")
                        }
                    )
                }

                items(
                    count = feedItems.itemCount,
                    key = { index ->
                        val row = feedItems[index]
                        if (row != null) {
                            "feed_row_${row.rowType}_${index}"
                        } else {
                            "feed_row_placeholder_$index"
                        }
                    },
                    contentType = { index -> feedItems[index]?.rowType?.name ?: "unknown" }
                ) { index ->
                    val row = feedItems[index]
                    row?.let {
                        when (it.rowType) {
                            RowTypeEnum.POSTS -> {
                                val posts = it.items?.mapNotNull { postMap ->
                                    safeConvertTo<PostFeed>(postMap, "PostFeed")
                                } ?: emptyList()
                                posts.forEach { postFeed ->
                                    key("feed_post_${postFeed.id}") {
                                        Log.d("FeedScreen", "Rendering post feed: $postFeed")
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
                                val shares = it.items?.mapNotNull { shareMap ->
                                    safeConvertTo<ShareFeed>(shareMap, "ShareFeed")
                                } ?: emptyList()
                                shares.forEach { shareFeed ->
                                    key("feed_share_${shareFeed.id}") {
                                        ShareItem(
                                            share = shareFeed,
                                            onProfileClick = { userId -> navController.navigate("profile/$userId") },
                                            onImageClick = { url -> selectedImageUrl = url }
                                        )
                                    }
                                }
                            }

                            RowTypeEnum.EVENTS -> {
                                val events = it.items?.mapNotNull { item ->
                                    runCatching { safeConvertTo<EventList>(item) }.getOrNull()
                                } ?: emptyList()
                                EventsRow(
                                    title = it.title ?: "",
                                    events = events,
                                    onEventClick = { event ->
                                        navController.navigate("event/${event.id}")
                                    },
                                    onShowMoreClick = {
                                        navController.navigate("events")
                                    }
                                )
                            }

                            RowTypeEnum.RECOMMENDED_GROUPS -> {

                                    val groups = it.items?.mapNotNull { item ->
                                        runCatching { safeConvertTo<RecommendedGroupItem>(item) }
                                            .getOrNull()
                                    } ?: emptyList()
                                    if (groups.isNotEmpty()) {
                                        GroupSuggestionsRow(
                                            title = it.title ?: "Recommended Groups",
                                            groups = groups,
                                            onGroupClick = { group ->
                                                // Navigate to group detail screen
                                                navController.navigate("group/${group.id}")
                                            },
                                            onShowMoreClick = {
                                                navController.navigate("groups")
                                            }
                                        )
                                    } else {
                                        Log.d(
                                            "FeedScreen",
                                            "No groups found in recommended_groups row"
                                        )
                                    }

                            }
                            RowTypeEnum.SUGGESTED_USERS -> {
                                val suggested = it.items?.mapNotNull { item ->
                                    runCatching { safeConvertTo<SuggestedUserItem>(item) }.getOrNull()
                                } ?: emptyList()
                                if (suggested.isNotEmpty()) {
                                    SuggestedUserRow(
                                        title = it.title,
                                        suggested = suggested,
                                        onUserClick = {},
                                        onFollowClick = {},
                                        onShowMoreClick = {}
                                    )
                                } else {
                                    Log.d(
                                        "FeedScreen",
                                        "No suggested user found in row"
                                    )
                                }
                            }
                            RowTypeEnum.MATCH_USERS -> {
                                val match = it.items?.mapNotNull { item ->
                                    runCatching { safeConvertTo<MatchUserItem>(item) }.getOrNull()
                                } ?: emptyList()
                                if (match.isNotEmpty()) {
                                   MatchUserRow(
                                       title = it.title,
                                       match = match,
                                       onUserClick = {}
                                   )
                                } else {
                                    Log.d(
                                        "FeedScreen",
                                        "No match found in row"
                                    )
                                }
                            }
                            RowTypeEnum.REELS -> {
                                val reels = it.items?.mapNotNull { item ->
                                    runCatching { safeConvertTo<ReelDisplay>(item) }.getOrNull()
                                } ?: emptyList()
                                if (reels.isNotEmpty()) {
                                ReelsRow(
                                    reels = reels,
                                    onReelClick = {},
                                    onShowMoreClick = {}
                                )
                                } else {
                                    Log.d(
                                        "FeedScreen",
                                        "No reels found in row"
                                    )
                                }
                            }

                            RowTypeEnum.STORIES -> {
                                val item = it.items?.mapNotNull { item ->
                                    runCatching { safeConvertTo<StoryItem>(item) }.getOrNull()
                                } ?: emptyList()
                                if (item.isNotEmpty()){
                                    FeedStoriesRow(
                                        stories = item,
                                        onCreateStoryClick = {
                                            navController.navigate("create_story")
                                        },
                                        onStoryClick = { storyFeed ->
                                            navController.navigate("story/${storyFeed.user?.id}")
                                        }
                                    )
                                }
                            }


                            else -> Unit
                        }
                    }
                }

                if (feedItems.loadState.append is LoadState.Loading) {
                    item(key = "append_loading_indicator") {
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

    // Bottom sheets remain unchanged
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
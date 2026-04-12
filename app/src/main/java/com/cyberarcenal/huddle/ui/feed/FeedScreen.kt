package com.cyberarcenal.huddle.ui.feed

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.models.StoryFeedCache
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.feed.components.*
import com.cyberarcenal.huddle.ui.profile.components.FullscreenImageDialog
import com.cyberarcenal.huddle.ui.common.feed.MediaDetailDialog
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.post.PostVideoFullscreenPlayer
import com.cyberarcenal.huddle.ui.feed.dataclass.FeedType
import com.cyberarcenal.huddle.ui.home.components.CreatePostRow
import com.cyberarcenal.huddle.ui.storyviewer.StoriesRow
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

inline fun <reified T> safeConvertTo(item: Any, tag: String = "Convert"): T? {
    return try {
        val gson = GsonBuilder().registerTypeAdapter(
            OffsetDateTime::class.java, JsonDeserializer { json, _, _ ->
                OffsetDateTime.parse(json.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }).create()
        val json = gson.toJson(item)
        gson.fromJson(json, T::class.java)
    } catch (e: Exception) {
        Log.e(tag, "Failed to convert item to ${T::class.simpleName}: ${e.message}", e)
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    feedType: FeedType,
    onActive: ((FeedViewModel) -> Unit)? = null,
    viewModel: FeedViewModel = viewModel(
        key = feedType.name, factory = FeedViewModelFactory(
            feedType = feedType,
            postRepository = UserPostsRepository(),
            feedRepository = FeedRepository(LocalContext.current),
            commentRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            storyFeedRepository = StoriesRepository(LocalContext.current),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository(),
            userMediaRepository = UserMediaRepository(),
            groupRepository = GroupRepository(),
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val feedItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.id

    val stories by viewModel.stories.collectAsState()
    val isLoadingStories by viewModel.storiesLoading.collectAsState()

    val followStatuses by viewModel.followStatuses.collectAsState()
    val loadingUserIds by viewModel.loadingUserIds.collectAsState()
    val groupMembershipStatuses by viewModel.groupMembershipStatuses.collectAsState()
    val joiningGroupIds by viewModel.joiningGroupIds.collectAsState()

    val commentSheetState by viewModel.commentSheetState.collectAsState()
    val optionsSheetState by viewModel.optionsSheetState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val comments by viewModel.comments.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var activeMediaDetail by remember { mutableStateOf<MediaDetailData?>(null) }
    var activeVideoPost by remember { mutableStateOf<Pair<PostFeed, String>?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = feedItems.loadState.refresh is LoadState.Loading || isLoadingStories

    // ==================== EFFECTS ====================
    LaunchedEffect(Unit) {
        TokenManager.getUser(context)?.let {
            viewModel.setCurrentUserId(it.id)
            viewModel.setCurrentUserData(it)
        }
        onActive?.invoke(viewModel)
    }

    LaunchedEffect(currentUser?.profilePicture?.imageUrl, currentUserId) {
        if (currentUser?.profilePicture?.imageUrl.isNullOrBlank() && currentUserId != null) {
            viewModel.loadUserImage()
        }
    }

    LaunchedEffect(feedType) {
        viewModel.loadStories()
        viewModel.loadUserImage()
        feedItems.refresh()
    }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> globalSnackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> globalSnackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    // SCROLL TO TOP (naibalik na!)
    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(viewModel.refreshTrigger) {
        viewModel.refreshTrigger.collect { feedItems.refresh() }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        PullToRefreshBox(
            state = pullToRefreshState, isRefreshing = isRefreshing, onRefresh = {
                coroutineScope.launch {
                    feedItems.refresh()
                    viewModel.loadStories()
                    viewModel.loadUserImage()
                }
            }, modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize(), contentPadding = padding
            ) {
                if (feedType == FeedType.HOME) {
                    item(key = "stories_row") {
                        StoriesRow(
                            stories = stories,
                            currentUserProfilePicture = currentUser?.profilePicture?.imageUrl,
                            onCreateStoryClick = { navController.navigate("create_story") },
                            onStoryClick = { _, index ->
                                val sessionId = UUID.randomUUID().toString()
                                StoryFeedCache.store(sessionId, stories)
                                navController.navigate("story_feed_viewer/$index/$sessionId")
                            })

                    }
                    item(key = "create_post_row") {
                        CreatePostRow(
                            navController = navController,
                            profilePictureUrl = currentUser?.profilePicture?.imageUrl,
                            onRowClick = { navController.navigate("create_post") })
                    }
                }

                // Load States
                when (val refreshState = feedItems.loadState.refresh) {
                    is LoadState.Loading -> {
                        if (feedItems.itemCount == 0) {
                            item(key = "initial_loading") { FeedInitialLoadingState() }
                        }
                    }

                    is LoadState.Error -> {
                        // Ipakita lang ang error state kung walang kahit anong item (pati cache)
                        if (feedItems.itemCount == 0) {
                            item(key = "refresh_error") {
                                FeedErrorState(
                                    error = refreshState.error, onRetry = { feedItems.retry() })
                            }
                        }
                    }

                    is LoadState.NotLoading -> {
                        if (feedItems.itemCount == 0) {
                            item(key = "empty_state") { FeedEmptyState(feedType) }
                        }
                    }
                }

                // Main Feed Content
                if (feedItems.itemCount > 0) {
                    items(count = feedItems.itemCount, key = { index ->
                        val row = feedItems.peek(index)
                        val typeName = row?.type?.name ?: "unknown"
                        when {
                            row?.item != null -> "item_${index}_${typeName}_${row.item.hashCode()}"
                            row?.items != null -> "items_${index}_${typeName}_${row.items.hashCode()}"
                            else -> "row_${index}_${typeName}"
                        }
                    }, contentType = { index ->
                        feedItems.peek(index)?.type?.name ?: "unknown"
                    }) { index ->
                        feedItems[index]?.let { row ->
                            UnifiedFeedRow(
                                row = row,
                                navController = navController,
                                onReactionClick = viewModel::sendReaction,
                                onCommentClick = viewModel::openCommentSheet,
                                onMoreClick = { data ->
                                    if (data is com.cyberarcenal.huddle.api.models.PostFeed) {
                                        viewModel.openOptionsSheet(data)
                                    }
                                },
                                onImageClick = { activeMediaDetail = it },
                                onFollowClick = { user ->
                                    user.id?.let { uid ->
                                        viewModel.toggleFollow(
                                            userId = uid,
                                            currentIsFollowing = followStatuses[uid]
                                                ?: user.isFollowing ?: false,
                                            username = user.username ?: "user"
                                        )
                                    }
                                },
                                onShare = viewModel::sharePost,
                                onVideoClick = { post, url -> activeVideoPost = Pair(post, url) },
                                isPaused = commentSheetState != null || activeVideoPost != null,
                                followStatuses = followStatuses,
                                loadingUsers = loadingUserIds,
                                onGroupJoinClick = { group ->
                                    group.id?.let { viewModel.joinGroup(it) }
                                },
                                groupMembershipStatuses = groupMembershipStatuses,
                                joiningGroupIds = joiningGroupIds
                            )
                        }
                    }
                }

                if (feedItems.loadState.append is LoadState.Loading) {
                    item(key = "append_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    selectedImageUrl?.let {
        FullscreenImageDialog(imageUrl = it, onDismiss = { selectedImageUrl = null })
    }

    activeMediaDetail?.let {
        MediaDetailDialog(
            media = it,
            onDismiss = { activeMediaDetail = null },
            onReactionClick = viewModel::sendReaction,
            onCommentClick = { cType, id, stats ->
                activeMediaDetail = null
                viewModel.openCommentSheet(cType, id, stats)
            })
    }

    // Bottom Sheets - FULLY EXPANDED
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
            onReport = { postId, reason -> viewModel.reportPost(postId, reason) })
    }

    activeVideoPost?.let { (post: PostFeed, url: String) ->
        PostVideoFullscreenPlayer(
            post = post,
            videoUrl = url,
            onDismiss = { activeVideoPost = null },
            onReactionClick = { reactionType: ReactionTypeEnum? ->
                post.id?.let { id ->
                    viewModel.sendReaction(
                        ReactionCreateRequest(
                            contentType = "post", objectId = id, reactionType = reactionType
                        )
                    )
                }
            },
            onCommentClick = {
                activeVideoPost = null
                viewModel.openCommentSheet("post", post.id!!, post.statistics!!)
            },
            onShareClick = {
                activeVideoPost = null
                viewModel.sharePost(
                    ShareRequestData(
                        contentType = "post", contentId = post.id!!
                    )
                )
            },
            onProfileClick = { userId: Int ->
                activeVideoPost = null
                navController.navigate("profile/$userId")
            })
    }
}

// ==================== HELPER COMPOSABLES ====================
@Composable
fun FeedInitialLoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading feed...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun FeedErrorState(error: Throwable, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.SentimentDissatisfied,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error.localizedMessage ?: "Please check your connection",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun FeedEmptyState(feedType: FeedType) {
    val message = when (feedType) {
        FeedType.HOME -> "No posts yet.\nBe the first one to post something!"
        FeedType.DISCOVER -> "Nothing to discover right now.\nCheck back later!"
        FeedType.FRIENDS -> "No friends' posts yet."
        FeedType.FOLLOWING -> "You're not following anyone yet."
        FeedType.GROUPS -> "No group posts available."
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
            )
        }
    }
}
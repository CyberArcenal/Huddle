package com.cyberarcenal.huddle.ui.feed

import android.media.session.MediaSession
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.models.StoryViewerData
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.feed.components.*
import com.cyberarcenal.huddle.ui.home.components.CreatePostRow
import com.cyberarcenal.huddle.ui.profile.components.FullscreenImageDialog
import com.cyberarcenal.huddle.ui.profile.components.MediaDetailDialog
import com.cyberarcenal.huddle.ui.storyviewer.StoriesRow
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

inline fun <reified T> safeConvertTo(item: Any, tag: String = "Convert"): T? {
    return try {
        val gson = GsonBuilder()
            .registerTypeAdapter(OffsetDateTime::class.java, JsonDeserializer { json, _, _ ->
                OffsetDateTime.parse(json.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            })
            .create()
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
        factory = FeedViewModelFactory(
            feedType = feedType,
            postRepository = UserPostsRepository(),
            feedRepository = FeedRepository(),
            commentRepository = CommentsRepository(),
            reactionsRepository = UserReactionsRepository(),
            storyFeedRepository = StoriesRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository()
        )
    )
) {
    val context = LocalContext.current
    val loadingUsers = remember { mutableStateMapOf<Int, Boolean>() }

    val feedItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentUserId by remember { mutableStateOf<Int?>(null) }
    var currentUser by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        currentUser = TokenManager.getUser(context)
        viewModel.setCurrentUserId(currentUserId)
        viewModel.setCurrentUserData(currentUser)
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

    var activeMediaDetail by remember { mutableStateOf<MediaDetailData?>(null) }


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

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success, is ActionState.Error -> {
                // Clear all loading flags after any action
                loadingUsers.clear()
            }
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

    LaunchedEffect(Unit) {
        onActive?.invoke(viewModel)
    }

    LaunchedEffect(viewModel.refreshTrigger) {
        viewModel.refreshTrigger.collect {
            feedItems.refresh()
        }
    }

    if (activeMediaDetail != null) {
        MediaDetailDialog(
            imageUrl = activeMediaDetail!!.url,
            user = activeMediaDetail!!.user,
            createdAt = activeMediaDetail!!.createdAt,
            statistics = activeMediaDetail!!.stats,
            objectId = activeMediaDetail!!.id,
            contentType = activeMediaDetail!!.type,
            onDismiss = { activeMediaDetail = null },
            onReactionClick = { data ->
                viewModel.sendReaction(data = data)
            },
            onCommentClick = { cType, id ->
                activeMediaDetail = null // Close dialog then open sheet
                viewModel.openCommentSheet(cType, id)
            }
        )
    }


            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        feedItems.refresh()
                        viewModel.loadStories()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "stories_row") {
                        StoriesRow(
                            stories = stories,
                            onCreateStoryClick = {
                                navController.navigate("create_story")
                            },
                            onStoryClick = { storyFeed, index ->
                                StoryViewerData.storyFeeds = stories // the full list of StoryFeed
                                navController.navigate("story_feed_viewer/$index")
                            }
                        )
                    }

                    item(key = "create_post_row") {
                        CreatePostRow(
                            profilePictureUrl = currentUser?.profilePictureUrl,
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
                                "feed_row_${row.type}_${index}"
                            } else {
                                "feed_row_placeholder_$index"
                            }
                        },
                        contentType = { index -> feedItems[index]?.type?.name ?: "unknown" }
                    ) { index ->
                        val row = feedItems[index]
                        row?.let {
                            UnifiedFeedRow(
                                row = it,
                                navController = navController,
                                onReactionClick = { data ->
                                    viewModel.sendReaction(data)
                                },
                                onCommentClick = { contentType, id ->
                                    viewModel.openCommentSheet(contentType, id)
                                },
                                onMoreClick = { data ->
                                    when (data) {
                                        is PostFeed -> {
                                            viewModel.openOptionsSheet(data)
                                        }

                                        is ShareFeed -> {}
                                        else -> {}
                                    }

                                },
                                onImageClick = { data ->
                                    activeMediaDetail = data
                                },
                                onGroupJoinClick = {},
                                onFollowClick = { userMinimal ->
                                    // Gamitin ang .let para sa null safety sa halip na return@onFollowClick
                                    userMinimal.id?.let { userId ->
                                        // I-set ang loading state para sa specific user na ito
                                        loadingUsers[userId] = true

                                        viewModel.toggleFollow(
                                            userId = userId,
                                            currentIsFollowing = userMinimal.isFollowing ?: false,
                                            username = userMinimal.username ?: "user"
                                        )
                                    }
                                },
                                onShare = { shareData ->
                                    viewModel.sharePost(shareData)
                                },
                                followStatuses = viewModel.followStatuses.value,
                                loadingUsers = loadingUsers
                            )
                        }
                    }

                    if (feedItems.loadState.append is LoadState.Loading) {
                        item(key = "append_loading_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
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
                val data = ReactionCreateRequest(
                    contentType = "comment",
                    objectId = id,
                    reactionType = reactionType
                )
                viewModel.sendReaction(data)
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
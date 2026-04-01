package com.cyberarcenal.huddle.ui.feed

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
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
import com.cyberarcenal.huddle.ui.common.feed.MediaDetailDialog
import com.cyberarcenal.huddle.ui.storyviewer.StoriesRow
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
        factory = FeedViewModelFactory(
            feedType = feedType,
            postRepository = UserPostsRepository(),
            feedRepository = FeedRepository(),
            commentRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            storyFeedRepository = StoriesRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository(),
            userMediaRepository = UserMediaRepository(),
            groupRepository = GroupRepository(),
        )
    )
) {
    val context = LocalContext.current

    val feedItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.id

    LaunchedEffect(Unit) {
        val storedUser = TokenManager.getUser(context)
        viewModel.setCurrentUserId(storedUser?.id)
        viewModel.setCurrentUserData(storedUser)
    }

// Load profile picture if missing – this will automatically trigger when the user changes
    LaunchedEffect(currentUser?.profilePictureUrl) {
        if (currentUser?.profilePictureUrl.isNullOrBlank() && currentUserId != null) {
            viewModel.loadUserImage()
        }
    }

    LaunchedEffect(currentUser?.profilePictureUrl) {
        if (currentUser?.profilePictureUrl.isNullOrBlank()) {
            viewModel.loadUserImage()
        }
    }


    LaunchedEffect(currentUser?.profilePictureUrl) {
        if (currentUser?.profilePictureUrl.isNullOrBlank()) {
            viewModel.loadUserImage()
        }
    }

    try {
        Log.d("Feedscreen", "User data: $currentUser")
    } catch (e: Exception) {
        TODO("Not yet implemented")
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
    val followStatuses by viewModel.followStatuses.collectAsState()
    val loadingUserIds by viewModel.loadingUserIds.collectAsState()

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var activeMediaDetail by remember { mutableStateOf<MediaDetailData?>(null) }

    val groupMembershipStatuses by viewModel.groupMembershipStatuses.collectAsState()
    val joiningGroupIds by viewModel.joiningGroupIds.collectAsState()

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
        viewModel.loadUserImage()
    }

    if (selectedImageUrl != null) {
        FullscreenImageDialog(
            imageUrl = selectedImageUrl!!, onDismiss = { selectedImageUrl = null })
    }

    LaunchedEffect(Unit) {
        onActive?.invoke(viewModel)
    }

    LaunchedEffect(viewModel.refreshTrigger) {
        viewModel.refreshTrigger.collect {
            feedItems.refresh()
        }
    }

    activeMediaDetail?.let {
        MediaDetailDialog(

            onDismiss = { activeMediaDetail = null },
            onReactionClick = { data -> viewModel.sendReaction(data = data) },
            onCommentClick = { cType, id, stats ->
                activeMediaDetail = null
                viewModel.openCommentSheet(cType, id, stats)
            },
            media = it
        )
    }

        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Inalis ang windowInsetsPadding dito para sumadsad sa ilalim
                        .imePadding(),
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            // Alisin ang rounded corners
                            shape = RectangleShape,
                            // Gamitin ang primary o inverseSurface pero walang elevation
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            // ITO ANG PINAKAMAHALAGA: Alisin ang default 8dp-12dp margin ng Snackbar
                            modifier = Modifier.padding(0.dp)
                        )
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
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
                    state = listState, modifier = Modifier.fillMaxSize()
                ) {
                    if (feedType == FeedType.HOME) {
                        item(key = "stories_row") {
                            StoriesRow(
                                stories = stories,
                                currentUserProfilePicture = currentUser?.profilePictureUrl,
                                onCreateStoryClick = { navController.navigate("create_story") },
                                onStoryClick = { _, index ->
                                    StoryViewerData.storyFeeds = stories
                                    navController.navigate("story_feed_viewer/$index")
                                })
                        }

                        item(key = "create_post_row") {
                            CreatePostRow(
                                profilePictureUrl = currentUser?.profilePictureUrl,
                                onRowClick = { navController.navigate("create_post") })
                        }
                    }
                    // --- FIX: Stable Keys and Content Type for Paging ---
                    items(count = feedItems.itemCount, key = feedItems.itemKey { row ->
                        // UnifiedContentItem stores data in row.item or row.items
                        // We generate a key based on the object's hash code or ID if possible.
                        val id = when {
                            row.item != null -> "item_${row.type}_${row.item.hashCode()}"
                            row.items != null -> "items_${row.type}_${row.items.hashCode()}"
                            else -> "empty_${row.type}_${row.hashCode()}"
                        }
                        id
                    }, contentType = feedItems.itemContentType { it.type.name }) { index ->
                        val row = feedItems[index]
                        row?.let {
                            UnifiedFeedRow(
                                row = it,
                                navController = navController,
                                onReactionClick = { data -> viewModel.sendReaction(data) },
                                onCommentClick = { contentType, id, stats ->
                                    viewModel.openCommentSheet(
                                        contentType, id, stats
                                    )
                                },
                                onMoreClick = { data ->
                                    if (data is PostFeed) viewModel.openOptionsSheet(data)
                                },
                                onImageClick = { data -> activeMediaDetail = data },

                                onFollowClick = { userMinimal ->
                                    userMinimal.id?.let { userId ->
                                        viewModel.toggleFollow(
                                            userId = userId,
                                            currentIsFollowing = followStatuses[userId]
                                                ?: userMinimal.isFollowing ?: false,
                                            username = userMinimal.username ?: "user"
                                        )
                                    }
                                },
                                onShare = { shareData -> viewModel.sharePost(shareData) },
                                followStatuses = followStatuses,
                                loadingUsers = loadingUserIds,

                                onGroupJoinClick = { group ->
                                    viewModel.joinGroup(group.id ?: return@UnifiedFeedRow)
                                },
                                groupMembershipStatuses = groupMembershipStatuses,
                                joiningGroupIds = joiningGroupIds
                            )
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
}

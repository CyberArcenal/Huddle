package com.cyberarcenal.huddle.ui.reel.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.reel.feed.components.ReelPlayerItem

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelFeedScreen(
    navController: NavController,
    userId: Int? = null,
    initialReelId: Int? = null,
    globalSnackbarHostState: SnackbarHostState,
    currentUser: UserProfile?,
) {
    val context = LocalContext.current
    val viewModel: ReelFeedViewModel = viewModel(
        factory = ReelFeedViewModelFactory(
            reelsRepository = ReelsRepository(context.applicationContext),
            commentsRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository(),
            targetUserId = userId
        )
    )

    var currentUserId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        viewModel.setCurrentUserId(currentUserId)
    }

    val reels = viewModel.reelsPagingFlow.collectAsLazyPagingItems()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { reels.itemCount }
    )

    LaunchedEffect(initialReelId, reels.itemCount) {
        if (initialReelId != null && reels.itemCount > 0) {
            val index = (0 until reels.itemCount).firstOrNull { i -> reels[i]?.id == initialReelId }
            if (index != null) pagerState.scrollToPage(index)
        }
    }

    // ROOT BOX: Background Black, No StatusBarsPadding para Fullscreen
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val refreshState = reels.loadState.refresh

        when {
            refreshState is LoadState.Loading && reels.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            refreshState is LoadState.Error && reels.itemCount == 0 -> {
                val error = refreshState as LoadState.Error
                EmptyReelsState(
                    title = "Something went wrong",
                    description = error.error.localizedMessage ?: "Unable to load reels at the moment.",
                    buttonText = "Retry",
                    onActionClick = { reels.refresh() }
                )
            }
            reels.itemCount == 0 && refreshState is LoadState.NotLoading -> {
                EmptyReelsState(
                    title = "No Reels Yet",
                    description = "Be the first one to share a reel with the community!",
                    buttonText = "Refresh",
                    onActionClick = { reels.refresh() }
                )
            }
            else -> {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true,
                    beyondViewportPageCount = 1
                ) { page ->
                    val reel = reels[page]
                    if (reel != null) {
                        ReelPlayerItem(
                            reel = reel,
                            currentUser = currentUser,
                            isActive = page == pagerState.currentPage,
                            onReactionClick = { reactionType ->
                                viewModel.sendReaction(
                                    ReactionCreateRequest(
                                        contentType = "reel",
                                        objectId = reel.id ?: 0,
                                        reactionType = reactionType
                                    )
                                )
                            },
                            onCommentClick = { reel.id?.let { viewModel.openCommentSheet(it) } },
                            onShareClick = { shareData -> viewModel.shareReel(shareData) },
                            onProfileClick = { userId -> userId?.let { navController.navigate("profile/$it") } },
                            onCreateClick = { navController.navigate("create_reel") },
                            onFollowClick = { id, currentIsFollowing, username ->
                                viewModel.toggleFollow(id, currentIsFollowing, username)
                            },
                            onMoreClick = { reelId ->
                                viewModel.deleteReel(reelId)
                            }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }
        }

        // SNACKBAR HOST
        val actionState by viewModel.actionState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(actionState) {
            when (actionState) {
                is ActionState.Success -> snackbarHostState.showSnackbar((actionState as ActionState.Success).message)
                is ActionState.Error -> snackbarHostState.showSnackbar((actionState as ActionState.Error).message)
                else -> {}
            }
            viewModel.clearActionState()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }

    // Comment bottom sheet
    val commentSheetState by viewModel.commentSheetState.collectAsState()
    if (commentSheetState != null) {
        CommentBottomSheet(
            comments = viewModel.comments.collectAsState().value,
            replies = viewModel.replies.collectAsState().value,
            expandedReplies = viewModel.expandedReplies.collectAsState().value,
            currentUserId = currentUserId,
            isLoadingMore = viewModel.isLoadingMore.collectAsState().value,
            onLoadMore = viewModel::loadMoreComments,
            onToggleReplyExpanded = viewModel::toggleReplyExpansion,
            onLoadReplies = viewModel::loadReplies,
            onReactToComment = { id, reactionType ->
                viewModel.sendReaction(ReactionCreateRequest("comment", id, reactionType))
            },
            onReplyToComment = viewModel::addReply,
            onReportComment = { },
            onDismiss = viewModel::dismissCommentSheet,
            onSendComment = viewModel::addComment,
            onDeleteComment = { },
            actionState = viewModel.actionState.collectAsState().value,
            errorMessage = null
        )
    }
}

@Composable
fun EmptyReelsState(
    title: String,
    description: String,
    buttonText: String,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onActionClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).padding(horizontal = 24.dp)
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}



// StoryFeedViewerScreen.kt
package com.cyberarcenal.huddle.ui.storyviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.data.models.StoryFeedCache
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.managers.*
import com.cyberarcenal.huddle.ui.common.story.StoryViewerFrame
import com.cyberarcenal.huddle.ui.highlight.components.AddToHighlightSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryFeedViewerScreen(
    startIndex: Int,
    sessionId: String,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current

    // Retrieve from cache using sessionId
    val storyFeeds = remember(sessionId) {
        StoryFeedCache.retrieve(sessionId) ?: emptyList()
    }

    // Clear cache when leaving screen
    DisposableEffect(sessionId) {
        onDispose {
            StoryFeedCache.clear(sessionId)
        }
    }

    if (storyFeeds.isEmpty()) {
        LaunchedEffect(Unit) {
            globalSnackbarHostState.showSnackbar("Cannot load stories")
            navController.popBackStack()
        }
        return
    }

    val scope = rememberCoroutineScope()
    val viewManager = remember { ViewManager(ViewsRepository(), scope) }

    // Retrieve current user ID for comment bottom sheet
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
    }

    val viewModel = viewModel<StoryFeedViewerViewModel>(
        factory = StoryFeedViewerViewModelFactory(
            storyFeeds, startIndex, viewManager,
            storyRepository = StoriesRepository(context = context),
            commentRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository(),
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val closeEvent by viewModel.closeEvent.collectAsState(initial = null)
    val actionState by viewModel.actionState.collectAsState()
    val commentSheetState by viewModel.commentSheetState.collectAsState()

    // Collect comment data
    val comments by viewModel.comments.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMoreComments by viewModel.isLoadingMoreComments.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val isBookmarked by viewModel.isStoryBookmarked.collectAsState()

    val showHighlightSheet by viewModel.showHighlightSheet.collectAsState()
    val userHighlights by viewModel.userHighlights.collectAsState()

    val isAuthor = (uiState as? StoryFeedViewerUiState.Success)?.currentStory?.statistics?.isAuthor == true

    // Show snackbar for action messages
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> globalSnackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> globalSnackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    LaunchedEffect(closeEvent) {
        if (closeEvent != null) {
            navController.popBackStack()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                when (val state = uiState) {
                    is StoryFeedViewerUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is StoryFeedViewerUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                    is StoryFeedViewerUiState.Success -> {
                        StoryViewerFrame(
                            story = state.currentStory,
                            totalStories = state.totalStoriesInCurrentUser,
                            currentIndex = state.currentStoryIndex,
                            onTapLeft = viewModel::previousStory,
                            onTapRight = viewModel::nextStory,
                            onMoreClick = viewModel::onMoreClick,
                            onCommentClick = viewModel::openCommentSheet,
                            onReactionClick = viewModel::sendReaction,
                            onShareClick = viewModel::sharePost,
                            onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            },
                            onAddToHighlightClick = if (isAuthor) viewModel::onAddToHighlightClick else null,
                            onSaveClick = viewModel::onSaveClick,
                            isSaved = isBookmarked,
                            isHighlighted = false,
                            isPaused = commentSheetState != null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Comment bottom sheet
    commentSheetState?.let { state ->
        CommentBottomSheet(
            comments = comments,
            replies = replies,
            expandedReplies = expandedReplies,
            currentUserId = currentUserId,
            isLoadingMore = isLoadingMoreComments,
            onLoadMore = viewModel::loadMoreComments,
            onToggleReplyExpanded = viewModel::toggleReplyExpansion,
            onLoadReplies = viewModel::loadReplies,
            onReactToComment = { commentId, reactionType ->
                viewModel.reactionManager.sendReaction(
                    ReactionCreateRequest("comment", commentId, reactionType)
                )
            },
            onReplyToComment = viewModel::addReply,
            onReportComment = { _ -> },
            onDismiss = viewModel::dismissCommentSheet,
            onSendComment = viewModel::addComment,
            onDeleteComment = viewModel::deleteComment,
            actionState = actionState,
            errorMessage = commentsError,
            statistics = state.statistics
        )
    }

    // Story options bottom sheet
    val storyOptionsSheetState by viewModel.storyOptionsSheetState.collectAsState()
    storyOptionsSheetState?.let { story ->
        StoryOptionsBottomSheet(
            story = story,
            onDismiss = viewModel::dismissStoryOptionsSheet,
            onDelete = viewModel::deleteStory,
            onArchive = viewModel::archiveStory,
            onAddToHighlight = viewModel::addToHighlight,
            onSave = viewModel::saveStory
        )
    }

    if (showHighlightSheet) {
        val storyId = (uiState as? StoryFeedViewerUiState.Success)?.currentStory?.id ?: 0
        AddToHighlightSheet(
            storyId = storyId,
            highlights = userHighlights,
            onDismiss = viewModel::dismissHighlightSheet,
            onAddStoryToHighlight = { highlightId, _ ->
                viewModel.addStoryToHighlight(highlightId)
            },
            onCreateNewHighlight = viewModel::onCreateNewHighlight
        )
    }
}
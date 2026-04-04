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
import com.cyberarcenal.huddle.data.models.StoryViewerData
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.managers.*
import com.cyberarcenal.huddle.ui.common.story.StoryViewerFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryFeedViewerScreen(
    startIndex: Int,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val storyFeeds = StoryViewerData.storyFeeds ?: emptyList()
    if (storyFeeds.isEmpty()) {
        LaunchedEffect(Unit) { navController.popBackStack() }
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
            storyRepository = StoriesRepository(),
            commentRepository = CommentsRepository(),
            reactionsRepository = ReactionsRepository(),
            sharePostsRepository = SharePostsRepository(),
            followRepository = FollowRepository()
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
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()

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

    Scaffold(
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clip(RoundedCornerShape(4.dp))
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
                            onCommentClick = viewModel::onCommentClick,
                            onReactionClick = viewModel::onReactionClick,
                            onShareClick = viewModel::onShareClick,
                            onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Comment bottom sheet
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
            actionState = actionState,      // Use collected value
            errorMessage = commentsError
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
}
// ProfileScreen.kt (corrected)

package com.cyberarcenal.huddle.ui.profile

import android.app.Activity
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.common.shimmer.ProfileShimmer
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.feed.components.PostOptionsBottomSheet
import com.cyberarcenal.huddle.ui.profile.components.*
import com.cyberarcenal.huddle.ui.profile.managers.ProfileImageManager
import com.cyberarcenal.huddle.ui.profile.managers.ProfileState
import com.cyberarcenal.huddle.ui.profile.managers.ProfileViewModel
import com.cyberarcenal.huddle.ui.profile.managers.ProfileViewModelFactory
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int?,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            userId, context.applicationContext as Application,
            userProfileRepository = UsersRepository(),
            followRepository = FollowRepository(),
            userMediaRepository = UserMediaRepository(),
            postRepository = UserPostsRepository(),
            commentRepository = CommentsRepository(),
            reactionRepository = UserReactionsRepository(),
            userContentRepository = UserContentRepository(),
            sharePostsRepository = SharePostsRepository(),
            storiesRepository = StoriesRepository()
        )
    )

    // Current user ID
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        viewModel.setCurrentUserId(currentUserId)
    }

    val loadingUsers = remember { mutableStateMapOf<Int, Boolean>() }

    // Paging flows
    val mediaItems = viewModel.mediaGridFlow.collectAsLazyPagingItems()
    val likedItems = viewModel.likedItemsFlow.collectAsLazyPagingItems()
    val userContent = viewModel.userContentFlow.collectAsLazyPagingItems()
    val userHighlights by viewModel.highlightManager.userHighlights.collectAsState()

    val followStatus by viewModel.followManager.followStatus.collectAsState()
    val followStats by viewModel.followManager.followStats.collectAsState()

    // Core state
    val profileState by viewModel.profileState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionState by viewModel.actionState.collectAsState()
    val fullscreenImageData by viewModel.fullscreenImage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Image manager state
    val selectedImageUri by viewModel.imageManager.selectedImageUri.collectAsState()
    val validationError by viewModel.imageManager.validationError.collectAsState()
    val uploadType by viewModel.imageManager.uploadType.collectAsState()

    // Comment manager state
    val commentSheetState by viewModel.commentManager.commentSheetState.collectAsState()
    val optionsSheetState by viewModel.commentManager.optionsSheetState.collectAsState()
    val comments by viewModel.commentManager.comments.collectAsState()
    val commentsError by viewModel.commentManager.commentsError.collectAsState()
    val replies by viewModel.commentManager.replies.collectAsState()
    val expandedReplies by viewModel.commentManager.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.commentManager.isLoadingMore.collectAsState()

    // Highlight manager state
    val recentStories by viewModel.highlightManager.recentStories.collectAsState()
    val isCreatingHighlight by viewModel.highlightManager.isCreatingHighlight.collectAsState()

    // UI state
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = profileState is ProfileState.Loading
    var showAddHighlightSheet by remember { mutableStateOf(false) }

    // Crop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val croppedUri = result.data?.let { UCrop.getOutput(it) }
                croppedUri?.let { viewModel.imageManager.onCropResult(it) }
            }
            UCrop.RESULT_ERROR -> {
                val error = result.data?.let { UCrop.getError(it) }
                viewModel.imageManager.onCropError(error?.message ?: "Crop failed")
            }
            else -> viewModel.imageManager.cancelCrop()
        }
    }

    // Image pickers
    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.imageManager.onImagePickedForProfile(it) } }
    )

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.imageManager.onImagePickedForCover(it) } }
    )

    // Auto-crop when image is selected
    LaunchedEffect(selectedImageUri, uploadType) {
        selectedImageUri?.let { uri ->
            val intent = when (uploadType) {
                ProfileImageManager.UploadType.PROFILE -> viewModel.imageManager.startProfileCropIntent(context, uri)
                ProfileImageManager.UploadType.COVER -> viewModel.imageManager.startCoverCropIntent(context, uri)
                else -> null
            }
            intent?.let { cropLauncher.launch(it) }
        }
    }

    // Show validation errors
    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.imageManager.cancelCrop()
        }
    }

    // Show action messages
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> snackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    // Refresh media grid after successful upload
    LaunchedEffect(actionState) {
        val state = actionState // ← local variable to avoid smart cast issue
        if (state is ActionState.Success &&
            (state.message.contains("Profile picture") || state.message.contains("Cover photo"))
        ) {
            mediaItems.refresh()
        }
    }

    // Fullscreen image dialog
    if (fullscreenImageData != null) {
        MediaDetailDialog(
            imageUrl = fullscreenImageData!!.url,
            user = fullscreenImageData!!.user,
            createdAt = fullscreenImageData!!.createdAt,
            statistics = fullscreenImageData!!.stats,
            objectId = fullscreenImageData!!.id,
            contentType = fullscreenImageData!!.type,
            onDismiss = { viewModel.dismissFullscreenImage() },
            onReactionClick = { data -> viewModel.reactionManager.sendReaction(data) },
            onCommentClick = { cType, id ->
                viewModel.dismissFullscreenImage()
                viewModel.commentManager.openCommentSheet(cType, id)
            }
        )
    }

    // Load recent stories when add highlight sheet is opened
    LaunchedEffect(showAddHighlightSheet) {
        if (showAddHighlightSheet) {
            viewModel.highlightManager.loadRecentStories()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    ProfileShimmer()
                }
                is ProfileState.Success -> {
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                viewModel.loadProfile()
                                userContent.refresh()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ProfileScrollContent(
                            navController = navController,
                            followStatus = followStatus,
                            followStats = followStats,
                            profile = state.profile,
                            isCurrentUser = userId == null,
                            userContent = userContent,
                            likedItems = likedItems,
                            storyHighlights = userHighlights,
                            mediaItems = mediaItems,
                            listState = listState,
                            onReactionClick = { data -> viewModel.reactionManager.sendReaction(data) },
                            onCommentClick = { contentType, objectId ->
                                viewModel.commentManager.openCommentSheet(contentType, objectId)
                            },
                            onShareClick = { data -> viewModel.sharePost(data) },
                            onMoreClick = { unifiedItem ->
                                when (unifiedItem) {
                                    is PostFeed -> { /* open options sheet */
                                    }

                                    else -> {}
                                }
                            },
                            onImageClick = { url -> viewModel.showFullscreenImage(url) },
                            onAvatarClick = { data -> viewModel.showFullscreenImage(data) },
                            onCoverClick = { data -> viewModel.showFullscreenImage(data) },
                            onEditProfilePicture = {
                                profilePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onEditCoverPhoto = {
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveProfilePicture = viewModel.imageManager::removeProfilePicture,
                            onRemoveCoverPhoto = viewModel.imageManager::removeCoverPhoto,
                            onFollowToggle = {
                                val targetUserId =
                                    followStatus?.userId ?: return@ProfileScrollContent
                                if (followStatus?.isFollowing == true) {
                                    viewModel.followManager.unfollowUser(targetUserId)
                                } else {
                                    viewModel.followManager.followUser(targetUserId)
                                }
                            },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToEditProfile = { navController.navigate("preferences") },
                            onNavigateBack = { navController.popBackStack() },
                            onAddHighlightClick = { showAddHighlightSheet = true },
                            onFollowClick = { user ->
                                user.id?.let {
                                    viewModel.followManager.followUser(user.id)
                                }
                            },

                            onHighlightClick = { highlight ->
                                // Navigate to story viewer with highlight ID
                                navController.navigate("highlight_viewer/${highlight.id}")
                            },
                            followStatuses = viewModel.followStatuses.value,
                            loadingUsers = loadingUsers
                        )
                    }
                }
                is ProfileState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadProfile() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Comment Bottom Sheet
    if (commentSheetState != null) {
        CommentBottomSheet(
            comments = comments,
            replies = replies,
            expandedReplies = expandedReplies,
            currentUserId = currentUserId,
            isLoadingMore = isLoadingMore,
            onLoadMore = viewModel.commentManager::loadMoreComments,
            onToggleReplyExpanded = viewModel.commentManager::toggleReplyExpansion,
            onLoadReplies = viewModel.commentManager::loadReplies,
            onReactToComment = { id, reactionType ->
                val data = ReactionCreateRequest(
                    contentType = "comment",
                    objectId = id,
                    reactionType = reactionType
                )
                viewModel.reactionManager.sendReaction(data)
            },
            onReplyToComment = viewModel.commentManager::addReply,
            onReportComment = { commentId ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Reported comment $commentId (not implemented)")
                }
            },
            onDismiss = viewModel.commentManager::dismissCommentSheet,
            onSendComment = viewModel.commentManager::addComment,
            onDeleteComment = viewModel.commentManager::deleteComment,
            actionState = actionState,
            errorMessage = commentsError
        )
    }

    // Post Options Bottom Sheet
    if (optionsSheetState != null) {
        PostOptionsBottomSheet(
            post = optionsSheetState!!.post,
            isCurrentUser = optionsSheetState!!.post.user?.id == currentUserId,
            onDismiss = viewModel.commentManager::dismissOptionsSheet,
            onDelete = viewModel::deletePost,
            onReport = { postId, reason -> viewModel.reportPost(postId, reason) }
        )
    }

    // Add Highlight Sheet
    if (showAddHighlightSheet) {
        AddHighlightSheet(
            stories = recentStories,
            onDismiss = { showAddHighlightSheet = false },
            onConfirm = { title: String, selectedIds: List<Int> ->
                viewModel.highlightManager.createHighlight(title, selectedIds)
                showAddHighlightSheet = false
            },
            isCreating = isCreatingHighlight
        )
    }
}
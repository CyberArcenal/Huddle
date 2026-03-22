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
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.feed.ActionState
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.feed.components.PostOptionsBottomSheet
import com.cyberarcenal.huddle.ui.profile.components.*
import com.cyberarcenal.huddle.ui.profile.ProfileViewModel.UploadType
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
            userFollowRepository = FollowRepository(),
            userMediaRepository = UserMediaRepository(),
            postRepository = UserPostsRepository(),
            commentRepository = CommentsRepository(),
            reactionRepository = UserReactionsRepository(),
            userContentRepository = UserContentRepository()
        )
    )

    // Current user ID
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        viewModel.setCurrentUserId(currentUserId)
    }

    val profileState by viewModel.profileState.collectAsState()
    val userContent = viewModel.userContentFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionState by viewModel.actionState.collectAsState()
    val fullscreenImage by viewModel.fullscreenImage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    val uploadType by viewModel.uploadType.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = profileState is ProfileState.Loading

    // Bottom sheet states from ViewModel
    val commentSheetState by viewModel.commentSheetState.collectAsState()
    val optionsSheetState by viewModel.optionsSheetState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    // Crop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val croppedUri = result.data?.let { UCrop.getOutput(it) }
                croppedUri?.let { viewModel.onCropResult(it) }
            }
            UCrop.RESULT_ERROR -> {
                val error = result.data?.let { UCrop.getError(it) }
                viewModel.onCropError(error?.message ?: "Crop failed")
            }
            else -> viewModel.cancelCrop()
        }
    }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.onImagePickedForProfile(it) } }
    )

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.onImagePickedForCover(it) } }
    )

    LaunchedEffect(selectedImageUri, uploadType) {
        selectedImageUri?.let { uri ->
            val intent = when (uploadType) {
                UploadType.PROFILE -> viewModel.startProfileCropIntent(context, uri)
                UploadType.COVER -> viewModel.startCoverCropIntent(context, uri)
                else -> null
            }
            intent?.let { cropLauncher.launch(it) }
        }
    }

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.cancelCrop()
        }
    }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> snackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    if (fullscreenImage != null) {
        FullscreenImageDialog(
            imageUrl = fullscreenImage!!,
            onDismiss = viewModel::dismissFullscreenImage
        )
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(Color.White)
        ) {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
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
                            profile = state.profile,
                            isCurrentUser = userId == null,
                            userContent = userContent,
                            listState = listState,
                            onReaction = { contentType, objectId, reactionType ->
                                viewModel.sendReaction(contentType, objectId, reactionType)
                            },
                            onCommentClick = { contentType, objectId ->
                                viewModel.openCommentSheet(contentType, objectId)
                            },
                            onShareClick = { contentType, objectId ->
                                // Handle share if needed (maybe navigate to share detail)
                                // Currently we have no share detail screen, but you could open a share sheet.
                            },
                            onMoreClick = { unifiedItem ->
                                // When clicking more on a post, we need to extract the post data
                                // For now, if the item is a post, open options sheet
                                when (unifiedItem.type) {
                                    "post" -> {

                                    }
                                    // Add other types if needed
                                    else -> {}
                                }
                            },
                            onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            },
                            onImageClick = { url ->
                                viewModel.showFullscreenImage(url)
                            },
                            onAvatarClick = { viewModel.showFullscreenImage(state.profile.profilePictureUrl) },
                            onCoverClick = { viewModel.showFullscreenImage(state.profile.coverPhotoUrl) },
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
                            onRemoveProfilePicture = viewModel::removeProfilePicture,
                            onRemoveCoverPhoto = viewModel::removeCoverPhoto,
                            onFollowToggle = viewModel::onFollowToggle,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToEditProfile = { navController.navigate("edit_profile") },
                            onNavigateBack = { navController.popBackStack() }
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
            onLoadMore = viewModel::loadMoreComments,
            onToggleReplyExpanded = viewModel::toggleReplyExpansion,
            onLoadReplies = viewModel::loadReplies,
            onReactToComment = { id, reactionType ->
                viewModel.sendCommentReaction(id, reactionType)
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

    // Post Options Bottom Sheet
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
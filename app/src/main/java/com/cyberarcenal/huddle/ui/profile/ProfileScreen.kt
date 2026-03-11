package com.cyberarcenal.huddle.ui.profile

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.feed.ActionState
import com.cyberarcenal.huddle.ui.feed.components.CommentBottomSheet
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
        factory = ProfileViewModelFactory(userId, context.applicationContext as android.app.Application)
    )

    // Current user ID
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        viewModel.setCurrentUserId(currentUserId)
    }
    val profileState by viewModel.profileState.collectAsState()
    val userPosts = viewModel.userPostsFlow.collectAsLazyPagingItems()
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileState.Success -> {
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                viewModel.loadProfile()
                                userPosts.refresh()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ProfileScrollContent(
                            profile = state.profile,
                            isCurrentUser = userId == null,
                            userPosts = userPosts,
                            listState = listState,
                            onToggleLike = viewModel::toggleLike,
                            onNavigateToComments = { postId ->
                                viewModel.openCommentSheet(postId)
                            },

                            onCommentClick = {
                                viewModel.openCommentSheet(it.id)
                            },
                            onMoreClick = {
                                viewModel.openOptionsSheet(it)
                            },
                            onProfileClick = {
                                navController.navigate("profile/${it.user?.id}")
                            },
                            onImageClick = { url ->

                            },

                            // Header callbacks
                            onAvatarClick = { viewModel.showFullscreenImage(state.profile.profilePictureUrl) },
                            onCoverClick = { viewModel.showFullscreenImage(state.profile.coverPhotoUrl) },
                            onEditProfilePicture = {
                                profilePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onEditCoverPhoto = {
                                coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                            Text("Error: ${state.message}")
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
            postId = commentSheetState!!.postId,
            comments = comments,
            replies = replies,
            expandedReplies = expandedReplies,
            currentUserId = currentUserId,
            isLoadingMore = isLoadingMore,
            onLoadMore = viewModel::loadMoreComments,
            onToggleReplyExpanded = viewModel::toggleReplyExpansion,
            onLoadReplies = viewModel::loadReplies,
            onLikeComment = viewModel::likeComment,
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
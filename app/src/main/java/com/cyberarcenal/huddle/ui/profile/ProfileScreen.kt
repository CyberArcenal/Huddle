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
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.models.HighlightCache
import com.cyberarcenal.huddle.data.models.StoryFeedCache
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.common.shimmer.ProfileShimmer
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.feed.MediaDetailDialog
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.post.PostVideoFullscreenPlayer
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.feed.components.PostOptionsBottomSheet
import com.cyberarcenal.huddle.ui.highlight.components.AddHighlightSheet
import com.cyberarcenal.huddle.ui.profile.components.*
import com.cyberarcenal.huddle.ui.profile.managers.ProfileImageManager
import com.cyberarcenal.huddle.ui.profile.managers.ProfileState
import com.cyberarcenal.huddle.ui.profile.managers.ProfileViewModel
import com.cyberarcenal.huddle.ui.profile.managers.ProfileViewModelFactory
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int?,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            userId, context.applicationContext as Application,
            userProfileRepository = UsersRepository(LocalContext.current),
            followRepository = FollowRepository(),
            userMediaRepository = UserMediaRepository(),
            postRepository = UserPostsRepository(),
            commentRepository = CommentsRepository(),
            reactionRepository = ReactionsRepository(),
            userContentRepository = UserContentRepository(),
            sharePostsRepository = SharePostsRepository(),
            reelsRepository = ReelsRepository(LocalContext.current),
            storiesRepository = StoriesRepository(context = context.applicationContext),
            groupRepository = GroupRepository(),
            context = context.applicationContext,
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
    val posts = viewModel.postsFlow.collectAsLazyPagingItems()
    val photos = viewModel.photosFlow.collectAsLazyPagingItems()
    val videos = viewModel.videosFlow.collectAsLazyPagingItems()
    val likedItems = viewModel.likedItemsFlow.collectAsLazyPagingItems()
    val userContent = viewModel.userContentFlow.collectAsLazyPagingItems()
    val userReels by viewModel.reelManager.userReels.collectAsState()
    val userHighlights by viewModel.highlightManager.userHighlights.collectAsState()

    val followStatus by viewModel.followManager.followStatus.collectAsState()
    val followStats by viewModel.followManager.followStats.collectAsState()

    // Core state
    val profileState by viewModel.profileState.collectAsState()
    val listState = rememberLazyListState()
    val actionState by viewModel.actionState.collectAsState()
    val fullscreenImageData by viewModel.fullscreenImage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val recentMoots by viewModel.recentMoots.collectAsState()

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
    val isCurrentUser by viewModel.isOwnProfile.collectAsState()

    // Highlight manager state
    val recentStories by viewModel.highlightManager.recentStories.collectAsState()
    val isCreatingHighlight by viewModel.highlightManager.isCreatingHighlight.collectAsState()

    val groupMembershipStatuses by viewModel.groupMembershipStatuses.collectAsState()
    val joiningGroupIds by viewModel.joiningGroupIds.collectAsState()

    val personalityDetail by viewModel.personalityManager.personalityDetail.collectAsState()
    val isLoadingPersonality by viewModel.personalityManager.isLoading.collectAsState()

    val selectedFilter by viewModel.selectedFilter.collectAsState()

    // UI state
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showAddHighlightSheet by remember { mutableStateOf(false) }
    var activeVideoPost by remember { mutableStateOf<Pair<PostFeed, String>?>(null) }

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
        onResult = { uri -> uri?.let { viewModel.imageManager.onImagePickedForProfile(it) } })

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.imageManager.onImagePickedForCover(it) } })

    // Auto-crop when image is selected
    LaunchedEffect(selectedImageUri, uploadType) {
        selectedImageUri?.let { uri ->
            val intent = when (uploadType) {
                ProfileImageManager.UploadType.PROFILE -> viewModel.imageManager.startProfileCropIntent(
                    context, uri
                )

                ProfileImageManager.UploadType.COVER -> viewModel.imageManager.startCoverCropIntent(
                    context, uri
                )

                else -> null
            }
            intent?.let { cropLauncher.launch(it) }
        }
    }

    // Show validation errors
    LaunchedEffect(validationError) {
        validationError?.let {
            globalSnackbarHostState.showSnackbar(it)
            viewModel.imageManager.cancelCrop()
        }
    }

    // Show action messages
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> globalSnackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> globalSnackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    // Refresh media grid after successful upload
    LaunchedEffect(actionState) {
        val state = actionState // ← local variable to avoid smart cast issue
        if (state is ActionState.Success && (state.message.contains("Profile picture") || state.message.contains(
                "Cover photo"
            ))
        ) {
            photos.refresh()
            videos.refresh()
        }
    }

    // Fullscreen image dialog
    fullscreenImageData?.let {
        MediaDetailDialog(
            media = it,
            onDismiss = { viewModel.dismissFullscreenImage() },
            onReactionClick = { data -> viewModel.reactionManager.sendReaction(data) },
            onCommentClick = { cType, id, stats ->
                viewModel.dismissFullscreenImage()
                viewModel.commentManager.openCommentSheet(cType, id, stats)
            },
            onShare = viewModel::sharePost
        )
    }

    // Load recent stories when add highlight sheet is opened
    LaunchedEffect(showAddHighlightSheet) {
        if (showAddHighlightSheet) {
            viewModel.highlightManager.loadRecentStories()
        }
    }

    // Update TokenManager when own profile loads
    LaunchedEffect(profileState, isCurrentUser) {
        val state = profileState
        if (isCurrentUser && state is ProfileState.Success) {
            TokenManager.saveUser(context, state.profile)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
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
                            viewModel.manualRefresh()
                            userContent.refresh()
                            photos.refresh()
                            videos.refresh()
                            likedItems.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ProfileScrollContent(
                            navController = navController,
                            followStatus = followStatus,
                            followStats = followStats,
                            profile = state.profile,
                            isCurrentUser = isCurrentUser,
                            userContent = userContent,
                            posts = posts,
                            photos = photos,
                            videos = videos,
                            likedItems = likedItems,
                            storyHighlights = userHighlights,
                            reelItems = userReels,
                            isReelsLoading = isRefreshing,
                            listState = listState,
                            onReactionClick = { data -> viewModel.reactionManager.sendReaction(data) },
                            onCommentClick = { contentType, objectId, stats ->
                                viewModel.commentManager.openCommentSheet(
                                    contentType, objectId, stats
                                )
                            },
                            onShareClick = { data -> viewModel.sharePost(data) },
                            onMoreClick = { unifiedItem ->
                                when (unifiedItem) {
                                    is PostFeed -> {
                                        viewModel.commentManager.openOptionsSheet(unifiedItem)
                                    }

                                    else -> {}
                                }
                            },
                            onVideoClick = { post, url -> activeVideoPost = Pair(post, url) },
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
                            onFilterChange = viewModel::setSelectedFilter,
                            selectedFilter = selectedFilter,
                            onFollowClick = { user ->
                                user.id?.let {
                                    viewModel.followManager.followUser(user.id)
                                }
                            },
                            isPaused = commentSheetState != null,

                            onHighlightClick = { highlight ->
                                // Navigate to story viewer with highlight ID
                                val index = userHighlights.indexOf(highlight)
                                val sessionId = UUID.randomUUID().toString()
                                // Store the entire list of StoryFeed in cache
                                HighlightCache.store(sessionId, userHighlights)

                                navController.navigate("highlight_carousel/$index/$sessionId")
                            },
                            followStatuses = viewModel.followStatuses.value,
                            loadingUsers = loadingUsers,
                            groupMembershipStatuses = groupMembershipStatuses,
                            joiningGroupIds = joiningGroupIds,
                            recentMoots = recentMoots,
                            onPersonalityClick = { mbti ->
                                viewModel.personalityManager.openPersonalityDetail(mbti)
                            }
                        )
                    }
                }

                is ProfileState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    contentType = "comment", objectId = id, reactionType = reactionType
                )
                viewModel.reactionManager.sendReaction(data)
            },
            onReplyToComment = viewModel.commentManager::addReply,
            onReportComment = { commentId ->
                coroutineScope.launch {
                    globalSnackbarHostState.showSnackbar("Reported comment $commentId (not implemented)")
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
            onReport = { postId, reason -> viewModel.reportPost(postId, reason) })
    }

    // Add Highlight Sheet
    if (showAddHighlightSheet) {
        AddHighlightSheet(
            stories = recentStories,
            onDismiss = { showAddHighlightSheet = false },
            onConfirm = { title: String, selectedIds: List<Int> ->
                viewModel.highlightManager.createHighlight(title, selectedIds, context)
                showAddHighlightSheet = false
            },
            isCreating = isCreatingHighlight
        )
    }

    // Personality Detail Bottom Sheet
    personalityDetail?.let { details ->
        PersonalityDetailBottomSheet(
            details = details,
            onDismiss = viewModel.personalityManager::dismissPersonalityDetail
        )
    }

    activeVideoPost?.let { (post: PostFeed, url: String) ->
        PostVideoFullscreenPlayer(
            post = post,
            videoUrl = url,
            onDismiss = { activeVideoPost = null },
            onReactionClick = { reactionType: ReactionTypeEnum? ->
                post.id?.let { id ->
                    viewModel.reactionManager.sendReaction(
                        ReactionCreateRequest(
                            contentType = "post",
                            objectId = id,
                            reactionType = reactionType
                        )
                    )
                }
            },
            onCommentClick = {
                activeVideoPost = null
                viewModel.commentManager.openCommentSheet("post", post.id!!, post.statistics!!)
            },
            onShareClick = { shareData ->
                activeVideoPost = null
                viewModel.sharePost(shareData)
            },
            onMoreClick = {
                activeVideoPost = null
                viewModel.commentManager.openOptionsSheet(post)
            },
            onProfileClick = { userId: Int ->
                if (userId != (profileState as? ProfileState.Success)?.profile?.id) {
                    activeVideoPost = null
                    navController.navigate("profile/$userId")
                }
            }
        )
    }
}
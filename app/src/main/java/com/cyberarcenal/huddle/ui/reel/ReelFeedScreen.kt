package com.cyberarcenal.huddle.ui.reel

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelFeedScreen(
    navController: NavController,
    initialReelId: Int? = null,
    viewModel: ReelFeedViewModel = viewModel(
        factory = ReelFeedViewModelFactory(
            reelsRepository = ReelsRepository(),
            commentsRepository = CommentsRepository(),
            reactionsRepository = UserReactionsRepository(),
            sharePostsRepository = SharePostsRepository()
        )
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

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

    val currentPage = pagerState.currentPage
    val players = remember { mutableStateMapOf<Int, ExoPlayer>() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> { players[currentPage]?.pause() }
                Lifecycle.Event.ON_RESUME -> { players[currentPage]?.play() }
                Lifecycle.Event.ON_DESTROY -> {
                    players.values.forEach { it.release() }
                    players.clear()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentPage) {
        players.forEach { (page, player) ->
            if (page != currentPage) {
                player.pause()
                player.seekTo(0)
            } else { player.play() }
        }
    }

    LaunchedEffect(initialReelId, reels.itemCount) {
        if (initialReelId != null && reels.itemCount > 0) {
            val index = (0 until reels.itemCount).firstOrNull { i -> reels[i]?.id == initialReelId }
            if (index != null) pagerState.scrollToPage(index)
        }
    }

    // ROOT BOX: Background Black, No StatusBarsPadding para Fullscreen
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 1
        ) { page ->
            val reel = reels[page]
            if (reel != null) {
                var isPlaying by remember { mutableStateOf(true) }
                var showPlayIcon by remember { mutableStateOf(false) }

                val player = players.getOrPut(page) {
                    ExoPlayer.Builder(context).build().apply {
                        playWhenReady = page == currentPage
                        repeatMode = Player.REPEAT_MODE_ONE
                        setMediaItem(MediaItem.fromUri(Uri.parse(reel.videoUrl ?: "")))
                        prepare()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isPlaying = !isPlaying
                            if (isPlaying) player.play() else player.pause()
                            coroutineScope.launch {
                                showPlayIcon = true
                                delay(500)
                                showPlayIcon = false
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = !isPlaying || showPlayIcon,
                        enter = fadeIn(), exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    ReelOverlay(
                        reel = reel,
                        onLikeClick = {
                            viewModel.sendReaction(
                                ReactionCreateRequest(
                                    contentType = "reel",
                                    objectId = reel.id ?: 0,
                                    reactionType = if (reel.hasLiked == true) null else ReactionCreateRequest.ReactionType.LIKE
                                )
                            )
                        },
                        onCommentClick = { reel.id?.let { viewModel.openCommentSheet(it) } },
                        onShareClick = { viewModel.shareReel(reel) },
                        onProfileClick = { userId -> userId?.let { navController.navigate("profile/$it") } }
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // SNACKBAR HOST - Naka-align sa bottom, walang extra box padding
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
                .padding(bottom = 80.dp) // Itaas ng konti para hindi matakpan ng bottom nav
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

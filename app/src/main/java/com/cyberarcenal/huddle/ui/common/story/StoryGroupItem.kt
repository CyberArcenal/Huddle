package com.cyberarcenal.huddle.ui.common.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.ViewsRepository
import com.cyberarcenal.huddle.ui.common.feed.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

/**
 * A feed item that displays a user's story group (multiple stories) with a built‑in story viewer.
 */
@Composable
fun StoryGroupedItem(
    user: UserMinimal?,
    stories: List<Story>,
    createdAt: OffsetDateTime?,
    caption: String? = null,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
    onReactionSummaryClick: () -> Unit = onCommentClick,
    onCommentSummaryClick: () -> Unit = onCommentClick
) {
    FeedItemFrame(
        user = user,
        createdAt = createdAt,
        statistics = stories.firstOrNull()?.statistics,
        headerSuffix = "",
        caption = caption,
        onReactionClick = onReactionClick,
        onCommentClick = onCommentClick,
        onShareClick = onShareClick,
        onMoreClick = onMoreClick,
        onProfileClick = onProfileClick,
        onReactionSummaryClick = onReactionSummaryClick,
        onCommentSummaryClick = onCommentSummaryClick,
        postData = stories.firstOrNull(),
        showBottomDivider = true,
        content = {
            StoryGroupViewer(
                stories = stories,
                user = user,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    )
}

@Composable
fun StoryGroupViewer(
    stories: List<Story>,
    user: UserMinimal?,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { stories.size })
    val currentIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val viewManager = remember { ViewManager(ViewsRepository(), coroutineScope) }

    // Visibility tracking - FIXED
    val configuration = LocalConfiguration.current
    val density = context.resources.displayMetrics.density
    val screenHeightPx = (configuration.screenHeightDp * density).toInt()
    var visiblePercentage by remember { mutableStateOf(1f) }
    val visibilityThreshold = 0.2f

    var isPaused by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    // Update visibility when layout changes
    fun updateVisibility(bounds: android.graphics.Rect) {
        val visibleTop = maxOf(bounds.top, 0)
        val visibleBottom = minOf(bounds.bottom, screenHeightPx)
        val visibleHeight = maxOf(0, visibleBottom - visibleTop)
        val totalHeight = bounds.height()
        visiblePercentage = if (totalHeight > 0) visibleHeight.toFloat() / totalHeight else 0f
    }

    // Auto-pause when not enough visible
    LaunchedEffect(visiblePercentage) {
        isPaused = visiblePercentage < visibilityThreshold
    }

    val onStoryEnd = {
        if (currentIndex < stories.size - 1) {
            coroutineScope.launch { pagerState.animateScrollToPage(currentIndex + 1) }
        }
    }

    LaunchedEffect(currentIndex) {
        val currentStory = stories[currentIndex]
        if (currentStory.id != null && !currentStory.hasViewed!!) {
            viewManager.recordView("story", currentStory.id, 3)
        }
    }

    LaunchedEffect(currentIndex, isPaused) {
        val story = stories[currentIndex]
        val isVideo = story.storyType.value == "video"
        timerJob?.cancel()
        timerJob = null
        currentProgress = 0f

        if (!isVideo && !isPaused) {
            val duration = 10000L
            val startTime = System.currentTimeMillis()
            timerJob = coroutineScope.launch {
                while (currentProgress < 1f) {
                    val elapsed = System.currentTimeMillis() - startTime
                    currentProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                    if (currentProgress >= 1f) {
                        onStoryEnd()
                        break
                    }
                    delay(16)
                }
            }
        }
    }

    DisposableEffect(currentIndex) {
        onDispose { timerJob?.cancel() }
    }

    Box(modifier = modifier
        .aspectRatio(9f / 16f)
        .onGloballyPositioned { coordinates ->
            val bounds = android.graphics.Rect(
                coordinates.positionInWindow().x.toInt(),
                coordinates.positionInWindow().y.toInt(),
                (coordinates.positionInWindow().x + coordinates.size.width).toInt(),
                (coordinates.positionInWindow().y + coordinates.size.height).toInt()
            )
            updateVisibility(bounds)
        }) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            key = { page -> stories[page].id ?: page }
        ) { page ->
            StoryContent(
                story = stories[page],
                isPaused = isPaused || pagerState.isScrollInProgress,
                onVideoProgress = { progress ->
                    if (page == currentIndex) {
                        currentProgress = progress
                    }
                },
                onVideoFinished = {
                    if (page == currentIndex) {
                        onStoryEnd()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Custom progress bars (replaces StoryProgressBar)
        StoryProgressIndicators(
            totalStories = stories.size,
            currentIndex = currentIndex,
            currentProgress = currentProgress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )

        // Navigation tap zones
        Row(modifier = Modifier.fillMaxSize()) {
            // Left tap (previous story)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex > 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(currentIndex - 1) }
                        }
                    }
            )
            // Center (can be used for pause/resume on long press)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            // Right tap (next story)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex < stories.size - 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(currentIndex + 1) }
                        }
                    }
            )
        }

        // Caption overlay for non‑text stories
        val currentStory = stories[currentIndex]
        if (!currentStory.content.isNullOrBlank() && currentStory.storyType.value != "text") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = currentStory.content!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StoryContent(
    story: Story,
    isPaused: Boolean,
    onVideoProgress: (Float) -> Unit,
    onVideoFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = story.storyType.value == "video"

    Box(modifier = modifier.background(Color.Black)) {
        if (isVideo && story.mediaUrl != null) {
            StoryVideoPlayer(
                videoUrl = story.mediaUrl!!,
                isPlaying = !isPaused,
                onVideoFinished = onVideoFinished,
                onProgressUpdate = onVideoProgress,
                modifier = Modifier.fillMaxSize()
            )
        } else if (story.storyType.value == "text") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.content ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        lineHeight = 32.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(story.mediaUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun StoryProgressIndicators(
    totalStories: Int,
    currentIndex: Int,
    currentProgress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(totalStories) { index ->
            val progress = when {
                index < currentIndex -> 1f
                index == currentIndex -> currentProgress
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
    }
}
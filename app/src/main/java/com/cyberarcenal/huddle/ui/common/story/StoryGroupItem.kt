package com.cyberarcenal.huddle.ui.common.story

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
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
    isPaused: Boolean = false,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int, PostStatsSerializers) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
    onReactionSummaryClick: (String, Int, PostStatsSerializers) -> Unit = onCommentClick,
    onCommentSummaryClick: (String, Int, PostStatsSerializers) -> Unit = onCommentClick,
    onGroupClick: (Int) -> Unit = {},
    onHeaderClick: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { stories.size })
    val currentStory = stories.getOrNull(pagerState.currentPage)

    FeedItemFrame(
        user = user,
        createdAt = createdAt,
        statistics = currentStory?.statistics ?: stories.firstOrNull()?.statistics,
        headerSuffix = "",
        caption = caption,
        onReactionClick = { reaction ->
            currentStory?.id?.let { id ->
                onReactionClick(
                    ReactionCreateRequest(
                        contentType = "stories.story",
                        objectId = id,
                        reactionType = reaction
                    )
                )
            }
        },
        onCommentClick = {
            currentStory?.let {
                onCommentClick("stories.story", it.id!!, it.statistics!!)
            }
        },
        onShareClick = onShareClick,
        onMoreClick = onMoreClick,
        onProfileClick = onProfileClick,
        onReactionSummaryClick = {
            currentStory?.let {
                onReactionSummaryClick("stories.story", it.id!!, it.statistics!!)
            }
        },
        onCommentSummaryClick = {
            currentStory?.let {
                onCommentSummaryClick("stories.story", it.id!!, it.statistics!!)
            }
        },
        postData = currentStory,
        showBottomDivider = true,
        content = {
            StoryGroupViewer(
                stories = stories,
                user = user,
                pagerState = pagerState,
                isPaused = isPaused,
                modifier = Modifier
                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
//                        shape = RoundedCornerShape(16.dp) ayoko ng rounded sa story wag mo
                        //                        itong gagalawin
                    )
            )
        },
        onHeaderClick = onHeaderClick,
        onGroupClick = onGroupClick,
        isPaused = isPaused
    )
}

@Composable
fun StoryGroupViewer(
    stories: List<Story>,
    user: UserMinimal?,
    pagerState: PagerState,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty()) return

    val context = LocalContext.current
    val currentIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val viewManager = remember { ViewManager(ViewsRepository(), coroutineScope) }

    // Visibility tracking - FIXED
    val configuration = LocalConfiguration.current
    val density = context.resources.displayMetrics.density
    val screenHeightPx = (configuration.screenHeightDp * density).toInt()
    var visiblePercentage by remember { mutableFloatStateOf(1f) }
    val visibilityThreshold = 0.2f

    var isAutoPaused by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    val effectiveIsPaused = isPaused || isAutoPaused

    // Update visibility when layout changes
    fun updateVisibility(bounds: Rect) {
        val visibleTop = maxOf(bounds.top, 0)
        val visibleBottom = minOf(bounds.bottom, screenHeightPx)
        val visibleHeight = maxOf(0, visibleBottom - visibleTop)
        val totalHeight = bounds.height()
        visiblePercentage = if (totalHeight > 0) visibleHeight.toFloat() / totalHeight else 0f
    }

    // Auto-pause when not enough visible
    LaunchedEffect(visiblePercentage) {
        isAutoPaused = visiblePercentage < visibilityThreshold
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

    LaunchedEffect(currentIndex, effectiveIsPaused) {
        val story = stories[currentIndex]
        val isVideo = story.storyType.value == "video"
        timerJob?.cancel()
        timerJob = null
        // Do NOT reset progress if just pausing/resuming
        // currentProgress = 0f // This was causing reset on pause

        if (!isVideo && !effectiveIsPaused) {
            val duration = 10000L
            val initialProgress = currentProgress
            val startTime = System.currentTimeMillis() - (initialProgress * duration).toLong()
            
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
            val bounds = Rect(
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
                isPaused = effectiveIsPaused || pagerState.isScrollInProgress,
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
                modifier = Modifier.fillMaxSize(),
            )
        } else if (story.storyType.value == "text") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.content ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
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
                    .height(2.dp)
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
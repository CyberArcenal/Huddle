package com.cyberarcenal.huddle.ui.common.story

import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
fun StoryGroupItem(
    user: UserMinimal?,
    stories: List<Story>,
    createdAt: OffsetDateTime?,
    caption: String? = null,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
) {
    FeedItemFrame(
        user = user,
        createdAt = createdAt,
        statistics = null,
        headerSuffix = "",
        caption = caption,
        onReactionClick = onReactionClick,
        onCommentClick = onCommentClick,
        onShareClick = onShareClick,
        onMoreClick = onMoreClick,
        onProfileClick = onProfileClick,
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

    val pagerState = rememberPagerState(pageCount = { stories.size })
    val currentIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val viewManager = remember { ViewManager(ViewsRepository(), coroutineScope) }

    // Pause flag (e.g., when user is scrolling or long‑press)
    var isPaused by remember { mutableStateOf(false) }

    // Progress value for the current story (0f..1f)
    var currentProgress by remember { mutableFloatStateOf(0f) }

    // Timer job for image/text stories
    var timerJob by remember { mutableStateOf<Job?>(null) }

    // Function to advance to next story
    val onStoryEnd = {
        if (currentIndex < stories.size - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(currentIndex + 1)
            }
        }
    }

    // Record view when story changes
    LaunchedEffect(currentIndex) {
        val currentStory = stories[currentIndex]
        if (currentStory.id != null && !currentStory.hasViewed!!) {
            viewManager.recordView(
                targetType = "story",
                targetId = currentStory.id,
                durationSeconds = 3
            )
        }
    }

    // Handle progress and auto‑advance based on media type
    LaunchedEffect(currentIndex, isPaused) {
        val story = stories[currentIndex]
        val isVideo = story.storyType.value == "video"

        // Cancel any previous timer job
        timerJob?.cancel()
        timerJob = null

        // Reset progress when story changes
        currentProgress = 0f

        if (isVideo) {
            // For videos, progress is updated by the player’s onProgressUpdate callback.
            // No timer needed; the video itself will call onStoryEnd when finished.
            // We only need to reset progress to 0 when paused? The player will handle.
        } else {
            // For images/text: run a timer
            if (!isPaused) {
                val duration = 10000L // 10 seconds for images and text stories
                val startTime = System.currentTimeMillis()
                timerJob = coroutineScope.launch {
                    while (currentProgress < 1f) {
                        val elapsed = System.currentTimeMillis() - startTime
                        currentProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                        if (currentProgress >= 1f) {
                            onStoryEnd()
                            break
                        }
                        delay(16) // smooth progress updates
                    }
                }
            }
        }
    }

    // Cleanup timer when story is disposed
    DisposableEffect(currentIndex) {
        onDispose {
            timerJob?.cancel()
        }
    }

    Box(modifier = modifier.aspectRatio(9f / 16f)) {
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
// HighlightCarouselScreen.kt
package com.cyberarcenal.huddle.ui.highlight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.data.models.HighlightCache
import com.cyberarcenal.huddle.data.repositories.ViewsRepository
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import com.cyberarcenal.huddle.ui.common.story.StoryViewerFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightCarouselScreen(
    startIndex: Int,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState,
    sessionId: String
) {
    val highlights = HighlightCache.retrieve(sessionId = sessionId) ?: emptyList()
    if (highlights.isEmpty()) {
        navController.popBackStack()
        return
    }

    val scope = rememberCoroutineScope()
    val viewManager = remember { ViewManager(ViewsRepository(), scope) }

    val viewModel = viewModel<HighlightCarouselViewModel>(
        factory = HighlightCarouselViewModelFactory(highlights, startIndex, viewManager)
    )

    val uiState by viewModel.uiState.collectAsState()
    val closeEvent by viewModel.closeEvent.collectAsState(initial = null)

    LaunchedEffect(closeEvent) {
        if (closeEvent != null) {
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            color = Color.DarkGray
        ) {
            when (val state = uiState) {
                is HighlightCarouselUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is HighlightCarouselUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.message, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                    }
                }
                is HighlightCarouselUiState.Success -> {
                    StoryViewerFrame(
                        story = state.currentStory,
                        totalStories = state.totalStoriesInCurrentHighlight,
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

sealed class HighlightCarouselUiState {
    object Loading : HighlightCarouselUiState()
    data class Error(val message: String) : HighlightCarouselUiState()
    data class Success(
        val currentStory: Story,
        val totalStoriesInCurrentHighlight: Int,
        val currentStoryIndex: Int
    ) : HighlightCarouselUiState()
}
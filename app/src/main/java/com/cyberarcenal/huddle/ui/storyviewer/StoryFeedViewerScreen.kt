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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.models.StoryViewerData
import com.cyberarcenal.huddle.data.repositories.ViewsRepository
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import com.cyberarcenal.huddle.ui.common.story.StoryViewerFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryFeedViewerScreen(
    startIndex: Int,
    navController: NavController
) {
    val storyFeeds = StoryViewerData.storyFeeds ?: emptyList()
    if (storyFeeds.isEmpty()) {
        // No stories — go back immediately
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    val scope = rememberCoroutineScope()
    val viewManager = remember { ViewManager(ViewsRepository(), scope) }

    val viewModel = viewModel<StoryFeedViewerViewModel>(
        factory = StoryFeedViewerViewModelFactory(storyFeeds, startIndex, viewManager)
    )

    val uiState by viewModel.uiState.collectAsState()
    val closeEvent by viewModel.closeEvent.collectAsState(initial = null)

    LaunchedEffect(closeEvent) {
        if (closeEvent != null) {
            navController.popBackStack()
        }
    }

    // Root container uses system bars padding and theme background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Surface that holds the viewer content; uses themed surface color
        Surface(
            modifier = Modifier
                .fillMaxSize(),
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

sealed class StoryFeedViewerUiState {
    object Loading : StoryFeedViewerUiState()
    data class Error(val message: String) : StoryFeedViewerUiState()
    data class Success(
        val currentStory: Story,
        val totalStoriesInCurrentUser: Int,
        val currentStoryIndex: Int,
        val currentUser: UserMinimal
    ) : StoryFeedViewerUiState()
}

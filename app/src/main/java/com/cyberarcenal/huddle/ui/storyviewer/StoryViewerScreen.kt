// StoryViewerScreen.kt
package com.cyberarcenal.huddle.ui.storyviewer

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
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import com.cyberarcenal.huddle.data.repositories.ViewsRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.StoryManager
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import com.cyberarcenal.huddle.ui.common.story.StoryViewerFrame
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    userId: Int?,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val actionState = remember { MutableStateFlow<ActionState>(ActionState.Idle) }

    val storyManager = remember {
        StoryManager(
            storiesRepository = StoriesRepository(),
            viewModelScope = coroutineScope,
            actionState = actionState
        )
    }

    val viewManager = remember { ViewManager(ViewsRepository(), coroutineScope) }

    val storyViewModel: StoryViewerViewModel = viewModel(
        factory = StoryViewerViewModelFactory(userId ?: 0, storyManager, viewManager)
    )

    val uiState by storyViewModel.uiState.collectAsState()
    val closeEvent by storyViewModel.closeEvent.collectAsState(initial = null)

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
                is StoryViewerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is StoryViewerUiState.Error -> {
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
                is StoryViewerUiState.Success -> {
                    val currentStory = state.stories.getOrNull(state.currentIndex)
                    if (currentStory != null) {
                        StoryViewerFrame(
                            story = currentStory,
                            totalStories = state.stories.size,
                            currentIndex = state.currentIndex,
                            onTapLeft = storyViewModel::previousStory,
                            onTapRight = storyViewModel::nextStory,
                            onMoreClick = storyViewModel::onMoreClick,
                            onCommentClick = storyViewModel::onCommentClick,
                            onReactionClick = storyViewModel::onReactionClick,
                            onShareClick = storyViewModel::onShareClick,
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
}
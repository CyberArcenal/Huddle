package com.cyberarcenal.huddle.ui.storyviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.data.repositories.stories.StoriesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    userId: Int?,
    navController: NavController,
    viewModel: StoryViewerViewModel = viewModel(
        factory = StoryViewerViewModelFactory(userId ?: 0, StoriesRepository())
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val closeEvent by viewModel.closeEvent.collectAsState(initial = null)

    // Handle close event
    LaunchedEffect(closeEvent) {
        if (closeEvent != null) {
            navController.popBackStack()
        }
    }

    // Ang main container ngayon ay may background color at padding para sa system bars
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Background sa labas ng viewer
            .systemBarsPadding() // Umiiwas sa status bar at nav bar
            .padding(horizontal = 8.dp, vertical = 8.dp) // Konting hangin sa gilid
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)), // Rounded corners para sa viewer
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
                        Text(text = state.message, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                    }
                }
                is StoryViewerUiState.Success -> {
                    val currentStory = state.stories.getOrNull(state.currentIndex)
                    if (currentStory != null) {
                        StoryContent(
                            story = currentStory,
                            onTapLeft = viewModel::previousStory,
                            onTapRight = viewModel::nextStory,
                            onClose = viewModel::close,
                            totalStories = state.stories.size,
                            currentIndex = state.currentIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryContent(
    story: Story,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onClose: () -> Unit,
    totalStories: Int,
    currentIndex: Int
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Media Content
        when (story.storyType) {
            StoryTypeEnum.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(story.mediaUrl?.toString())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            StoryTypeEnum.TEXT -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF212121)), contentAlignment = Alignment.Center) {
                    Text(
                        text = story.content ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            else -> { /* Video Placeholder */ }
        }

        // Navigation Taps
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTapLeft() })
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTapRight() })
        }

        // Overlay Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            // Progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalStories) { index ->
                    val progress = if (index < currentIndex) 1f else if (index == currentIndex) 0.5f else 0f
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.weight(1f).height(2.dp).clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Info & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile (Minimalist)
                val username = story.user?.username ?: "User"
                Text(
                    text = username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

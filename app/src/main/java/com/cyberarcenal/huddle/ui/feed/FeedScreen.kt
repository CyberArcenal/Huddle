package com.cyberarcenal.huddle.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.data.repositories.stories.StoriesRepository
import com.cyberarcenal.huddle.ui.home.components.PostItem
import com.cyberarcenal.huddle.ui.home.components.StoriesRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = viewModel()
) {
    val feedItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Stories state
    var stories by remember { mutableStateOf<List<StoryFeed>>(emptyList()) }
    var isLoadingStories by remember { mutableStateOf(false) }
    val storiesRepository = remember { StoriesRepository() }

    // Pull to Refresh State (Material 3)
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = feedItems.loadState.refresh is LoadState.Loading || isLoadingStories

    // Function to load stories
    suspend fun loadStories() {
        isLoadingStories = true
        storiesRepository.getFollowingStories()
            .onSuccess { stories = it }
            .onFailure { error ->
                snackbarHostState.showSnackbar("Failed to load stories: ${error.message}")
            }
        isLoadingStories = false
    }

    // Initial Load
    LaunchedEffect(Unit) {
        loadStories()
    }

    // Handle Like Events
    LaunchedEffect(Unit) {
        viewModel.likeEvents.collect { result ->
            when (result) {
                is LikeResult.Success -> {
                    // Refresh current page to update UI
                    feedItems.refresh()
                }
                is LikeResult.Error -> {
                    snackbarHostState.showSnackbar("Error: ${result.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Apply scaffold padding as content padding to LazyColumn,
        // while PullToRefreshBox fills the whole area (no outer padding)
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    feedItems.refresh()
                    loadStories()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues   // This ensures content respects top/bottom bars
            ) {
                // 1. Stories Header
                item {
                    if (stories.isNotEmpty()) {
                        StoriesRow(
                            stories = stories,
                            onStoryClick = { storyFeed ->
                                navController.navigate("story/${storyFeed.user?.id}")
                            }
                        )
                    } else if (isLoadingStories) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearProgressIndicator(modifier = Modifier.width(100.dp))
                        }
                    }
                }

                // 2. Feed Items (Gamit ang items count approach para iwas sa import error)
                items(
                    count = feedItems.itemCount,
                    key = feedItems.itemKey { it.id },
                    contentType = feedItems.itemContentType { "post" }
                ) { index ->
                    val post = feedItems[index]
                    post?.let {
                        PostItem(
                            post = it,
                            onLikeClick = { isLiked, count ->
                                viewModel.toggleLike(it.id, isLiked, count)
                            },
                            onCommentClick = {
                                navController.navigate("comments/${it.id}")
                            },
                            onMoreClick = {
                                // Options logic here
                            },
                            onProfileClick = {
                                navController.navigate("profile/${it.user?.id}")
                            }
                        )
                    }
                }

                // 3. Loading More (Pagination Append)
                if (feedItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // 4. Append Error
                val appendError = feedItems.loadState.append as? LoadState.Error
                if (appendError != null) {
                    item {
                        Button(
                            onClick = { feedItems.retry() },
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Text("Retry loading more")
                        }
                    }
                }
            }
        }

        // Full-screen Error Placeholder (First Load Only)
        if (feedItems.loadState.refresh is LoadState.Error && feedItems.itemCount == 0) {
            val error = (feedItems.loadState.refresh as LoadState.Error).error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Could not load feed: ${error.message}")
                    Button(onClick = { feedItems.retry() }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}
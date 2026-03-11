package com.cyberarcenal.huddle.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.cyberarcenal.huddle.ui.home.components.CreatePostRow
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
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Stories state
    var stories by remember { mutableStateOf<List<StoryFeed>>(emptyList()) }
    var isLoadingStories by remember { mutableStateOf(false) }
    val storiesRepository = remember { StoriesRepository() }

    // Pull to Refresh State
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = feedItems.loadState.refresh is LoadState.Loading || isLoadingStories

    // Handle Scroll to Top Event
    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }

    // Function to load stories
    suspend fun loadStories() {
        isLoadingStories = true
        // Ginamit ang getStoryFeed(includeOwn = true) para makita pati ang sariling stories
        storiesRepository.getStoryFeed(includeOwn = true)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                // 1. Stories Row
                item {
                    StoriesRow(
                        stories = stories,
                        onCreateStoryClick = {
                            navController.navigate("create_story")
                        },
                        onStoryClick = { storyFeed ->
                            navController.navigate("story/${storyFeed.user?.id}")
                        }
                    )
                }

                // 2. Create Post Row
                item {
                    CreatePostRow(
                        profilePictureUrl = null,
                        onRowClick = {
                            navController.navigate("create_post")
                        }
                    )
                }

                // 3. Feed Items
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
                            onProfileClick = {
                                navController.navigate("profile/${it.user?.id}")
                            }
                        )
                    }
                }

                if (feedItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

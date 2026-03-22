package com.cyberarcenal.huddle.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.feed.FeedType
import com.cyberarcenal.huddle.ui.feed.FeedViewModel
import com.cyberarcenal.huddle.ui.feed.FeedViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(navController: NavController) {
    // Tabs definition
    val tabs = listOf("For You", "Following", "Discover")

    // Pager state for swiping between tabs
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Repositories (ideally injected via DI)
    val postRepo = remember { UserPostsRepository() }
    val commentRepo = remember { CommentsRepository() }
    val reactionRepo = remember { UserReactionsRepository() }
    val storyRepo = remember { StoriesRepository() }
    val feedRepo = remember { FeedRepository() }

    Scaffold(
        topBar = {
            // Use ScrollableTabRow for horizontal scrolling
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                edgePadding = 0.dp // optional, remove extra padding at edges
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondViewportPageCount = 1
        ) { page ->
            // Map tab title to FeedType (adjust as needed)
            val feedType = when (tabs[page]) {
                "For You" -> FeedType.HOME
                "Following" -> FeedType.FOLLOWING
                "Discover" -> FeedType.DISCOVER
                else -> FeedType.HOME
            }

            val viewModel: FeedViewModel = viewModel(
                key = "feed_vm_${feedType.name}",
                factory = FeedViewModelFactory(
                    feedType = feedType,
                    postRepository = postRepo,
                    commentRepository = commentRepo,
                    reactionsRepository = reactionRepo,
                    storyFeedRepository = storyRepo,
                    feedRepository = feedRepo,
                )
            )

            FeedScreen(
                navController = navController,
                feedType = feedType,
                viewModel = viewModel
            )
        }
    }
}
package com.cyberarcenal.huddle.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.feed.FeedType
import com.cyberarcenal.huddle.ui.feed.FeedViewModel
import com.cyberarcenal.huddle.ui.feed.FeedViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun HomeTabbedFeed(navController: NavController) {
    val tabs = listOf("Home", "Discover", "Friends", "Following", "Groups")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Replace TabRow with ScrollableTabRow
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else Color.Gray
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val feedType = when (page) {
                0 -> FeedType.HOME // 'Friends' logic in backend
                1 -> FeedType.DISCOVER
                2 -> FeedType.FRIENDS
                3 -> FeedType.FOLLOWING
                4 -> FeedType.GROUPS
                else -> FeedType.HOME
            }

            val viewModel: FeedViewModel = viewModel(
                key = "feed_${feedType.name}",
                factory = FeedViewModelFactory(
                    postRepository = UserPostsRepository(),
                    commentRepository = CommentsRepository(),
                    reactionsRepository = UserReactionsRepository(),
                    storyFeedRepository = StoriesRepository(),
                    feedType = feedType,
                    feedRepository = FeedRepository(),
                    sharePostsRepository = SharePostsRepository()
                )
            )

            FeedScreen(
                navController = navController,
                viewModel = viewModel,
                feedType = feedType
            )
        }
    }
}
package com.cyberarcenal.huddle.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.feed.FeedScreen
import com.cyberarcenal.huddle.ui.feed.FeedViewModel
import com.cyberarcenal.huddle.ui.feed.FeedViewModelFactory
import com.cyberarcenal.huddle.ui.feed.dataclass.FeedType
import kotlinx.coroutines.launch

import com.cyberarcenal.huddle.ui.live.LiveListScreen
import com.cyberarcenal.huddle.ui.live.LiveViewModel
import com.cyberarcenal.huddle.ui.live.LiveViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun HomeTabbedFeed(
    navController: NavController,
    homeViewModel: HomeViewModel,
    globalSnackbarHostState: SnackbarHostState
) {
    val tabs = listOf("Live", "Home", "Discover", "Friends", "Following", "Groups")
    val pagerState = rememberPagerState(
        initialPage = 1, // "Home" is at index 1
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Track view models for each page
    val viewModels = remember { mutableMapOf<Int, FeedViewModel>() }

    LaunchedEffect(Unit) {
        homeViewModel.feedRefreshRequest.collect {
            // Refresh and scroll only the current active page's view model
            viewModels[pagerState.currentPage]?.let { vm ->
                vm.refreshFeed()
                vm.requestScrollToTop()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Replace TabRow with ScrollableTabRow
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
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { 
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) 
                        }
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
            if (page == 0) {
                LiveListScreen(
                    viewModel = viewModel(
                        factory = LiveViewModelFactory(
                            LiveRepository(),
                            CommentsRepository(),
                            ReactionsRepository()
                        )
                    ),
                    navController = navController
                )
            } else {
                val feedType = when (page) {
                    1 -> FeedType.HOME
                    2 -> FeedType.DISCOVER
                    3 -> FeedType.FRIENDS
                    4 -> FeedType.FOLLOWING
                    5 -> FeedType.GROUPS
                    else -> FeedType.HOME
                }

                val viewModel: FeedViewModel = viewModel(
                    key = "feed_${feedType.name}",
                    factory = FeedViewModelFactory(
                        feedType = feedType,
                        postRepository = UserPostsRepository(),
                        feedRepository = FeedRepository(LocalContext.current),
                        commentRepository = CommentsRepository(),
                        reactionsRepository = ReactionsRepository(),
                        storyFeedRepository = StoriesRepository(context = LocalContext.current),
                        sharePostsRepository = SharePostsRepository(),
                        followRepository = FollowRepository(),
                        userMediaRepository = UserMediaRepository(),
                        groupRepository = GroupRepository(),
                    )
                )

                // Register the view model for this page
                SideEffect {
                    viewModels[page] = viewModel
                }

                FeedScreen(
                    navController = navController,
                    viewModel = viewModel,
                    feedType = feedType,
                    globalSnackbarHostState = globalSnackbarHostState,
                )
            }
        }
    }
}
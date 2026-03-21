package com.cyberarcenal.huddle.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    // 1. Define tabs
    val tabs = listOf("For You", "Following", "Discover")

    // 2. Setup Pager State
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // 3. Repositories (Ideally, use Hilt to inject these)
    val postRepo = remember { UserPostsRepository() }
    val commentRepo = remember { CommentsRepository() }
    val reactionRepo = remember { UserReactionsRepository() }
    val storyRepo = remember { StoriesRepository() }
    val feedRepo = remember { FeedRepository() }


    Scaffold(
        topBar = {
            // TabRow sa loob ng TopAppBar area para malinis
            Column {
                TabRow(
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
                    }
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
        }
    ) { paddingValues ->
        // 4. HorizontalPager para sa content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondViewportPageCount = 1 // Load adjacent pages for smoother experience
        ) { page ->

            // 5. Determine FeedType base sa page index
            val feedType = when (tabs[page]) {
                "Home" -> FeedType.HOME    // O kung ano man ang tawag mo sa home feed sa backend
                "Discover" -> FeedType.DISCOVER
                "Friends" -> FeedType.FRIENDS // Siguraduhin na may FRIENDS sa iyong FeedType enum
                "Following" -> FeedType.FOLLOWING
                "Groups" -> FeedType.GROUPS   // Siguraduhin na may GROUPS sa iyong FeedType enum
                else -> FeedType.HOME
            }


            /**
             * IMPORTANT: Gumagamit tayo ng key sa 'viewModel' para magkaroon ng
             * hiwalay na instance ang bawat tab. Kung hindi, maghahalo ang posts.
             */
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
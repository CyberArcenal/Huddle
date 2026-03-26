package com.cyberarcenal.huddle.ui.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import com.cyberarcenal.huddle.ui.common.user.UserItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    userId: Int? = null,
    viewModel: FriendsViewModel = viewModel(
        factory = FriendsViewModelFactory(
            userId = userId,
            userFollowRepository = FollowRepository(),
            userProfileRepository = UsersRepository()
        )
    )
) {
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val followingOverrides by viewModel.followingOverrides.collectAsState()
    val loadingUsers by viewModel.loadingUsers.collectAsState()
    val tabs = FriendsTab.entries

    // Pager state for swiping
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Sync pager state with ViewModel's selected tab when user taps a tab
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.scrollToPage(selectedTabIndex)
        }
    }

    // Sync ViewModel's selected tab when user swipes
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTabIndex != pagerState.currentPage) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    // Collect paging data for each tab
    val followersItems = viewModel.followersFlow.collectAsLazyPagingItems()
    val followingItems = viewModel.followingFlow.collectAsLazyPagingItems()
    val mootsItems = viewModel.mootsFlow.collectAsLazyPagingItems()
    val suggestionsItems = viewModel.suggestionsFlow.collectAsLazyPagingItems()
    val matchesItems = viewModel.matchesFlow.collectAsLazyPagingItems()
    val popularItems = viewModel.popularFlow.collectAsLazyPagingItems()

    val tabItemsMap = mapOf(
        FriendsTab.FOLLOWERS to followersItems,
        FriendsTab.FOLLOWING to followingItems,
        FriendsTab.MOOTS to mootsItems,
        FriendsTab.SUGGESTIONS to suggestionsItems,
        FriendsTab.MATCHES to matchesItems,
        FriendsTab.POPULAR to popularItems
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) },
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                            viewModel.selectTab(index)
                        }
                    },
                    text = {
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                )
            }
        }

        // HorizontalPager for swipeable content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentItems = tabItemsMap[tabs[page]] ?: followersItems

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(count = currentItems.itemCount) { index ->
                        val user = currentItems[index]
                        user?.let {
                            val isFollowing = followingOverrides[it.id] ?: (it.isFollowing == true)
                            val isLoading = loadingUsers[it.id] ?: false

                            UserItem(
                                user = it,
                                onItemClick = {
                                    navController.navigate("profile/${it.id}")
                                },
                                isVertical = false,
                                onFollowClick = { viewModel.toggleFollow(it.id!!, isFollowing) },
                                isFollowing = isFollowing,
                                isLoading = isLoading
                            )
                        }
                    }

                    // Handle loading and empty states
                    when (val state = currentItems.loadState.refresh) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Error: ${state.error.message}", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { currentItems.refresh() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                            }
                        }
                        is LoadState.NotLoading -> {
                            if (currentItems.itemCount == 0) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize().padding(top = 60.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (currentItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
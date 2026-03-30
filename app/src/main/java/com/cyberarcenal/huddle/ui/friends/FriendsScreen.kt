package com.cyberarcenal.huddle.ui.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.user.UserItem
import kotlinx.coroutines.launch

enum class MainFriendsTab(val displayName: String) {
    FRIENDSHIP("FriendShip"),
    REQUESTS("Requests"),
    SUGGESTIONS("Suggestions"),
    FOLLOWERS("Followers"),
    FOLLOWING("Following")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    userId: Int? = null,
    viewModel: FriendshipViewModel = viewModel(
        factory = FriendshipViewModelFactory(
            friendshipRepository = FriendshipsRepository(),
            followRepository = FollowRepository(),
            matchingRepository = UserMatchingRepository(),
            searchRepository = UserSearchRepository()
        )
    )
) {
    val selectedTabIndex by viewModel.selectedTab.collectAsState()
    val tabs = MainFriendsTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Sync pager with ViewModel
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedTabIndex != pagerState.currentPage) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
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
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        viewModel.selectTab(index)
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 12.dp)
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
            val uiState by viewModel.uiState.collectAsState()
            
            when (tabs[page]) {
                MainFriendsTab.FRIENDSHIP -> {
                    when (uiState) {
                        is FriendshipUiState.Success -> ConnectionsTab(uiState as FriendshipUiState.Success, viewModel, navController)
                        is FriendshipUiState.Loading -> LoadingState()
                        is FriendshipUiState.Error -> ErrorState((uiState as FriendshipUiState.Error).message) { viewModel.loadAllData() }
                    }
                }
                MainFriendsTab.REQUESTS -> {
                    when (uiState) {
                        is FriendshipUiState.Success -> RequestsTab(uiState as FriendshipUiState.Success, viewModel, navController)
                        is FriendshipUiState.Loading -> LoadingState()
                        is FriendshipUiState.Error -> ErrorState((uiState as FriendshipUiState.Error).message) { viewModel.loadAllData() }
                    }
                }
                MainFriendsTab.SUGGESTIONS -> {
                    when (uiState) {
                        is FriendshipUiState.Success -> SuggestionsTab(uiState as FriendshipUiState.Success, viewModel, navController)
                        is FriendshipUiState.Loading -> LoadingState()
                        is FriendshipUiState.Error -> ErrorState((uiState as FriendshipUiState.Error).message) { viewModel.loadAllData() }
                    }
                }
                MainFriendsTab.FOLLOWERS -> {
                    FollowersTabPage(userId, viewModel, navController)
                }
                MainFriendsTab.FOLLOWING -> {
                    FollowingTabPage(userId, viewModel, navController)
                }
            }
        }
    }
}

@Composable
fun FollowersTabPage(userId: Int?, viewModel: FriendshipViewModel, navController: NavController) {
    val followRepository = remember { FollowRepository() }
    val matchingRepository = remember { UserMatchingRepository() }
    
    val followersFlow = remember(userId) {
        Pager(PagingConfig(pageSize = 20)) {
            FriendsPagingSource(followRepository, matchingRepository, userId, FriendsTab.FOLLOWERS)
        }.flow
    }.collectAsLazyPagingItems()

    UserPagingList(followersFlow, viewModel, navController)
}

@Composable
fun FollowingTabPage(userId: Int?, viewModel: FriendshipViewModel, navController: NavController) {
    val followRepository = remember { FollowRepository() }
    val matchingRepository = remember { UserMatchingRepository() }
    
    val followingFlow = remember(userId) {
        Pager(PagingConfig(pageSize = 20)) {
            FriendsPagingSource(followRepository, matchingRepository, userId, FriendsTab.FOLLOWING)
        }.flow
    }.collectAsLazyPagingItems()

    UserPagingList(followingFlow, viewModel, navController)
}

@Composable
fun UserPagingList(
    items: LazyPagingItems<UserMinimal>,
    viewModel: FriendshipViewModel,
    navController: NavController
) {
    val followStatuses by viewModel.followManager.followStatuses.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items.itemCount) { index ->
            items[index]?.let { user ->
                user.id?.let { userId ->
                    UserItem(
                        user = user,
                        isVertical = false,
                        onFollowClick = {
                            viewModel.followManager.toggleFollow(
                                userId,
                                followStatuses[userId] ?: user.isFollowing ?: false,
                                user.username ?: ""
                            )
                        },
                        onItemClick = { navController.navigate("profile/$userId") },
                        isFollowing = followStatuses[userId] ?: user.isFollowing ?: false,
                        isLoading = false
                    )
                }
            }
        }

        // Loading and Error states for Paging
        when (items.loadState.append) {
            is LoadState.Loading -> {
                item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
            }
            is LoadState.Error -> {
                item { 
                    Button(onClick = { items.retry() }, Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Retry Loading More")
                    }
                }
            }
            else -> {}
        }
        
        if (items.loadState.refresh is LoadState.Loading) {
            item { Box(Modifier.fillParentMaxSize(), Alignment.Center) { CircularProgressIndicator() } }
        }
    }
}

class FriendshipViewModelFactory(
    private val friendshipRepository: FriendshipsRepository,
    private val followRepository: FollowRepository,
    private val matchingRepository: UserMatchingRepository,
    private val searchRepository: UserSearchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendshipViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendshipViewModel(friendshipRepository, followRepository, matchingRepository, searchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.cyberarcenal.huddle.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cyberarcenal.huddle.data.repositories.FollowViewsRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import com.cyberarcenal.huddle.ui.common.UserItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    userId: Int? = null,
    viewModel: FriendsViewModel = viewModel(
        factory = FriendsViewModelFactory(
            userId = userId,
            userFollowRepository = FollowViewsRepository(),
            userProfileRepository = UsersRepository()
        )
    )
) {
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val followingOverrides by viewModel.followingOverrides.collectAsState()
    val tabs = FriendsTab.entries

    // Collect paging data for each tab
    val followersItems = viewModel.followersFlow.collectAsLazyPagingItems()
    val followingItems = viewModel.followingFlow.collectAsLazyPagingItems()
    val mootsItems = viewModel.mootsFlow.collectAsLazyPagingItems()
    val suggestionsItems = viewModel.suggestionsFlow.collectAsLazyPagingItems()
    val popularItems = viewModel.popularFlow.collectAsLazyPagingItems()

    val tabItemsMap = mapOf(
        FriendsTab.FOLLOWERS to followersItems,
        FriendsTab.FOLLOWING to followingItems,
        FriendsTab.MOOTS to mootsItems,
        FriendsTab.SUGGESTIONS to suggestionsItems,
        FriendsTab.POPULAR to popularItems
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f)) },
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
                    onClick = { viewModel.selectTab(index) },
                    text = { 
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) 
                    }
                )
            }
        }

        // List Content
        val currentItems = tabItemsMap[tabs[selectedTabIndex]] ?: followersItems
        
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(count = currentItems.itemCount) { index ->
                    val user = currentItems[index]
                    user?.let {
                        // Optimistic UI check
                        val isFollowing = followingOverrides[it.id] ?: (it.isFollowing == true)
                        
                        UserItem(
                            user = it as UserMinimal,
                            onItemClick = {
                                navController.navigate("profile/${it.id}")
                            },
                            isVertical = false,
                            onFollowClick = {}
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
                                    Text("Error: ${state.error.message}", color = Color.Gray, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { currentItems.refresh() },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Retry")
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
                                    Text("No users found", color = Color.Gray, fontSize = 15.sp)
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

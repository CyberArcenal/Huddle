// ProfileScrollContent.kt

package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.SubcomposeAsyncImage
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.models.StoryViewerData
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.shimmer.ShimmerFeedItem
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect
import com.cyberarcenal.huddle.ui.highlight.components.HighlightCard

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScrollContent(
    navController: NavController,
    profile: UserProfile,
    isCurrentUser: Boolean,
    userContent: LazyPagingItems<UnifiedContentItem>,
    likedItems: LazyPagingItems<UnifiedContentItem>,
    mediaItems: LazyPagingItems<UserMediaItem>? = null,
    storyHighlights: List<StoryHighlight>,
    listState: LazyListState,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int, stats: PostStatsSerializers?) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onImageClick: (MediaDetailData) -> Unit,
    onAvatarClick: (MediaDetailData) -> Unit,
    onCoverClick: (MediaDetailData) -> Unit,
    onEditProfilePicture: () -> Unit,
    onEditCoverPhoto: () -> Unit,
    onFollowClick: (UserMinimal) -> Unit,
    onRemoveProfilePicture: () -> Unit,
    onRemoveCoverPhoto: () -> Unit,
    onFollowToggle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    onMoreClick: (Any) -> Unit,
    onAddHighlightClick: () -> Unit,
    followStatus: FollowStatusResponseData?,
    followStats: FollowStatsResponse?,
    onHighlightClick: (StoryHighlight) -> Unit,


    followStatuses: Map<Int, Boolean>,
    loadingUsers: Map<Int, Boolean>,

    recentMoots: List<UserMinimal> = emptyList(),

    groupMembershipStatuses: Map<Int, Boolean>,
    joiningGroupIds: Map<Int, Boolean>,
) {
    val tabs = listOf("Posts", "Photos", "Reels", "Groups", "About")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- HEADER ---
        item(key = "profile_header") {
            ProfileFixedHeader(
                navController = navController,
                profile = profile,
                isCurrentUser = isCurrentUser,
                onAvatarClick = onAvatarClick,
                onCoverClick = onCoverClick,
                onEditProfilePicture = onEditProfilePicture,
                onEditCoverPhoto = onEditCoverPhoto,
                onRemoveProfilePicture = onRemoveProfilePicture,
                onRemoveCoverPhoto = onRemoveCoverPhoto,
                onFollowToggle = onFollowToggle,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToEditProfile = onNavigateToEditProfile,
                onNavigateBack = onNavigateBack,
                onAddHighlightClick = onAddHighlightClick,
                followStatus = followStatus,
                followStats = followStats,
                recentMoots = recentMoots
            )
        }


        // --- STORY HIGHLIGHTS ---
        item(key = "story_highlights") {
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color.White)
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add New Highlight
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(62.dp).clip(CircleShape)
                                .background(Color(0xFFF0F2F5)).clickable { onAddHighlightClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Inside the LazyRow in ProfileScrollContent
                items(storyHighlights) { highlight ->
                    HighlightCard(
                        highlight = highlight, onClick = {
                            StoryViewerData.highlights = storyHighlights
                            val index = storyHighlights.indexOf(highlight)
                            navController.navigate("highlight_carousel/$index")
                        })
                }
            }
        }

        // --- STICKY TABS ---
        stickyHeader(key = "profile_tabs") {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = Color.White,
                divider = { HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE4E6EB)) }) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        })
                }
            }
        }

        // --- TAB CONTENT ---
        when (selectedTabIndex) {
            0 -> { // Posts Tab
                if (userContent.loadState.refresh is LoadState.Loading && userContent.itemCount == 0) {
                    items(3) { ShimmerFeedItem() }
                } else if (userContent.itemCount == 0 && userContent.loadState.refresh is LoadState.NotLoading) {
                    item(key = "content_empty") {
                        EmptyStatePlaceholder(text = "No posts to display")
                    }
                } else {
                    items(
                        count = userContent.itemCount, key = { index ->
                            val item = userContent[index]
                            if (item != null) "${item.type}_${index}" else "placeholder_$index"
                        }) { index ->
                        val item = userContent[index]
                        item?.let {
                            UnifiedFeedRow(
                                row = it,
                                navController = navController,
                                onReactionClick = onReactionClick,
                                onCommentClick = onCommentClick,
                                onMoreClick = onMoreClick,
                                onImageClick = onImageClick,
                                onGroupJoinClick = {},
                                onFollowClick = onFollowClick,
                                onShare = onShareClick,
                                followStatuses = followStatuses,
                                loadingUsers = loadingUsers,
                                groupMembershipStatuses = groupMembershipStatuses,
                                joiningGroupIds = joiningGroupIds
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F2F5))
                        }
                    }

                    if (userContent.loadState.append is LoadState.Loading) {
                        items(1) { ShimmerFeedItem() }
                    }
                }
            }

            1 -> { // Photos Tab
                if (mediaItems == null) {
                    item { NoMediaPlaceholder() }
                } else if (mediaItems.loadState.refresh is LoadState.Loading && mediaItems.itemCount == 0) {
                    item {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth()
                                .height(600.dp), // Using a fixed height to avoid infinite height constraints
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalItemSpacing = 4.dp
                        ) {
                            items(6) { MediaGridShimmerItem() }
                        }
                    }
                } else if (mediaItems.itemCount == 0 && mediaItems.loadState.refresh is LoadState.NotLoading) {
                    item { NoMediaPlaceholder() }
                } else {
                    // We render the staggered grid as a single item using its own scrolling
                    // Or we could use the renderMediaGrid helper if it supported staggered.
                    // To keep it simple and truly Pinterest-style within the parent LazyColumn,
                    // we'll use a fixed height or similar approach, but the best way in Compose for nested
                    // scrolling is usually to not nest scrollables.
                    // However, given the requirement, I'll implement it within the item block.
                    item {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth()
                                .height(1000.dp), // Height should ideally be dynamic or use fillParentMaxHeight if inside HorizontalPager
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalItemSpacing = 4.dp
                        ) {
                            items(mediaItems.itemCount) { index ->
                                val media = mediaItems[index]
                                media?.let {
                                    val imageUrl = it.url?.toString() ?: ""
                                    val thumbnailUrl = it.thumbnail?.toString() ?: imageUrl

                                    Box(
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                            .clip(RoundedCornerShape(4.dp)).clickable {
                                                onImageClick(
                                                    MediaDetailData(
                                                        url = imageUrl,
                                                        user = null,
                                                        createdAt = it.createdAt,
                                                        stats = null,
                                                        id = it.contentId,
                                                        type = it.contentType
                                                    )
                                                )
                                            }) {
                                        SubcomposeAsyncImage(
                                            model = thumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.FillWidth,
                                            loading = {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .aspectRatio(1f).shimmerEffect()
                                                )
                                            },
                                            error = {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .background(Color.LightGray),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.BrokenImage,
                                                        contentDescription = "Error loading image",
                                                        tint = Color.Gray
                                                    )
                                                }
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            4 -> { // About Tab
                item {
                    ProfileAboutTab(profile)
                }
            }
            // Other tabs can be added here
            else -> {
                item(key = "tab_coming_soon") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text("Coming soon...")
                    }
                }
            }
        }
    }
}


fun LazyListScope.renderMediaGrid(
    mediaItems: LazyPagingItems<UserMediaItem>, onImageClick: (MediaDetailData) -> Unit
) {
    val rowCount = (mediaItems.itemCount + 2) / 3
    items(
        count = rowCount, key = { rowIndex -> "media_row_$rowIndex" }) { rowIndex ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (col in 0..2) {
                val itemIndex = rowIndex * 3 + col
                if (itemIndex < mediaItems.itemCount) {
                    val media = mediaItems[itemIndex]
                    media?.let {
                        val imageUrl = it.url?.toString() ?: ""
                        val thumbnailUrl = it.thumbnail?.toString() ?: imageUrl

                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp)).clickable {
                                    onImageClick(
                                        MediaDetailData(
                                            url = imageUrl,
                                            user = null,
                                            createdAt = it.createdAt,
                                            stats = null,
                                            id = it.contentId,
                                            type = it.contentType
                                        )
                                    )
                                }) {
                            SubcomposeAsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().shimmerEffect()
                                    )
                                },
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BrokenImage,
                                            contentDescription = "Error loading image",
                                            tint = Color.Gray
                                        )
                                    }
                                })
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

@Composable
private fun NoMediaPlaceholder() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No media yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Photos and videos shared will appear here",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MediaGridShimmerItem() {
    Box(
        modifier = Modifier.aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(4.dp))
            .shimmerEffect()
    )
}



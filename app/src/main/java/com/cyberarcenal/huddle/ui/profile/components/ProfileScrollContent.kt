// ProfileScrollContent.kt

package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.shimmer.ShimmerFeedItem
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect
import com.cyberarcenal.huddle.ui.feed.safeConvertTo
import com.cyberarcenal.huddle.utils.formatRelativeTime
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

data class OnlyIdData(
    val id: Int,
)

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
    onCommentClick: (String, Int) -> Unit,                    // contentType, objectId
    onShareClick: (ShareRequestData) -> Unit,                      // contentType, objectId
    onImageClick: (MediaDetailData) -> Unit,
    onAvatarClick: (MediaDetailData) -> Unit,
    onCoverClick: (MediaDetailData) -> Unit,
    onEditProfilePicture: () -> Unit,
    onEditCoverPhoto: () -> Unit,

    onRemoveProfilePicture: () -> Unit,
    onRemoveCoverPhoto: () -> Unit,
    onFollowToggle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    onMoreClick: (Any) -> Unit,
    onAddHighlightClick: () -> Unit,
) {
    val tabs = listOf("Posts", "Photos", "Reels", "Groups", "About")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- HEADER ---
        item(key = "profile_header") {
            ProfileFixedHeader(
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
            )
        }

        // --- STORY HIGHLIGHTS ---
        item(key = "story_highlights") {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add New Highlight
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F2F5))
                                .clickable { onAddHighlightClick() },
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

                // List of Highlights
                items(storyHighlights) { highlight ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = highlight.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFE4E6EB), CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = highlight.title?: "",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // --- STICKY TABS ---
        stickyHeader(key = "profile_tabs") {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp,
                containerColor = Color.White,
                divider = { HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE4E6EB)) },
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
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    )
                }
            }
        }

        // --- TAB CONTENT ---
        when (pagerState.currentPage) {
            0 -> { // Posts Tab
                if (userContent.loadState.refresh is LoadState.Loading && userContent.itemCount == 0) {
                    items(3) { ShimmerFeedItem() }
                } else if (userContent.itemCount == 0 && userContent.loadState.refresh is LoadState.NotLoading) {
                    item(key = "content_empty") {
                        EmptyStatePlaceholder(text = "No posts to display")
                    }
                } else {
                    renderUnifiedFeedList(
                        userContent = userContent,
                        navController = navController,
                        onReactionClick = onReactionClick,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick,
                        onImageClick = onImageClick,
                        onMoreClick = onMoreClick
                    )

                    if (userContent.loadState.append is LoadState.Loading) {
                        items(1) { ShimmerFeedItem() }
                    }
                }
            }
            1 -> { // Photos Tab
                if (mediaItems == null) {
                    item { NoMediaPlaceholder() }
                } else if (mediaItems.loadState.refresh is LoadState.Loading && mediaItems.itemCount == 0) {
                    items(6) { MediaGridShimmerItem() }
                } else if (mediaItems.itemCount == 0 && mediaItems.loadState.refresh is LoadState.NotLoading) {
                    item { NoMediaPlaceholder() }
                } else {
                    renderMediaGrid(mediaItems, onImageClick)

                    if (mediaItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
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

// Helper function to render the feed list
fun LazyListScope.renderUnifiedFeedList(
    userContent: LazyPagingItems<UnifiedContentItem>,
    navController: NavController,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onImageClick: (MediaDetailData) -> Unit,
    onMoreClick: (Any) -> Unit
) {
    items(
        count = userContent.itemCount,
        key = { index ->
            val item = userContent[index]
            if (item != null) "${item.type}_${index}" else "placeholder_$index"
        }
    ) { index ->
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
                onFollowClick = {},
                onShare = onShareClick
            )
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F2F5))
        }
    }
}

// Helper to render media grid
fun LazyListScope.renderMediaGrid(
    mediaItems: LazyPagingItems<UserMediaItem>,
    onImageClick: (MediaDetailData) -> Unit
) {
    val rowCount = (mediaItems.itemCount + 2) / 3
    items(
        count = rowCount,
        key = { rowIndex -> "media_row_$rowIndex" }
    ) { rowIndex ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (col in 0..2) {
                val itemIndex = rowIndex * 3 + col
                if (itemIndex < mediaItems.itemCount) {
                    val media = mediaItems[itemIndex]
                    media?.let {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onImageClick(
                                        MediaDetailData(
                                            url = it.url?.toString() ?: "",
                                            user = null,
                                            createdAt = it.createdAt,
                                            stats = null,
                                            id = it.contentId,
                                            type = it.contentType
                                        )
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = it.thumbnail?.toString() ?: it.url?.toString(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
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
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.TopCenter
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
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
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .shimmerEffect()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHighlightSheet(
    stories: List<Story>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Int>) -> Unit,
    isCreating: Boolean
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var title by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "New Highlight",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Highlight Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Select Stories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (stories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent stories found", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(stories) { story ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val id = story.id
                                    if (id != null) {
                                        selectedIds = if (id in selectedIds) {
                                            selectedIds.minus(id)
                                        } else {
                                            selectedIds.plus(id)
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = story.id in selectedIds,
                                onCheckedChange = null
                            )
                            AsyncImage(
                                model = story.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = story.content?.take(30) ?: "Story",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatRelativeDate(story.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(title.ifBlank { "Highlight" }, selectedIds.toList()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedIds.isNotEmpty() && !isCreating,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Create Highlight")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatRelativeDate(dateTime: OffsetDateTime?): String {
    if (dateTime == null) return ""
    val now = OffsetDateTime.now()
    val diff = now.toInstant().toEpochMilli() - dateTime.toInstant().toEpochMilli()
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> "${days / 7} weeks ago"
    }
}

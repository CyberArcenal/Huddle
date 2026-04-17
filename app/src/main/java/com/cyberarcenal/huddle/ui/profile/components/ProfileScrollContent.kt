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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.cyberarcenal.huddle.data.models.HighlightCache
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.models.StoryFeedCache
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.feed.UnifiedFeedRow
import com.cyberarcenal.huddle.ui.common.shimmer.ShimmerFeedItem
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect
import com.cyberarcenal.huddle.ui.highlight.components.HighlightCard
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import android.util.Log
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

inline fun <reified T> safeConvertTo(item: Any, tag: String = "Convert"): T? {
    return try {
        val gson = GsonBuilder().registerTypeAdapter(
            OffsetDateTime::class.java, JsonDeserializer { json, _, _ ->
                OffsetDateTime.parse(json.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }).create()
        val json = gson.toJson(item)
        gson.fromJson(json, T::class.java)
    } catch (e: Exception) {
        Log.e(tag, "Failed to convert item to ${T::class.simpleName}: ${e.message}", e)
        null
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScrollContent(
    navController: NavController,
    profile: UserProfile,
    isCurrentUser: Boolean,
    userContent: LazyPagingItems<UnifiedContentItem>,
    posts: LazyPagingItems<UnifiedContentItem>,
    photos: LazyPagingItems<UserMediaItem>,
    videos: LazyPagingItems<UserMediaItem>,
    likedItems: LazyPagingItems<UnifiedContentItem>,
    reelItems: List<ReelDisplay> = emptyList(),
    isReelsLoading: Boolean = false,
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
    onVideoClick: (PostFeed, String) -> Unit = { _, _ -> },
    onAddHighlightClick: () -> Unit,
    onFilterChange: (String?) -> Unit,
    selectedFilter: String?,
    followStatus: FollowStatusResponseData?,
    followStats: FollowStatsResponse?,
    onHighlightClick: (StoryHighlight) -> Unit,
    isPaused: Boolean = false,


    followStatuses: Map<Int, Boolean>,
    loadingUsers: Map<Int, Boolean>,

    recentMoots: List<UserMinimal> = emptyList(),

    groupMembershipStatuses: Map<Int, Boolean>,
    joiningGroupIds: Map<Int, Boolean>,
    onPersonalityClick: (String) -> Unit = {}
) {
    val tabs = listOf("Posts", "Photos", "Videos", "Reels", "About")
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
                recentMoots = recentMoots,
                onPersonalityClick = onPersonalityClick
            )
        }


        // --- STORY HIGHLIGHTS ---
        item(key = "story_highlights") {
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add New Highlight
                if (isCurrentUser) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .shadow(8.dp, RoundedCornerShape(30.dp))
                                    .background(Color(0xFFF0F2F5))
                                    .clickable { onAddHighlightClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                // Inside the LazyRow in ProfileScrollContent
                items(storyHighlights) { highlight ->
                    HighlightCard(
                        highlight = highlight, onClick = {
                            val sessionId = UUID.randomUUID().toString()
                            HighlightCache.store(sessionId, storyHighlights)
                            val index = storyHighlights.indexOf(highlight)
                            navController.navigate("highlight_carousel/$index/$sessionId")
                        })
                }
            }
        }

        // --- STICKY TABS ---
        stickyHeader(key = "profile_tabs") {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {
                    HorizontalDivider(
                        thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant
                    )
                }) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        })
                }
            }
        }

        // --- TAB CONTENT ---
        when (selectedTabIndex) {
            0 -> { // Posts Tab
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                "post" -> "Posts"
                                "user_image" -> "Photos"
                                "share" -> "Videos"
                                else -> "All Content"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        onFilterChange(null)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Posts") },
                                    onClick = {
                                        onFilterChange("post")
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Photos") },
                                    onClick = {
                                        onFilterChange("user_image")
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Videos") },
                                    onClick = {
                                        onFilterChange("share")
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                renderUnifiedContent(
                    items = userContent,
                    navController = navController,
                    onReactionClick = onReactionClick,
                    onCommentClick = onCommentClick,
                    onMoreClick = onMoreClick,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onFollowClick = onFollowClick,
                    onShareClick = onShareClick,
                    isPaused = isPaused,
                    followStatuses = followStatuses,
                    loadingUsers = loadingUsers,
                    groupMembershipStatuses = groupMembershipStatuses,
                    joiningGroupIds = joiningGroupIds
                )
            }

            1 -> { // Photos Tab
                renderStaggeredMediaGrid(
                    mediaItems = photos,
                    profile = profile,
                    onImageClick = onImageClick
                )
            }

            2 -> { // Videos Tab
                renderStaggeredMediaGrid(
                    mediaItems = videos,
                    profile = profile,
                    onImageClick = onImageClick
                )
            }

            3 -> { // Reels Tab
                if (isReelsLoading && reelItems.isEmpty()) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(9) { MediaGridShimmerItem() }
                        }
                    }
                } else if (reelItems.isEmpty() && !isReelsLoading) {
                    item { NoMediaPlaceholder() }
                } else {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 5000.dp),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            userScrollEnabled = false // Parent handles scrolling
                        ) {
                            items(reelItems) { reel ->
                                val thumbnail = reel.thumbnailUrl ?: reel.videoUrl ?: reel.media?.firstOrNull()?.fileUrl
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.66f) // Standard reel aspect ratio
                                        .clickable {
                                            navController.navigate("reels/${reel.id}?userId=${profile.id}");
                                        }
                                ) {
                                    SubcomposeAsyncImage(
                                        model = thumbnail,
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
                                        }
                                    )
                                    
                                    // Play icon and view count overlay
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = reel.statistics?.viewCount?.toString() ?: "0",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
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

fun LazyListScope.renderUnifiedGridContent(
    items: LazyPagingItems<UnifiedContentItem>,
    onImageClick: (MediaDetailData) -> Unit
) {
    if (items.loadState.refresh is LoadState.Loading && items.itemCount == 0) {
        items(5) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .shimmerEffect()
                    )
                }
            }
            Spacer(Modifier.height(1.dp))
        }
    } else if (items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading) {
        item { NoMediaPlaceholder() }
    } else {
        val rowCount = (items.itemCount + 2) / 3
        items(rowCount) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (col in 0..2) {
                        val itemIndex = rowIndex * 3 + col
                    if (itemIndex < items.itemCount) {
                        val unifiedItem = items[itemIndex]
                        val itemMap = unifiedItem?.item
                        
                        // Handle different item structures based on type
                        val displayUrl: String
                        val actualUrl: String
                        var itemId = 0
                        var userMinimal: UserMinimal? = null
                        var createdAt: java.time.OffsetDateTime? = null
                        var stats: PostStatsSerializers? = null
                        var isVideoContent = false

                        when (unifiedItem?.type) {
                            UnifiedContentItemTypeEnum.POST, UnifiedContentItemTypeEnum.POSTS -> {
                                val post = safeConvertTo<PostFeed>(itemMap as Any, "PostGrid")
                                val media = post?.media?.firstOrNull()
                                actualUrl = media?.fileUrl ?: ""
                                displayUrl = actualUrl
                                // Check if it's video based on extension since MediaDisplay doesn't have contentType
                                isVideoContent = actualUrl.endsWith(".mp4", ignoreCase = true)
                                itemId = post?.id ?: 0
                                userMinimal = post?.user
                                createdAt = post?.createdAt
                                stats = post?.statistics
                            }
                            UnifiedContentItemTypeEnum.SHARE, UnifiedContentItemTypeEnum.SHARES -> {
                                val share = safeConvertTo<ShareFeed>(itemMap as Any, "ShareGrid")
                                itemId = share?.id ?: 0
                                userMinimal = share?.user
                                createdAt = share?.createdAt
                                stats = share?.statistics
                                
                                // Mas detalyadong extraction para sa shared content
                                val contentDetail = share?.contentObjectDetail
                                val contentData = share?.contentObjectData
                                when (contentDetail?.type) {
                                    "post" -> {
                                        val originalPost = safeConvertTo<PostFeed>(contentData as Any, "ShareGridOriginal")
                                        val media = originalPost?.media?.firstOrNull()
                                        actualUrl = media?.fileUrl ?: ""
                                        displayUrl = actualUrl
                                        isVideoContent = actualUrl.endsWith(".mp4", ignoreCase = true)
                                    }
                                    "reel" -> {
                                        val reel = safeConvertTo<ReelDisplay>(contentData as Any, "ShareGridReel")
                                        actualUrl = reel?.videoUrl ?: reel?.media?.firstOrNull()?.fileUrl ?: ""
                                        displayUrl = if (reel?.thumbnailUrl?.isNotEmpty() == true) {
                                            reel.thumbnailUrl
                                        } else {
                                            actualUrl
                                        }
                                        isVideoContent = true
                                    }
                                    else -> {
                                        actualUrl = ""
                                        displayUrl = ""
                                    }
                                }
                            }
                            UnifiedContentItemTypeEnum.USER_IMAGE -> {
                                val userImage = safeConvertTo<UserImageDisplay>(itemMap as Any, "UserImageGrid")
                                displayUrl = userImage?.imageUrl ?: ""
                                actualUrl = displayUrl
                                itemId = userImage?.id ?: 0
                                userMinimal = userImage?.user
                                stats = userImage?.statistics
                            }
                            else -> {
                                val rawUrl = (itemMap?.get("file_url") as? String 
                                    ?: itemMap?.get("image") as? String
                                    ?: itemMap?.get("image_url") as? String
                                    ?: itemMap?.get("url") as? String
                                    ?: "")
                                
                                val rawThumbnail = itemMap?.get("thumbnail") as? String
                                val contentType = (itemMap?.get("content_type") as? String ?: "").lowercase()
                                
                                isVideoContent = contentType == "video" ||
                                                   contentType == "reel" || 
                                                   rawUrl.endsWith(".mp4", ignoreCase = true)

                                actualUrl = rawUrl
                                displayUrl = if (!rawThumbnail.isNullOrEmpty()) {
                                    rawThumbnail
                                } else if (!isVideoContent) {
                                    rawUrl
                                } else {
                                    ""
                                }
                                
                                itemId = (itemMap?.get("id") as? Number)?.toInt() 
                                    ?: (itemMap?.get("content_id") as? Number)?.toInt() 
                                    ?: 0
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable {
                                    onImageClick(
                                        MediaDetailData(
                                            url = actualUrl,
                                            user = userMinimal,
                                            createdAt = createdAt,
                                            stats = stats,
                                            id = itemId,
                                            type = unifiedItem?.type?.value ?: "post"
                                        )
                                    )
                                }
                        ) {
                            SubcomposeAsyncImage(
                                model = displayUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                                error = {
                                    Box(
                                        Modifier.fillMaxSize().background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.BrokenImage, null, tint = Color.Gray)
                                    }
                                }
                            )
                            
                            // Show play icon if it's a video/share
                            if (isVideoContent) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(1.dp))
        }

        if (items.loadState.append is LoadState.Loading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.renderUnifiedContent(
    items: LazyPagingItems<UnifiedContentItem>,
    navController: NavController,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int, stats: PostStatsSerializers?) -> Unit,
    onMoreClick: (Any) -> Unit,
    onImageClick: (MediaDetailData) -> Unit,
    onVideoClick: (PostFeed, String) -> Unit,
    onFollowClick: (UserMinimal) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    isPaused: Boolean,
    followStatuses: Map<Int, Boolean>,
    loadingUsers: Map<Int, Boolean>,
    groupMembershipStatuses: Map<Int, Boolean>,
    joiningGroupIds: Map<Int, Boolean>
) {
    if (items.loadState.refresh is LoadState.Loading && items.itemCount == 0) {
        items(3) { ShimmerFeedItem() }
    } else if (items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading) {
        item {
            EmptyStatePlaceholder(text = "No content to display")
        }
    } else {
        items(
            count = items.itemCount,
            key = { index ->
                val item = items[index]
                if (item != null) "${item.type}_${index}" else "placeholder_$index"
            }
        ) { index ->
            val item = items[index]
            item?.let {
                UnifiedFeedRow(
                    row = it,
                    navController = navController,
                    onReactionClick = onReactionClick,
                    onCommentClick = onCommentClick,
                    onMoreClick = onMoreClick,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onGroupJoinClick = {},
                    onFollowClick = onFollowClick,
                    onShare = onShareClick,
                    isPaused = isPaused,
                    followStatuses = followStatuses,
                    loadingUsers = loadingUsers,
                    groupMembershipStatuses = groupMembershipStatuses,
                    joiningGroupIds = joiningGroupIds,
                    onGroupClick = {navController.navigate("group/${it}")}
                )
                HorizontalDivider(
                    thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        if (items.loadState.append is LoadState.Loading) {
            items(1) { ShimmerFeedItem() }
        }
    }
}

fun LazyListScope.renderStaggeredMediaGrid(
    mediaItems: LazyPagingItems<UserMediaItem>,
    profile: UserProfile,
    onImageClick: (MediaDetailData) -> Unit
) {
    if (mediaItems.loadState.refresh is LoadState.Loading && mediaItems.itemCount == 0) {
        item {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalItemSpacing = 4.dp,
                userScrollEnabled = false
            ) {
                items(6) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (it % 2 == 0) 200.dp else 250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    } else if (mediaItems.itemCount == 0 && mediaItems.loadState.refresh is LoadState.NotLoading) {
        item { NoMediaPlaceholder() }
    } else {
        item {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 5000.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalItemSpacing = 4.dp,
                userScrollEnabled = false
            ) {
                items(mediaItems.itemCount) { index ->
                    val media = mediaItems[index]
                    media?.let {
                        val imageUrl = it.url?.toString() ?: ""
                        
                        // Check kung video ba ang content (reel o video type, o kaya .mp4 ang extension)
                        val isVideo = it.contentType?.lowercase() == "video" || 
                                      it.contentType?.lowercase() == "reel" || 
                                      imageUrl.endsWith(".mp4", ignoreCase = true)
                        
                        // Siguraduhin na thumbnail ang priority. 
                        // Kung video at walang thumbnail, wag gagamit ng .mp4 fallback.
                        val thumbnailUrl = if (it.thumbnail != null && it.thumbnail.toString().isNotEmpty()) {
                            it.thumbnail.toString()
                        } else if (!isVideo) {
                            it.thumbnail?.toString()?:imageUrl
                        } else {
                            "" // Blanko muna kung video pero walang thumbnail
                        }
                        
                        // Pinterest-like aspect ratio
                        val aspectRatio = if (index % 3 == 0) 0.8f else if (index % 3 == 1) 1f else 1.2f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onImageClick(
                                        MediaDetailData(
                                            url = imageUrl,
                                            user = UserMinimal(
                                                id = profile.id,
                                                username = profile.username,
                                                profilePictureUrl = profile.profilePictureUrl,
                                                fullName = profile.fullName
                                            ),
                                            createdAt = it.createdAt,
                                            stats = null,
                                            id = it.contentId,
                                            type = it.contentType
                                        )
                                    )
                                }
                        ) {
                            SubcomposeAsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                                error = {
                                    Box(
                                        Modifier.fillMaxSize().background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.BrokenImage, null, tint = Color.Gray)
                                    }
                                }
                            )
                            
                            if (isVideo) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (mediaItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

fun LazyListScope.renderMediaGrid(
    mediaItems: LazyPagingItems<UserMediaItem>,
    profile: UserProfile,
    onImageClick: (MediaDetailData) -> Unit
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
                                    val allMediaDisplays = (0 until mediaItems.itemCount).mapNotNull { idx ->
                                        mediaItems[idx]?.let { item ->
                                            com.cyberarcenal.huddle.api.models.MediaDisplay(
                                                id = item.contentId,
                                                fileUrl = item.url?.toString(),
                                                createdAt = item.createdAt
                                            )
                                        }
                                    }
                                    onImageClick(
                                        MediaDetailData(
                                            url = imageUrl,
                                            user = UserMinimal(
                                                id = profile.id,
                                                username = profile.username,
                                                profilePictureUrl = profile.profilePictureUrl,
                                                fullName = profile.fullName
                                            ),
                                            createdAt = it.createdAt,
                                            stats = null,
                                            id = it.contentId,
                                            type = it.contentType,
                                            allMedia = allMediaDisplays,
                                            initialIndex = itemIndex
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

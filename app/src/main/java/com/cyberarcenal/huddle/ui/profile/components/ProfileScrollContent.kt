// ProfileScrollContent.kt

package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.ui.common.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.PostItem
import com.cyberarcenal.huddle.ui.common.ShareItem

@Composable
fun ProfileScrollContent(
    profile: UserProfile,
    isCurrentUser: Boolean,
    userContent: LazyPagingItems<UnifiedContentItem>,
    listState: LazyListState,
    onReaction: (String, Int, ReactionType?) -> Unit,          // contentType, objectId, reactionType
    onCommentClick: (String, Int) -> Unit,                    // contentType, objectId
    onShareClick: (String, Int) -> Unit,                      // contentType, objectId
    onMoreClick: (UnifiedContentItem) -> Unit,                // item
    onProfileClick: (Int) -> Unit,
    onImageClick: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit,
    onEditProfilePicture: () -> Unit,
    onEditCoverPhoto: () -> Unit,
    onRemoveProfilePicture: () -> Unit,
    onRemoveCoverPhoto: () -> Unit,
    onFollowToggle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
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
                onNavigateBack = onNavigateBack
            )
        }

        // Tabs
        stickyHeader(key = "profile_tabs") {
            ProfileTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                if (userContent.loadState.refresh is LoadState.Loading && userContent.itemCount == 0) {
                    item(key = "content_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (userContent.itemCount == 0) {
                    item(key = "content_empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "No content yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    }
                } else {
                    items(
                        count = userContent.itemCount,
                        key = { index ->
                            val item = userContent[index]
                            if (item != null) "${item.type}_${item.data.hashCode()}" else
                                "placeholder_$index"
                        }
                    ) { index ->
                        val unifiedItem = userContent[index]
                        unifiedItem?.let { item ->
                            when (item.type) {
                                "post" -> {
                                    val post = item.data as PostFeed
                                    FeedItemFrame(
                                        user = post.user,
                                        createdAt = post.createdAt,
                                        statistics = post.statistics,
                                        headerSuffix = "",
                                        onReactionClick = { reactionType ->
                                            onReaction("post", post.id!!, reactionType)
                                        },
                                        onCommentClick = { onCommentClick("post", post.id!!) },
                                        onShareClick = { onShareClick("post", post.id!!) },
                                        onMoreClick = { onMoreClick(item) },
                                        onProfileClick = { onProfileClick(post.user?.id ?: return@FeedItemFrame) },
                                        content = {
                                            PostItem(
                                                post = post,
                                                onImageClick = onImageClick
                                            )
                                        }
                                    )
                                }
                                "share" -> {
                                    val share = item.data as ShareFeed
                                    ShareItem(
                                        share = share,
                                        onProfileClick = { onProfileClick(share.user?.id ?: return@ShareItem) },
                                        onCommentClick = { onCommentClick("share", share.id!!) },
                                        onReactionClick = { reactionType ->
                                            onReaction("share", share.id!!, reactionType)
                                        },
                                        onImageClick = onImageClick
                                    )
                                }
                                "reel" -> {
                                    val reel = item.data as ReelDisplay
                                    // You'll need a ReelItem composable; placeholder for now
                                    Text("Reel: ${reel.caption}", modifier = Modifier.padding(16.dp))
                                    // Alternatively, create a proper ReelItem and pass callbacks
                                }
                                "story" -> {
                                    val story = item.data as Story
                                    // Story item – usually shown differently; placeholder
                                    Text("Story by ${story.user?.username}", modifier = Modifier.padding(16.dp))
                                }
                                else -> {
                                    // Fallback for unknown types
                                    Text("Unknown content type", modifier = Modifier.padding(16.dp))
                                }
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (userContent.loadState.append is LoadState.Loading) {
                        item(key = "append_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
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
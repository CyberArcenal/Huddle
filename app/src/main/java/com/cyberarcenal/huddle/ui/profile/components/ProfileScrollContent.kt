package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.common.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.PostItem

@Composable
fun ProfileScrollContent(
    profile: UserProfile,
    isCurrentUser: Boolean,
    userPosts: LazyPagingItems<PostFeed>,
    listState: LazyListState,
    onReaction: (Int, ReactionCreateRequest.ReactionType?) -> Unit,
    onNavigateToComments: (Int?) -> Unit,
    // ── Bagong parameters para sa header ──
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit,
    onEditProfilePicture: () -> Unit,
    onEditCoverPhoto: () -> Unit,
    onRemoveProfilePicture: () -> Unit,
    onRemoveCoverPhoto: () -> Unit,
    onFollowToggle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateBack: () -> Unit,

    onCommentClick : (postId: PostFeed) -> Unit,
    onMoreClick : (PostFeed) -> Unit,
    onProfileClick: (PostFeed) -> Unit,
    onImageClick: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ================== SCROLLABLE HEADER ==================
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

        // ================== STICKY TABS ==================
        stickyHeader(key = "profile_tabs") {
            ProfileTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        // ================== TAB CONTENT ==================
        when (selectedTab) {
            0 -> {
                // Posts
                if (userPosts.loadState.refresh is LoadState.Loading && userPosts.itemCount == 0) {
                    item(key = "posts_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (userPosts.itemCount == 0) {
                    item(key = "posts_empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "No posts yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    }
                } else {
                    items(
                        count = userPosts.itemCount,
                        key = { index -> 
                            val post = userPosts[index]
                            if (post != null) "post_${post.id}" else "post_placeholder_$index"
                        }
                    ) { index ->
                        val post = userPosts[index]
                        post?.let { postFeed ->
                            FeedItemFrame(
                                user = postFeed.user,
                                createdAt = postFeed.createdAt,
                                statistics = postFeed.statistics,
                                headerSuffix = "",
                                onReactionClick = {reaction -> onReaction(postFeed.id as Int, reaction)},
                                onCommentClick = { onCommentClick(postFeed) },
                                onShareClick = {},
                                onMoreClick = {onMoreClick(postFeed)},
                                onProfileClick = {onProfileClick(postFeed)},
                                content = {
                                    PostItem(
                                        post = postFeed,
                                        onImageClick = { url -> onImageClick(url) }
                                    )
                                }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (userPosts.loadState.append is LoadState.Loading) {
                        item(key = "posts_append_loading") {
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

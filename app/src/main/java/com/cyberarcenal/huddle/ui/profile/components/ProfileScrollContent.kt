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
import com.cyberarcenal.huddle.api.models.Post
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.feed.components.PostItem

@Composable
fun ProfileScrollContent(
    profile: UserProfile,
    isCurrentUser: Boolean,
    userPosts: LazyPagingItems<PostFeed>,
    listState: LazyListState,
    onToggleLike: (Int?) -> Unit,
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
        item {
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
        stickyHeader {
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
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (userPosts.itemCount == 0) {
                    item {
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
                        key = { index -> userPosts[index]?.id ?: index }
                    ) { index ->
                        val post = userPosts[index]
                        post?.let {
                            PostItem(
                                post = it,
                                onLikeClick = { _, _ -> onToggleLike(it.id) },
                                onCommentClick = { onNavigateToComments(it.id) },
                                onMoreClick = {
                                    onMoreClick(it)
                                },
                                onProfileClick = {
                                    onProfileClick(it)
                                },
                                onImageClick = { onImageClick(it)
                                },
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (userPosts.loadState.append is LoadState.Loading) {
                        item {
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
                item {
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
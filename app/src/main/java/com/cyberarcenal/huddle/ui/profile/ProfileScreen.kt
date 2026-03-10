package com.cyberarcenal.huddle.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.home.components.PostItem // Siguraduhing tama ang import ng iyong PostItem
import com.cyberarcenal.huddle.ui.theme.Gradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int?,
    navController: NavController
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(userId)
    )
    ProfileScreenContent(
        userId = userId,
        navController = navController,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    userId: Int?,
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val profileState by viewModel.profileState.collectAsState()
    val userPosts = viewModel.userPostsFlow.collectAsLazyPagingItems()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                },
                // INALIS ANG navigationIcon (Back Button)
                actions = {
                    if (userId == null) {
                        IconButton(onClick = { /* settings */ }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (profileState) {
                is ProfileState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileState.Success -> {
                    val profile = (profileState as ProfileState.Success).profile
                    ProfileContent(
                        profile = profile,
                        isCurrentUser = userId == null,
                        isFollowing = profile.isFollowing,
                        onFollowClick = {
                            if (profile.isFollowing) viewModel.unfollowUser() else viewModel.followUser()
                        },
                        userPosts = userPosts,
                        paddingValues = paddingValues,
                        viewModel = viewModel
                    )
                }
                is ProfileState.Error -> {
                    Text("Error loading profile", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    profile: UserProfile,
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    userPosts: androidx.paging.compose.LazyPagingItems<PostFeed>,
    paddingValues: PaddingValues,
    viewModel: ProfileViewModel
) {
    // Ginamit ang LazyColumn sa halip na Grid para magmukhang Facebook Feed
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = 120.dp // Space para sa floating navigation bar
        )
    ) {
        // 1. Profile Header
        item {
            ProfileHeaderModern(
                profile = profile,
                isCurrentUser = isCurrentUser,
                isFollowing = isFollowing,
                onFollowClick = onFollowClick
            )
        }

        // 2. Section Title (Optional)
        item {
            Text(
                text = "Posts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(16.dp)
            )
        }

        // 3. Vertical Posts Feed (Facebook Style)
        items(
            count = userPosts.itemCount,
            key = { index -> userPosts[index]?.id ?: index }
        ) { index ->
            val post = userPosts[index]
            post?.let {
                // Gamitin ang iyong existing PostItem component para sa full feed look
                PostItem(
                    post = it,
                    onLikeClick = { isLiked, count ->
                        // Kung may toggleLike ang profile viewModel, tawagin dito
                    },
                    onCommentClick = { /* navigate to comments */ }
                )
                Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun ProfileHeaderModern(
    profile: UserProfile,
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Gradients.buttonGradient)
            )
            AsyncImage(
                model = profile.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${profile.firstName} ${profile.lastName}".trim(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "@${profile.username}",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
        )

        profile.bio?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModernStatItem(count = profile.followersCount, label = "Followers")
            ModernStatItem(count = profile.followingCount, label = "Following")
            ModernStatItem(count = profile.postsCount, label = "Posts")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        if (isCurrentUser) {
            Button(
                onClick = { /* edit */ },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = onFollowClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isFollowing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ModernStatItem(count: Any, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray)
        )
    }
}
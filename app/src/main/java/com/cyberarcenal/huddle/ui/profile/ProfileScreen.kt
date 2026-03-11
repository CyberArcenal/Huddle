package com.cyberarcenal.huddle.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.home.components.PostItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int?,
    navController: NavController
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userId))
    val profileState by viewModel.profileState.collectAsState()
    val userPosts = viewModel.userPostsFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionState by viewModel.actionState.collectAsState()

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> snackbarHostState.showSnackbar(state.message)
            is ActionState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
        // Inalis ang topBar slot para mawala ang padding sa itaas
    ) { paddingValues ->
        // Inalis ang padding(paddingValues) para maging full-screen (0 padding sa taas)
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileState.Success -> {
                    ProfileContent(
                        profile = state.profile,
                        isCurrentUser = userId == null,
                        userPosts = userPosts,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                is ProfileState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}")
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    profile: UserProfile,
    isCurrentUser: Boolean,
    userPosts: androidx.paging.compose.LazyPagingItems<PostFeed>,
    viewModel: ProfileViewModel,
    navController: NavController
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Posts", "Media", "Likes")
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) showBottomSheet = false
                    viewModel.changeProfilePicture(uri)
                }
            }
        }
    )

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("View Profile Picture") },
                    leadingContent = { Icon(Icons.Outlined.Visibility, null) },
                    modifier = Modifier.clickable { showBottomSheet = false }
                )
                if (isCurrentUser) {
                    ListItem(
                        headlineContent = { Text("Change Profile Picture") },
                        leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            ProfileHeaderSection(
                profile = profile,
                isCurrentUser = isCurrentUser,
                viewModel = viewModel,
                navController = navController,
                onAvatarClick = { showBottomSheet = true }
            )
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        if (selectedTab == 0) {
            when {
                userPosts.loadState.refresh is LoadState.Loading -> {
                    item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(32.dp)) } }
                }
                userPosts.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "No posts yet", 
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    }
                }
                else -> {
                    items(count = userPosts.itemCount, key = { userPosts[it]?.id ?: it }) { index ->
                        val post = userPosts[index]
                        post?.let {
                            PostItem(it, { _, _ -> viewModel.toggleLike(it.id) }, {
                                navController.navigate("comments/${it.id}")
                            })
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        } else {
            item { Box(Modifier.fillMaxWidth().padding(64.dp), Alignment.TopCenter) { Text("Coming soon...", color = Color.Gray) } }
        }
    }
}

@Composable
fun ProfileHeaderSection(
    profile: UserProfile,
    isCurrentUser: Boolean,
    viewModel: ProfileViewModel,
    navController: NavController,
    onAvatarClick: () -> Unit
) {
    Column {
        Box(modifier = Modifier.height(200.dp)) {
            // Cover Photo simula sa 0 (walang padding sa taas)
            AsyncImage(
                model = profile.coverPhotoUrl ?: "https://images.unsplash.com/photo-1557683316-973673baf926",
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            // Back Button Overlay
            if (!isCurrentUser || navController.previousBackStackEntry != null) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            // Settings/More button overlay
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        if (isCurrentUser) navController.navigate("settings")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentUser) Icons.Outlined.Settings else Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(4.dp)
                    .clickable { onAvatarClick() }
            ) {
                AsyncImage(
                    model = profile.profilePictureUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
            }

            // Action Button
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 4.dp)) {
                if (isCurrentUser) {
                    OutlinedButton(
                        onClick = { navController.navigate("edit_profile") },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Edit Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Button(onClick = viewModel::onFollowToggle, shape = RoundedCornerShape(20.dp)) {
                        Text(if (profile.isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${profile.firstName} ${profile.lastName}".trim().ifEmpty { profile.username },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                if (profile.isVerified == true) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            Text("@${profile.username}", color = Color.Gray)
            profile.bio?.let { Spacer(Modifier.height(8.dp)); Text(it) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileStat(count = profile.followingCount.toString(), label = "Following")
                ProfileStat(count = profile.followersCount.toString(), label = "Followers")
            }
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = count, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(4.dp))
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}

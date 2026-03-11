package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFixedHeader(
    profile: UserProfile,
    isCurrentUser: Boolean,
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
    var showAvatarSheet by remember { mutableStateOf(false) }
    var showCoverSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Bottom sheets (unchanged) – same as before, but moved here
    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("View Profile Picture") },
                    leadingContent = { Icon(Icons.Outlined.Visibility, null) },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showAvatarSheet = false
                            onAvatarClick()
                        }
                    }
                )
                if (isCurrentUser) {
                    ListItem(
                        headlineContent = { Text("Change Profile Picture") },
                        leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showAvatarSheet = false
                                onEditProfilePicture()
                            }
                        }
                    )
                    if (profile.profilePictureUrl != null) {
                        ListItem(
                            headlineContent = { Text("Remove Profile Picture") },
                            leadingContent = { Icon(Icons.Outlined.Delete, null) },
                            modifier = Modifier.clickable {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showAvatarSheet = false
                                    onRemoveProfilePicture()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCoverSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoverSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("View Cover Photo") },
                    leadingContent = { Icon(Icons.Outlined.Visibility, null) },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showCoverSheet = false
                            onCoverClick()
                        }
                    }
                )
                if (isCurrentUser) {
                    ListItem(
                        headlineContent = { Text("Change Cover Photo") },
                        leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showCoverSheet = false
                                onEditCoverPhoto()
                            }
                        }
                    )
                    if (profile.coverPhotoUrl != null) {
                        ListItem(
                            headlineContent = { Text("Remove Cover Photo") },
                            leadingContent = { Icon(Icons.Outlined.Delete, null) },
                            modifier = Modifier.clickable {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showCoverSheet = false
                                    onRemoveCoverPhoto()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    Column {
        // Top bar with back and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            if (isCurrentUser) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            } else {
                IconButton(onClick = { /* more options */ }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                }
            }
        }

        Box(modifier = Modifier.height(200.dp)) {
            // Cover Photo
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.coverPhotoUrl ?: "https://images.unsplash.com/photo-1557683316-973673baf926")
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.LightGray)
                    .clickable { showCoverSheet = true },
                contentScale = ContentScale.Crop
            )

            // Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(4.dp)
                    .clickable { showAvatarSheet = true }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profile.profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
            }

            // Action Button
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 4.dp)) {
                if (isCurrentUser) {
                    OutlinedButton(
                        onClick = onNavigateToEditProfile,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Edit Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val isFollowing by remember { derivedStateOf { profile.isFollowing } }
                    Button(
                        onClick = onFollowToggle,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing == true) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (isFollowing == true) "Following" else "Follow",
                            fontWeight = FontWeight.Bold,
                            color = if (isFollowing == true) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    }
                }
            }
        }

        // Profile info (name, username, bio, phone, location, stats) – still part of fixed header
        ProfileInfo(profile)
    }
}
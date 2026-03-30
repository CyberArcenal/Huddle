package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.FollowStatsResponse
import com.cyberarcenal.huddle.api.models.FollowStatusResponse
import com.cyberarcenal.huddle.api.models.PersonalityTypeEnum
import com.cyberarcenal.huddle.api.models.UserImageMinimal
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.models.MediaDetailData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFixedHeader(
    profile: UserProfile,
    isCurrentUser: Boolean,
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
    onAddHighlightClick: () -> Unit,
    followStatus: FollowStatusResponse?,
    followStats: FollowStatsResponse?,
) {
    var showAvatarSheet by remember { mutableStateOf(false) }
    var showCoverSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Helper to build UserMinimal from profile
    val userMinimal = remember(profile) {
        UserMinimal(
            id = profile.id,
            username = profile.username,
            profilePictureUrl = profile.profilePictureUrl,
            personalityType = profile.personalityType as? PersonalityTypeEnum,
            hobbies = profile.hobbies,
            fullName = "${profile.firstName} ${profile.lastName}".trim(),
            location = profile.location,
            reasons = profile.reasons,
            isFollowing = profile.isFollowing,
            capabilityScore = profile.capabilityScore
        )
    }

    // Avatar bottom sheet logic
    if (showAvatarSheet) {
        ProfileImageBottomSheet(
            image = profile.profilePicture,
            isCurrentUser = isCurrentUser,
            onView = {
                profile.profilePicture?.let { image ->
                    if (image.id != null) {
                        onAvatarClick(
                            MediaDetailData(
                                url = image.imageUrl ?: "",
                                user = userMinimal,
                                createdAt = image.createdAt,
                                stats = image.statistics,
                                id = image.id,
                                type = "usermedia"
                            )
                        )
                    }
                }
                showAvatarSheet = false
            },
            onChange = {
                showAvatarSheet = false
                onEditProfilePicture()
            },
            onRemove = {
                showAvatarSheet = false
                onRemoveProfilePicture()
            }
        )
    }

    // Cover bottom sheet logic
    if (showCoverSheet) {
        ProfileImageBottomSheet(
            image = profile.coverPhoto,
            isCurrentUser = isCurrentUser,
            onView = {
                profile.coverPhoto?.let { image ->
                    if (image.id != null) {
                        onCoverClick(
                            MediaDetailData(
                                url = image.imageUrl ?: "",
                                user = userMinimal,
                                createdAt = image.createdAt,
                                stats = image.statistics,
                                id = image.id,
                                type = "usermedia"
                            )
                        )
                    }
                }
                showCoverSheet = false
            },
            onChange = {
                showCoverSheet = false
                onEditCoverPhoto()
            },
            onRemove = {
                showCoverSheet = false
                onRemoveCoverPhoto()
            }
        )
    }

    Column {
        // --- 1. COVER PHOTO AND OVERLAY SECTION ---
        Box(modifier = Modifier.height(200.dp)) {
            // Cover photo
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

            // OVERLAY BUTTONS (Add, Settings, More) - Naka-overlay sa image
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp), // Saktong 10dp padding
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentUser) {
                    IconButton(
                        onClick = onAddHighlightClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.3f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add highlight")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.3f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                } else {
                    IconButton(
                        onClick = { /* more options */ },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.3f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                    }
                }
            }

            // --- 2. AVATAR (Naka-overlap sa Cover) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
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

            // --- 3. EDIT/FOLLOW BUTTON (Bottom End) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 4.dp)
            ) {
                if (isCurrentUser) {
                    Button(
                        onClick = onNavigateToEditProfile,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0F2F5),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Preferences", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val isFollowing = followStatus?.isFollowing == true
                    Button(
                        onClick = onFollowToggle,
                        shape = RoundedCornerShape(20.dp),
                        colors = if (isFollowing)
                            ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F2F5), contentColor = Color.Black)
                        else
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Text(
                            if (isFollowing) "Following" else "Follow",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Profile info (name, bio, etc.)
        ProfileInfo(profile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileImageBottomSheet(
    image: UserImageMinimal?,
    isCurrentUser: Boolean,
    onView: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { scope.launch { sheetState.hide() } },
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text("View Photo") },
                leadingContent = { Icon(Icons.Outlined.Visibility, null) },
                modifier = Modifier.clickable {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onView() }
                }
            )
            if (isCurrentUser) {
                ListItem(
                    headlineContent = { Text("Change Photo") },
                    leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onChange() }
                    }
                )
                if (image != null) {
                    ListItem(
                        headlineContent = { Text("Remove Photo") },
                        leadingContent = { Icon(Icons.Outlined.Delete, null) },
                        modifier = Modifier.clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onRemove() }
                        }
                    )
                }
            }
        }
    }
}
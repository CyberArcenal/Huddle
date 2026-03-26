package com.cyberarcenal.huddle.ui.common.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.UserMinimal

enum class AvatarShape {
    CIRCLE,
    SQUARE
}

@Composable
fun UserItem(
    user: UserMinimal,
    isVertical: Boolean = false,
    onFollowClick: (UserMinimal) -> Unit,
    onItemClick: () -> Unit,
    isFollowing: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    avatarShape: AvatarShape = AvatarShape.CIRCLE
) {
    if (isVertical) {
        VerticalStoryUserItem(
            username = user.username,
            fullName = user.fullName,
            profilePictureUrl = user.profilePictureUrl,
            personalityType = user.personalityType,
            capabilityScore = user.capabilityScore,
            reasons = user.reasons,
            isFollowing = isFollowing,         // use the passed value
            isLoading = isLoading,             // pass loading state
            onFollowClick = { onFollowClick(user) },
            onItemClick = onItemClick,
            modifier = modifier
        )
    } else {
        HorizontalListUserItem(
            username = user.username,
            displayName = user.fullName,
            profilePictureUrl = user.profilePictureUrl,
            personalityType = user.personalityType,
            capabilityScore = user.capabilityScore,
            reasons = user.reasons,
            isFollowing = isFollowing,
            isLoading = isLoading,
            onFollowClick = { onFollowClick(user) },
            onItemClick = onItemClick,
            avatarShape = avatarShape,
            modifier = modifier
        )
    }
}

/**
 * Modern vertical card with full‑width square avatar.
 * Spacing is reduced so the image occupies ~70% of the card height.
 */
@Composable
private fun VerticalStoryUserItem(
    username: String?,
    fullName: String?,
    profilePictureUrl: String?,
    personalityType: UserMinimal.PersonalityType?,
    capabilityScore: Int?,
    reasons: List<String>?,
    isFollowing: Boolean,
    isLoading: Boolean = false,
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .padding(8.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            // Full‑width square avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                UserAvatar(
                    username = username,
                    profilePictureUrl = profilePictureUrl,
                    size = null,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxSize()
                )
                capabilityScore?.let { score ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00BFA5))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$score%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = fullName ?: username ?: "User",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            personalityType?.value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Text(
                text = reasons?.firstOrNull() ?: "Recommended",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = if (isFollowing) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Horizontal list item with modern layout and percentage badge.
 */
@Composable
private fun HorizontalListUserItem(
    username: String?,
    displayName: String?,
    profilePictureUrl: String?,
    personalityType: UserMinimal.PersonalityType?,
    capabilityScore: Int?,
    reasons: List<String>?,
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit,
    avatarShape: AvatarShape,
    isFollowing: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(getAvatarShape(avatarShape, 8.dp))
        ) {
            UserAvatar(
                username = username,
                profilePictureUrl = profilePictureUrl,
                size = 56.dp,
                shape = getAvatarShape(avatarShape, 8.dp)
            )
            capabilityScore?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00BFA5))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$score%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName ?: username ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                personalityType?.value?.let {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = reasons?.firstOrNull() ?: displayName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Button(
            onClick = onFollowClick,
            modifier = Modifier.height(32.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp),
            colors = if (isFollowing) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
            } else {
                Text(text = if (isFollowing) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Shared avatar component (unchanged but uses theme)
@Composable
private fun UserAvatar(
    username: String?,
    profilePictureUrl: String?,
    size: Dp? = null,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val finalModifier = if (size != null) {
        modifier.size(size).clip(shape)
    } else {
        modifier.clip(shape)
    }
    if (!profilePictureUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(profilePictureUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = finalModifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = finalModifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username?.take(1)?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// Helper to get shape from AvatarShape
@Composable
private fun getAvatarShape(shape: AvatarShape, cornerRadius: Dp = 0.dp): Shape {
    return when (shape) {
        AvatarShape.CIRCLE -> CircleShape
        AvatarShape.SQUARE -> RoundedCornerShape(cornerRadius)
    }
}

/**
 * "See More" card for the end of horizontal lists.
 */
@Composable
fun SeeMoreUserCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "See More",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "See All\nPeople",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Preview functions (unchanged but kept for reference)
@Preview(showBackground = true, name = "Horizontal List Style")
@Composable
fun PreviewHorizontalUserItem() {
    val mockUser = UserMinimal(
        id = 1,
        username = "johndoe",
        fullName = "John Doe",
        profilePictureUrl = null,
        personalityType = UserMinimal.PersonalityType.INTJ,
        capabilityScore = 85,
        reasons = listOf("Same hobbies as you"),
        isFollowing = false
    )

    MaterialTheme {
        UserItem(
            user = mockUser,
            isVertical = false,
            onFollowClick = {},
            onItemClick = {},
            avatarShape = AvatarShape.CIRCLE,
            isFollowing = true,
            isLoading = false,
        )
    }
}

@Preview(showBackground = true, name = "Vertical Story Style (Full-Width Avatar)")
@Composable
fun PreviewVerticalUserItem() {
    val mockUser = UserMinimal(
        id = 2,
        username = "janedoe",
        fullName = "Jane Doe",
        profilePictureUrl = null,
        personalityType = UserMinimal.PersonalityType.ENFP,
        capabilityScore = 92,
        reasons = listOf("Near your location"),
        isFollowing = true
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            UserItem(
                user = mockUser,
                isVertical = true,
                onFollowClick = {},
                onItemClick = {},
                isFollowing = true,
                isLoading = false,
            )
        }
    }
}

@Preview(showBackground = true, name = "Home Feed Row Sample")
@Composable
fun PreviewUserRow() {
    val mockUsers = listOf(
        UserMinimal(id = 3, username = "alex", fullName = "Alex Johnson", personalityType = UserMinimal.PersonalityType.INTJ, capabilityScore = 80),
        UserMinimal(id = 4, username = "maria", fullName = "Maria Garcia", personalityType = UserMinimal.PersonalityType.INFJ, capabilityScore = 95),
        UserMinimal(id = 5, username = "kyle", fullName = "Kyle Smith", personalityType = UserMinimal.PersonalityType.ESTP, capabilityScore = 70)
    )

    MaterialTheme {
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mockUsers) { user ->
                UserItem(
                    user = user,
                    isVertical = true,
                    onFollowClick = {},
                    onItemClick = {},
                    isFollowing = true,
                    isLoading = false,
                )
            }
        }
    }
}
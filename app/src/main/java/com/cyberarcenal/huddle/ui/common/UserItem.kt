package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.UserMinimal

@Composable
fun UserItem(
    user: UserMinimal,
    isVertical: Boolean = false, // STYLE SELECTOR: true = Story-like, false = List
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit
) {
    if (isVertical) {
        VerticalStoryUserItem(
            username = user.fullName,
            profilePictureUrl = user.profilePictureUrl,
            personalityType = user.personalityType,
            capabilityScore = user.capabilityScore,
            reasons = user.reasons,
            isFollowing = user.isFollowing ?: false,
            onFollowClick = onFollowClick,
            onItemClick = onItemClick
        )
    } else {
        HorizontalListUserItem(
            username = user.username,
            displayName = user.fullName,
            profilePictureUrl = user.profilePictureUrl,
            personalityType = user.personalityType,
            capabilityScore = user.capabilityScore,
            reasons = user.reasons,
            isFollowing = user.isFollowing ?: false,
            onFollowClick = onFollowClick,
            onItemClick = onItemClick
        )
    }
}


/**
 * STYLE 1: VERTICAL (Para sa Home Feed / Story-like recommendations)
 */
@Composable
private fun VerticalStoryUserItem(
    username: String?,
    profilePictureUrl: String?,
    personalityType: UserMinimal.PersonalityType?,
    capabilityScore: Int?,
    reasons: List<String>?,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .padding(4.dp)
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with Capability Score
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(username, profilePictureUrl, size = 64.dp)
                CapabilityBadge(capabilityScore, size = 20.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name & MBTI
            Text(
                text = username ?: "User",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            personalityType?.value.let {
                if (it != null) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Reason/Social Proof
            Text(
                text = reasons?.firstOrNull() ?: "Recommended",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Follow Button (Compact)
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .height(28.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = if (isFollowing) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color.Black)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontSize = 10.sp)
            }
        }
    }
}

/**
 * STYLE 2: HORIZONTAL (Para sa Search, Followers, Discover lists)
 */
@Composable
private fun HorizontalListUserItem(
    username: String?,
    displayName: String?,
    profilePictureUrl: String?,
    personalityType: UserMinimal.PersonalityType?,
    capabilityScore: Int?,
    reasons: List<String>?,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            UserAvatar(username, profilePictureUrl, size = 48.dp)
            CapabilityBadge(capabilityScore, size = 16.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = username ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                personalityType?.value.let {
                    Spacer(modifier = Modifier.width(4.dp))
                    if (it != null) {
                        Text(it, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                text = reasons?.firstOrNull() ?: displayName ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1
            )
        }

        Button(
            onClick = onFollowClick,
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = if (isFollowing) {
                ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0), contentColor = Color.Black)
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(if (isFollowing) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- SHARED COMPONENTS ---

@Composable
private fun UserAvatar(
    username: String?,
    profilePictureUrl: String?,
    size: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    if (!profilePictureUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(profilePictureUrl).crossfade(true).build(),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, Color.LightGray.copy(0.3f), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(username?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CapabilityBadge(score: Int?, size: androidx.compose.ui.unit.Dp) {
    score?.let {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFFFB100))
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(it.toString(), fontSize = (size.value * 0.5).sp, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

// --- PREVIEWS ---

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
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Vertical Story Style")
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
                onItemClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Home Feed Row Sample")
@Composable
fun PreviewUserRow() {
    val mockUsers = listOf(
        UserMinimal(id = 3, username = "alex", personalityType =UserMinimal.PersonalityType.INTJ, capabilityScore = 80),
        UserMinimal(id = 4, username = "maria", personalityType = UserMinimal.PersonalityType.INFJ, capabilityScore = 95),
        UserMinimal(id = 5, username = "kyle", personalityType = UserMinimal.PersonalityType.ESTP, capabilityScore = 70)
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
                    onItemClick = {}
                )
            }
        }
    }
}

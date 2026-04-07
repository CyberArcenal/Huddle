package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.profile.Enums.getDisplayName

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileInfo(navController: NavController, profile: UserProfile, isCurrentUser: Boolean,
                recentMoots: List<UserMinimal> =
    emptyList(),) {
    Column(modifier = Modifier.padding(16.dp)) {
        // ProfileInfo.kt - sa loob ng ProfileInfo composable

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${profile.firstName} ${profile.lastName}".trim().ifEmpty { profile.username },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            if (profile.isVerified == true) {
                Icon(Icons.Outlined.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            // ✅ Personality badge
            profile.personalityType?.let { personality ->
                Spacer(modifier = Modifier.width(4.dp))
                AssistChip(
                    onClick = { /* Optional: show explanation dialog */ },
                    label = {
                        Text(
                            text = personality.value,  // e.g., "INTJ"
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier.height(24.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            } ?: run {
                // Ipakita lang ang "Not set up" badge kung sariling profile
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AssistChip(
                        onClick = { navController.navigate("preferences") },
                        label = {
                            Text(
                                text = "Not set up",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier.height(24.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        Text(
            text = "@${profile.username}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        profile.bio?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }

        // --- NEW: Love Language & Relationship Goal ---
        if (profile.loveLanguage != null || profile.relationshipGoal != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile.loveLanguage?.let { loveLang ->
                    AssistChip(
                        onClick = { },
                        label = { Text("Love Language: ${loveLang.value}") },
                        leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
                profile.relationshipGoal?.let { goal ->
                    AssistChip(
                        onClick = { },
                        label = { Text(goal.value) },
                        leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        profile.phoneNumber?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        profile.location?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProfileStat(count = (profile.postsCount ?: 0).toString(), label = "Posts")
            ProfileStat(count = (profile.friendsCount ?: 0).toString(), label = "Friends")
            ProfileStat(count = (profile.followersCount ?: 0).toString(), label = "Followers")
            ProfileStat(count = (profile.followingCount ?: 0).toString(), label = "Following")
        }

        // --- NEW: Reasons / "Why I'm here" ---
        if (!profile.reasons.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Why I'm here",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile.reasons.forEach { reason ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(reason) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        if (!isCurrentUser && profile.mutualFriendsCount != null && profile.mutualFriendsCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${profile.mutualFriendsCount} mutual friend${if (profile.mutualFriendsCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* navigate to mutual friends list */ }
            )
        }

        // Friends preview row
        if (!profile.friendsPreview.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { /* navigate to full friends list */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profile.friendsPreview) { friend ->
                    FriendPreviewItem(friend = friend, onFriendClick = { /* navigate to friend's profile */ })
                }
            }
        }

        if (recentMoots.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            RecentMootsRow(
                moots = recentMoots,
                navController = navController,
                modifier = Modifier.fillMaxWidth()
            )
        }


        // Preferences section
        ProfileCategoryTabs(profile)
    }
}


@Composable
private fun FriendPreviewItem(
    friend: UserMinimal,
    onFriendClick: (UserMinimal) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onFriendClick(friend) }
    ) {
        AsyncImage(
            model = friend.profilePictureUrl,
            contentDescription = friend.fullName ?: friend.username,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = friend.fullName?.split(" ")?.firstOrNull() ?: friend.username ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}






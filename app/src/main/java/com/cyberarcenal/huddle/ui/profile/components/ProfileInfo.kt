package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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

        // Sa ProfileAboutTab, pagkatapos ng bio o location, idagdag:

        profile.personalityType?.let { personality ->
            InfoCard(
                icon = Icons.Default.Person,
                title = "Personality Type",
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = { },
                            label = { Text(personality.getDisplayName()) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            )
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
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileStat(count = profile.followingCount.toString(), label = "Following")
            ProfileStat(count = profile.followersCount.toString(), label = "Followers")
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






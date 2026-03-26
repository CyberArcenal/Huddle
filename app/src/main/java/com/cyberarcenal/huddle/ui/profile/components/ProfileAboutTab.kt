package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAboutTab(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bio section
        profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            InfoCard(
                icon = Icons.Default.Info,
                title = "Bio",
                content = { Text(bio, style = MaterialTheme.typography.bodyMedium) }
            )
        }

        // Location and phone
        if (!profile.location.isNullOrBlank() || !profile.phoneNumber.isNullOrBlank()) {
            InfoCard(
                icon = Icons.Default.LocationOn,
                title = "Contact & Location",
                content = {
                    Column {
                        if (!profile.location.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(profile.location, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (!profile.phoneNumber.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(profile.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            )
        }

        // Preferences sections
        val preferences = listOf(
            "Hobbies" to profile.hobbies?.map { it.name },
            "Interests" to profile.interests?.map { it.name },
            "Favorites" to profile.favorites?.map { it.name },
            "Music" to profile.favoriteMusic?.map { it.name },
            "Work" to profile.works?.map { it.name },
            "Education" to profile.schools?.map { it.name },
            "Achievements" to profile.achievements?.map { it.name },
            "Causes" to profile.causes?.map { it.name },
            "Lifestyle" to profile.lifestyleTags?.map { it.name }
        ).filter { (_, items) -> items.isNullOrEmpty().not() }

        preferences.forEach { (title, items) ->
            InfoCard(
                icon = getIconForPreference(title),
                title = title,
                content = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items?.forEach { item ->
                            AssistChip(
                                onClick = { /* maybe navigate to search? */ },
                                label = { Text(item) },
                                shape = MaterialTheme.shapes.extraSmall
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun getIconForPreference(title: String): ImageVector {
    return when (title) {
        "Hobbies" -> Icons.Default.Favorite
        "Interests" -> Icons.Default.Star
        "Favorites" -> Icons.Default.Star
        "Music" -> Icons.Default.MusicNote
        "Work" -> Icons.Default.Work
        "Education" -> Icons.Default.School
        "Achievements" -> Icons.Default.EmojiEvents
        "Causes" -> Icons.Default.VolunteerActivism
        "Lifestyle" -> Icons.Default.Spa
        else -> Icons.Default.Info
    }
}
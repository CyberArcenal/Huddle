package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.UserProfile

@Composable
fun ProfilePreferences(profile: UserProfile) {
    val hasAny = (profile.hobbies?.isNotEmpty() == true) ||
            (profile.interests?.isNotEmpty() == true) ||
            (profile.favorites?.isNotEmpty() == true) ||
            (profile.favoriteMusic?.isNotEmpty() == true) ||
            (profile.works?.isNotEmpty() == true) ||
            (profile.schools?.isNotEmpty() == true) ||
            (profile.achievements?.isNotEmpty() == true) ||
            (profile.causes?.isNotEmpty() == true) ||
            (profile.lifestyleTags?.isNotEmpty() == true)

    if (!hasAny) return

    Spacer(modifier = Modifier.height(24.dp))

    // Hobbies
    if (!profile.hobbies.isNullOrEmpty()) {
        PreferenceChipSection(title = "Hobbies", items = profile.hobbies.map { it.name })
    }
    // Interests
    if (!profile.interests.isNullOrEmpty()) {
        PreferenceChipSection(title = "Interests", items = profile.interests.map { it.name })
    }
    // Favorites
    if (!profile.favorites.isNullOrEmpty()) {
        PreferenceChipSection(title = "Favorites", items = profile.favorites.map { it.name })
    }
    // Music
    if (!profile.favoriteMusic.isNullOrEmpty()) {
        PreferenceChipSection(title = "Music", items = profile.favoriteMusic.map { it.name })
    }
    // Works
    if (!profile.works.isNullOrEmpty()) {
        PreferenceChipSection(title = "Work", items = profile.works.map { it.name })
    }
    // Schools
    if (!profile.schools.isNullOrEmpty()) {
        PreferenceChipSection(title = "Education", items = profile.schools.map { it.name })
    }
    // Achievements
    if (!profile.achievements.isNullOrEmpty()) {
        PreferenceChipSection(title = "Achievements", items = profile.achievements.map { it.name })
    }
    // Causes
    if (!profile.causes.isNullOrEmpty()) {
        PreferenceChipSection(title = "Causes", items = profile.causes.map { it.name })
    }
    // Lifestyle Tags
    if (!profile.lifestyleTags.isNullOrEmpty()) {
        PreferenceChipSection(title = "Lifestyle", items = profile.lifestyleTags.map { it.name })
    }
}
package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.UserProfile

data class CategoryTabItem(
    val title: String,
    val items: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ProfileCategoryTabs(profile: UserProfile) {
    val categories = buildCategories(profile).filter { it.items.isNotEmpty() }
    if (categories.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedCategory = categories[selectedIndex]

    Column(modifier = Modifier.fillMaxWidth()) {
        // Horizontal scrollable chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                val isSelected = index == selectedIndex
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedIndex = index },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = category.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color.Transparent,
                        disabledLabelColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color.Transparent else Color.LightGray,
                        borderWidth = 1.dp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items for selected category
        if (selectedCategory.items.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCategory.items.forEach { item ->
                    AssistChip(
                        onClick = { },
                        label = { Text(item) },
                        shape = RoundedCornerShape(20.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFF0F2F5),
                            labelColor = Color.Black
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
    }
}

private fun buildCategories(profile: UserProfile): List<CategoryTabItem> {
    return listOf(
        CategoryTabItem(
            title = "Hobbies",
            items = profile.hobbies?.map { it.name } ?: emptyList(),
            icon = Icons.Default.Favorite
        ),
        CategoryTabItem(
            title = "Interests",
            items = profile.interests?.map { it.name } ?: emptyList(),
            icon = Icons.Default.Star
        ),
        CategoryTabItem(
            title = "Favorites",
            items = profile.favorites?.map { it.name } ?: emptyList(),
            icon = Icons.Default.Star
        ),
        CategoryTabItem(
            title = "Music",
            items = profile.favoriteMusic?.map { it.name } ?: emptyList(),
            icon = Icons.Default.MusicNote
        ),
        CategoryTabItem(
            title = "Work",
            items = profile.works?.map { it.name } ?: emptyList(),
            icon = Icons.Default.Work
        ),
        CategoryTabItem(
            title = "Education",
            items = profile.schools?.map { it.name } ?: emptyList(),
            icon = Icons.Default.School
        ),
        CategoryTabItem(
            title = "Achievements",
            items = profile.achievements?.map { it.name } ?: emptyList(),
            icon = Icons.Default.EmojiEvents
        ),
        CategoryTabItem(
            title = "Causes",
            items = profile.causes?.map { it.name } ?: emptyList(),
            icon = Icons.Default.VolunteerActivism
        ),
        CategoryTabItem(
            title = "Lifestyle",
            items = profile.lifestyleTags?.map { it.name } ?: emptyList(),
            icon = Icons.Default.Spa
        )
    )
}
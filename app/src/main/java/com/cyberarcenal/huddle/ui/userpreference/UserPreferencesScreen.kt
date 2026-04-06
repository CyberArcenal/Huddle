// ui/userpreferences/UserPreferencesScreen.kt
package com.cyberarcenal.huddle.ui.userpreference

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.UserPreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPreferencesScreen(
    navController: NavController,
    viewModel: UserPreferencesViewModel = viewModel(
        factory = UserPreferencesViewModelFactory(UserPreferencesRepository())
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                PreferenceCategoryItem(
                    title = category.title(),
                    onItemClick = {
                        navController.navigate("preferences/edit/${category.name.lowercase()}")
                    }
                )
            }
        }
    }
}

@Composable
private fun PreferenceCategoryItem(title: String, onItemClick: () -> Unit) {
    Card(
        onClick = onItemClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



// Helper to get display title from category enum
fun PreferenceCategory.title(): String = when (this) {
    PreferenceCategory.HOBBIES -> "Hobbies"
    PreferenceCategory.INTERESTS -> "Interests"
    PreferenceCategory.FAVORITES -> "Favorites"
    PreferenceCategory.MUSIC -> "Music"
    PreferenceCategory.WORKS -> "Works"
    PreferenceCategory.SCHOOLS -> "Schools"
    PreferenceCategory.ACHIEVEMENTS -> "Achievements"
    PreferenceCategory.CAUSES -> "Causes"
    PreferenceCategory.LIFESTYLE_TAGS -> "Lifestyle Tags"
}
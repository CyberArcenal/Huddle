// ui/userpreferences/UserPreferencesScreen.kt
package com.cyberarcenal.huddle.ui.userpreference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                // Pinagsama ang TopBar padding at saktong 16.dp na margin
                top = paddingValues.calculateTopPadding() + 16.dp,
                // Pinagsama ang Bottom padding at 16.dp
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
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
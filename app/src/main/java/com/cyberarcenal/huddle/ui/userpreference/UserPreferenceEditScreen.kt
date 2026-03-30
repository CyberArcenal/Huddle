// ui/userpreference/UserPreferenceEditScreen.kt
package com.cyberarcenal.huddle.ui.userpreference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
fun UserPreferenceEditScreen(
    navController: NavController,
    categoryName: String,
    viewModel: UserPreferencesViewModel = viewModel(
        factory = UserPreferencesViewModelFactory(UserPreferencesRepository())
    )
) {
    val category = runCatching { PreferenceCategory.valueOf(categoryName.uppercase()) }
        .getOrElse {
            navController.popBackStack()
            return
        }

    val available by viewModel.available.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saving by viewModel.saving.collectAsState()

    var selectedIds by remember { mutableStateOf(selected.map { it.id }) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selected) {
        selectedIds = selected.map { it.id }
    }

    LaunchedEffect(category) {
        viewModel.loadPreferences(category)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.title()) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(
                            onClick = { viewModel.savePreferences(selectedIds) },
                            enabled = !saving
                        ) {
                            Text("Save")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        // Remove system insets to avoid extra spacing
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // only the top bar height is applied
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPreferences(category) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search field with no top padding
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp), // only horizontal padding
                            placeholder = { Text("Search...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                                // top = 0.dp (default)
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filtered = available.filter {
                                it.name.contains(searchQuery, ignoreCase = true)
                            }
                            items(filtered.size) { index ->
                                val item = filtered[index]
                                val isSelected = selectedIds.contains(item.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isSelected) {
                                                selectedIds - item.id
                                            } else {
                                                selectedIds + item.id
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) {
                                                selectedIds + item.id
                                            } else {
                                                selectedIds - item.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
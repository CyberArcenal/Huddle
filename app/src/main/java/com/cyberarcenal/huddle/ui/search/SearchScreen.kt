package com.cyberarcenal.huddle.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.SearchResult
import com.cyberarcenal.huddle.data.repositories.search.SearchRepository
import com.cyberarcenal.huddle.data.repositories.users.UsersRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            searchRepository = SearchRepository(),
            usersRepository = UsersRepository()
        )
    )
) {
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchResults = viewModel.searchResultsFlow.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Modern Search Bar Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Search people, huddles...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }

        // Error Handling
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 2. Results or Suggestions
        Box(modifier = Modifier.fillMaxSize()) {
            if (query.length in 1..2) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion, fontWeight = FontWeight.Medium) },
                            leadingContent = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            modifier = Modifier.clickable { viewModel.onQueryChange(suggestion) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp) // Space para sa floating nav
                ) {
                    // Title for results
                    if (searchResults.itemCount > 0) {
                        item {
                            Text(
                                "Search Results",
                                style = MaterialTheme.typography.labelLarge.copy(color = Color.Gray),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults[index]?.id ?: index }
                    ) { index ->
                        val result = searchResults[index]
                        result?.let {
                            SearchResultItem(result = it)
                        }
                    }

                    // Paging States
                    searchResults.apply {
                        when {
                            loadState.refresh is LoadState.Loading -> {
                                item { Box(Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 3.dp) } }
                            }
                            loadState.append is LoadState.Loading -> {
                                item { Box(Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(result: SearchResult) {
    val fullName = buildString {
        if (!result.firstName.isNullOrBlank()) append(result.firstName)
        if (!result.lastName.isNullOrBlank()) {
            if (isNotEmpty()) append(" ")
            append(result.lastName)
        }
        if (isEmpty()) append(result.username)
    }

    ListItem(
        modifier = Modifier.clickable { /* TODO: Navigate to Profile */ },
        headlineContent = {
            Text(
                text = result.username,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = fullName,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        },
        leadingContent = {
            // Modern Avatar with Coil
            AsyncImage(
                model = result.profilePictureUrl, // Gamitin ang model property kung meron na
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                // Fallback icon kung walang image
                error = null
            ) ?: Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(8.dp)
            )
        },
        trailingContent = {
            Button(
                onClick = { /* Follow logic */ },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// Factory remains the same
class SearchViewModelFactory(
    private val searchRepository: SearchRepository,
    private val usersRepository: UsersRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(searchRepository, usersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.cyberarcenal.huddle.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.SearchRepository
import com.cyberarcenal.huddle.data.repositories.SearchHistoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            SearchRepository(),
            SearchHistoryRepository(),          // for entity search
                // for suggestions (if needed)
        )
    )
) {
    val query by viewModel.searchQuery.collectAsState()
    val currentCategory by viewModel.searchCategory.collectAsState()
    val searchResults = viewModel.searchResultsFlow.collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Search Header
        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                TextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp)),
                    placeholder = { Text("Search Huddle...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = viewModel::clearSearch) { Icon(Icons.Default.Close, null) } },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchCategory.values()) { category ->
                        FilterChip(
                            selected = currentCategory == category,
                            onClick = { viewModel.onCategoryChange(category) },
                            label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Results
        Box(modifier = Modifier.fillMaxSize()) {
            if (query.length < 2) {
                EmptySearchPlaceholder()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = searchResults.itemCount) { index ->
                        val item = searchResults[index]
                        item?.let {
                            RenderSearchResult(it)
                        }
                    }

                    when (val state = searchResults.loadState.refresh) {
                        is LoadState.Loading -> item { Box(Modifier.fillParentMaxSize(), Alignment.Center) { CircularProgressIndicator() } }
                        is LoadState.Error -> item { Box(Modifier.fillParentMaxSize(), Alignment.Center) { Text("Error loading results") } }
                        else -> {}
                    }

                    if (searchResults.loadState.append is LoadState.Loading) {
                        item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(32.dp)) } }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderSearchResult(item: Any) {
    when (item) {
        is SearchResult -> { // USERS
            SearchListItem(
                title = item.username,
                subtitle = "${item.firstName} ${item.lastName}",
                image = item.profilePictureUrl,
                isCircle = true
            )
        }
        is GroupDisplay -> { // GROUPS
            SearchListItem(
                title = item.name,
                subtitle = "${item.memberCount} members",
                image = item.profilePicture?.toString(),
                isCircle = false
            )
        }
        is PostDisplay -> { // POSTS
            SearchListItem(
                title = item.content?.take(50) ?: "Post",
                subtitle = "By @${item.user?.username}",
                image = item.user?.profilePictureUrl,
                isCircle = true
            )
        }
        is EventList -> { // EVENTS
            SearchListItem(
                title = item.title ?: "Event",
                subtitle = item.location ?: "Online",
                image = null,
                isCircle = false
            )
        }
    }
}

@Composable
fun SearchListItem(title: String?, subtitle: String, image: String?, isCircle: Boolean) {
    ListItem(
        headlineContent = {
            if (title != null) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        },
        supportingContent = { Text(subtitle, color = Color.Gray, maxLines = 1) },
        leadingContent = {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(if (isCircle) CircleShape else RoundedCornerShape(8.dp)).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        }
    )
}

@Composable
fun EmptySearchPlaceholder() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("Search for people, posts, and more", color = Color.Gray)
    }
}
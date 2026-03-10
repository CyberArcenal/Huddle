package com.cyberarcenal.huddle.ui.events.eventslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.data.repositories.events.EventsRepository
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsListScreen(
    navController: NavController,
    viewModel: EventsListViewModel = viewModel(
        factory = EventsListViewModelFactory(EventsRepository())
    )
) {
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()
    val events = viewModel.eventsFlow.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("createevent") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Event")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search events...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            // Filter tabs
            FilterTabs(
                selectedFilter = filter,
                onFilterSelected = viewModel::setFilter
            )

            // Error display
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Events list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    count = events.itemCount,
                    key = { index -> events[index]?.id ?: index }
                ) { index ->
                    val event = events[index]
                    event?.let {
                        EventListItem(
                            event = it,
                            onClick = { viewModel.navigateToEventDetail(navController, it.id) }
                        )
                    }
                }

                // Loading states
                events.apply {
                    when (loadState.refresh) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is LoadState.Error -> {
                            val error = (loadState.refresh as LoadState.Error).error
                            item {
                                Text(
                                    text = "Error: ${error.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    when (loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LinearProgressIndicator()
                                }
                            }
                        }
                        is LoadState.Error -> {
                            val error = (loadState.append as LoadState.Error).error
                            item {
                                Text(
                                    text = "Error loading more: ${error.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }

                // Empty state
                if (events.itemCount == 0 && events.loadState.refresh is LoadState.NotLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No events found")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabs(
    selectedFilter: EventsFilter,
    onFilterSelected: (EventsFilter) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = when (selectedFilter) {
            is EventsFilter.Upcoming -> 0
            is EventsFilter.Past -> 1
            is EventsFilter.Type -> when (selectedFilter.type) {
                "public" -> 2
                "private" -> 3
                "group" -> 4
                else -> 0
            }
            EventsFilter.All -> 5
        },
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        edgePadding = 16.dp,
        indicator = {}
    ) {
        // Upcoming
        FilterChip(
            selected = selectedFilter is EventsFilter.Upcoming,
            onClick = { onFilterSelected(EventsFilter.Upcoming) },
            label = { Text("Upcoming") },
            modifier = Modifier.padding(end = 8.dp)
        )
        // Past
        FilterChip(
            selected = selectedFilter is EventsFilter.Past,
            onClick = { onFilterSelected(EventsFilter.Past) },
            label = { Text("Past") },
            modifier = Modifier.padding(end = 8.dp)
        )
        // Public
        FilterChip(
            selected = selectedFilter is EventsFilter.Type && selectedFilter.type == "public",
            onClick = { onFilterSelected(EventsFilter.Type("public")) },
            label = { Text("Public") },
            modifier = Modifier.padding(end = 8.dp)
        )
        // Private
        FilterChip(
            selected = selectedFilter is EventsFilter.Type && selectedFilter.type == "private",
            onClick = { onFilterSelected(EventsFilter.Type("private")) },
            label = { Text("Private") },
            modifier = Modifier.padding(end = 8.dp)
        )
        // Group events
        FilterChip(
            selected = selectedFilter is EventsFilter.Type && selectedFilter.type == "group",
            onClick = { onFilterSelected(EventsFilter.Type("group")) },
            label = { Text("Group") },
            modifier = Modifier.padding(end = 8.dp)
        )
        // All
        FilterChip(
            selected = selectedFilter is EventsFilter.All,
            onClick = { onFilterSelected(EventsFilter.All) },
            label = { Text("All") },
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
fun EventListItem(
    event: EventList,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date indicator
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = event.startTime.format(DateTimeFormatter.ofPattern("MMM")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = event.startTime.format(DateTimeFormatter.ofPattern("dd")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Event details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = formatEventTime(event.startTime, event.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Attendees count chip
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.attendeesCount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

private fun formatEventTime(start: OffsetDateTime, end: OffsetDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return "${start.format(formatter)} - ${end.format(formatter)}"
}

// Factory
class EventsListViewModelFactory(
    private val eventsRepository: EventsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventsListViewModel(eventsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.cyberarcenal.huddle.ui.events.attendies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.cyberarcenal.huddle.api.models.EventAttendanceWithUser
import com.cyberarcenal.huddle.api.models.PersonalityTypeEnum
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository
import com.cyberarcenal.huddle.ui.common.user.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventAttendeesScreen(
    eventId: Int,
    navController: NavController,
    attendanceRepository: EventAttendanceRepository,
    friendshipsRepository: FriendshipsRepository,
    globalSnackbarHostState: SnackbarHostState
) {
    val viewModel: EventAttendeesViewModel = viewModel(
        factory = EventAttendeesViewModelFactory(eventId, attendanceRepository, friendshipsRepository)
    )

    val attendees = viewModel.attendeesPagingFlow.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showFriendsOnly by viewModel.showFriendsOnly.collectAsState()
    val selectedPersonality by viewModel.selectedPersonalityType.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendees") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Badge(
                            containerColor = if (showFriendsOnly || selectedPersonality != null)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (showFriendsOnly || selectedPersonality != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showFriendsOnly) {
                            AssistChip(
                                onClick = { viewModel.toggleFriendsOnly() },
                                label = { Text("Friends only") },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, null) }
                            )
                        }
                        selectedPersonality?.let { type ->
                            AssistChip(
                                onClick = { viewModel.updatePersonalityFilter(null) },
                                label = { Text(type) }
                            )
                        }
                    }
                }
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
                placeholder = { Text("Search attendees...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            // Sort dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                SortDropdown(
                    selected = sortOption,
                    onSortSelected = viewModel::updateSortOption
                )
            }

            // Attendee list (Paging)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    count = attendees.itemCount,
                    key = attendees.itemKey { it.user?.id ?: it.hashCode() }
                ) { index ->
                    val attendee = attendees[index]
                    attendee?.let {
                        AttendeeCard(
                            attendee = it,
                            onClick = { it.user?.id?.let { userId -> navController.navigate("profile/$userId") } }
                        )
                    }
                }

                if (attendees.loadState.refresh is androidx.paging.LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (attendees.loadState.refresh is androidx.paging.LoadState.Error) {
                    item {
                        Text("Error loading attendees", modifier = Modifier.padding(16.dp))
                    }
                }

                if (attendees.loadState.append is androidx.paging.LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            showFriendsOnly = showFriendsOnly,
            selectedPersonality = selectedPersonality,
            onFriendsOnlyToggle = viewModel::toggleFriendsOnly,
            onPersonalitySelected = viewModel::updatePersonalityFilter,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun SortDropdown(selected: EventAttendeesViewModel.SortOption, onSortSelected: (EventAttendeesViewModel.SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Sort: ${selected.name.replace('_', ' ')}")
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventAttendeesViewModel.SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name.replace('_', ' ')) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AttendeeCard(attendee: EventAttendanceWithUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                url = attendee.user?.profilePictureUrl,
                username = attendee.user?.username,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attendee.user?.fullName ?: attendee.user?.username ?: "User",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@${attendee.user?.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Personality badge
                attendee.user?.personalityType?.value?.let { type ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                // Hobbies preview (first 2)
                val hobbies = attendee.user?.hobbies?.take(2) ?: emptyList()
                if (hobbies.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        hobbies.forEach { hobby ->
                            AssistChip(
                                onClick = { },
                                label = { Text(hobby.name ?: "", fontSize = 10.sp) },
                                enabled = false
                            )
                        }
                        if ((attendee.user?.hobbies?.size ?: 0) > 2) {
                            Text("+${(attendee.user?.hobbies?.size ?: 0) - 2}", fontSize = 10.sp)
                        }
                    }
                }
            }
            // Capability score
            attendee.user?.capabilityScore?.let { score ->
                if (score > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$score%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    showFriendsOnly: Boolean,
    selectedPersonality: String?,
    onFriendsOnlyToggle: () -> Unit,
    onPersonalitySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Filter Attendees", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Friends only filter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showFriendsOnly,
                    onCheckedChange = { onFriendsOnlyToggle() }
                )
                Text("Friends attending")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Personality type filter
            Text("Personality Type")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PersonalityTypeEnum.values().toList()) { type ->
                    FilterChip(
                        selected = selectedPersonality == type.value,
                        onClick = {
                            if (selectedPersonality == type.value) onPersonalitySelected(null)
                            else onPersonalitySelected(type.value)
                        },
                        label = { Text(type.value ?: "") }
                    )
                }
            }
        }
    }
}
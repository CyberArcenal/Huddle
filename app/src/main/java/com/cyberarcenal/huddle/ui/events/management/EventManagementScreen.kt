package com.cyberarcenal.huddle.ui.events.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.EventAnalyticsSummary
import com.cyberarcenal.huddle.api.models.EventAttendanceWithUser
import com.cyberarcenal.huddle.api.models.EventDetail
import com.cyberarcenal.huddle.api.models.PatchedEventUpdateRequest
import com.cyberarcenal.huddle.data.repositories.EventAnalyticsRepository
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.user.Avatar
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen(
    eventId: Int,
    navController: NavController,
    eventRepository: EventRepository,
    attendanceRepository: EventAttendanceRepository,
    analyticsRepository: EventAnalyticsRepository,
    globalSnackbarHostState: SnackbarHostState
) {
    val viewModel: EventManagementViewModel = viewModel(
        factory = EventManagementViewModelFactory(eventId, eventRepository, attendanceRepository, analyticsRepository)
    )

    val event by viewModel.event.collectAsState()
    val attendees by viewModel.attendees.collectAsState()
    val pendingAttendees by viewModel.pendingAttendees.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                globalSnackbarHostState.showSnackbar((actionState as ActionState.Success).message)
                viewModel.clearActionState()
            }
            is ActionState.Error -> {
                globalSnackbarHostState.showSnackbar((actionState as ActionState.Error).message)
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (event == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Event not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tabs
                var selectedTab by remember { mutableStateOf(0) }
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp
                ) {
                    listOf("Overview", "Attendees", "Analytics").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> OverviewTab(event = event!!)
                    1 -> AttendeesTab(
                        event = event!!,
                        attendees = attendees,
                        pendingAttendees = pendingAttendees,
                        onApprove = viewModel::approveAttendee,
                        onReject = viewModel::rejectAttendee,
                        onRemove = viewModel::removeAttendee
                    )
                    2 -> AnalyticsTab(analytics = analytics)
                }
            }
        }
    }

    if (showEditDialog) {
        EditEventDialog(
            event = event!!,
            onDismiss = { showEditDialog = false },
            onSave = { updateRequest ->
                viewModel.updateEvent(updateRequest)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun OverviewTab(event: EventDetail) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Event Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Title", event.title ?: "Untitled")
                    InfoRow("Type", event.eventType?.value?.replaceFirstChar { it.uppercase() } ?: "Public")
                    InfoRow("Date & Time", formatDateTimeRange(event.startTime, event.endTime))
                    InfoRow("Location", event.location ?: "TBA")
                    InfoRow("Attendees", "${event.attendeesCount ?: 0} / ${event.maxAttendees?.toString() ?: "∞"}")
                    InfoRow("Status", when {
                        event.isFull == true -> "Full"
                        event.startTime != null && event.startTime.isBefore(OffsetDateTime.now()) -> "Ongoing / Completed"
                        else -> "Upcoming"
                    })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(event.description ?: "No description")
                }
            }
        }
    }
}

@Composable
private fun AttendeesTab(
    event: EventDetail,
    attendees: List<EventAttendanceWithUser>,
    pendingAttendees: List<EventAttendanceWithUser>,
    onApprove: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val isPrivate = event.eventType?.value == "private"
    var selectedSection by remember { mutableStateOf(0) } // 0 = confirmed, 1 = pending

    Column(modifier = Modifier.fillMaxSize()) {
        if (isPrivate && pendingAttendees.isNotEmpty()) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text("Confirmed (${attendees.size})") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text("Pending (${pendingAttendees.size})") }
                )
            }
        }

        val list = if (selectedSection == 0 || !isPrivate) attendees else pendingAttendees
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list, key = { it.user?.id ?: it.hashCode() }) { attendance ->
                AttendeeManagementCard(
                    attendance = attendance,
                    isPending = selectedSection == 1 && isPrivate,
                    onApprove = { onApprove(attendance.user?.id ?: return@AttendeeManagementCard) },
                    onReject = { onReject(attendance.user?.id ?: return@AttendeeManagementCard) },
                    onRemove = { onRemove(attendance.user?.id ?: return@AttendeeManagementCard) }
                )
            }
            if (list.isEmpty()) {
                item {
                    Text("No attendees", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AttendeeManagementCard(
    attendance: EventAttendanceWithUser,
    isPending: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                url = attendance.user?.profilePictureUrl,
                username = attendance.user?.username,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attendance.user?.fullName ?: attendance.user?.username ?: "User",
                    fontWeight = FontWeight.Bold
                )
                Text("@${attendance.user?.username}", style = MaterialTheme.typography.bodySmall)
                attendance.user?.personalityType?.value?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (isPending) {
                Row {
                    IconButton(onClick = onApprove) {
                        Icon(Icons.Default.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onReject) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTab(analytics: EventAnalyticsSummary?) {
    if (analytics == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No analytics data available")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RSVP Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total changes: ${analytics.totalRsvpChanges}")
                        Text("Avg per day: ${String.format("%.1f", analytics.avgChangesPerDay)}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    analytics.currentRsvpCounts?.let { counts ->
                        Text("Going: ${counts["going"] ?: 0}")
                        Text("Maybe: ${counts["maybe"] ?: 0}")
                        Text("Declined: ${counts["declined"] ?: 0}")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    analytics.dailyBreakdown?.take(7)?.forEach { day ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(day["date"]?.toString() ?: "", style = MaterialTheme.typography.bodySmall)
                            Text("Changes: ${day["changes"] ?: 0}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun EditEventDialog(
    event: EventDetail,
    onDismiss: () -> Unit,
    onSave: (PatchedEventUpdateRequest) -> Unit
) {
    var title by remember { mutableStateOf(event.title ?: "") }
    var description by remember { mutableStateOf(event.description ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var maxAttendees by remember { mutableStateOf(event.maxAttendees?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
                OutlinedTextField(value = maxAttendees, onValueChange = { maxAttendees = it }, label = { Text("Max Attendees") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val request = PatchedEventUpdateRequest(
                    title = title,
                    description = description,
                    location = location,
                    maxAttendees = maxAttendees.toLongOrNull()
                )
                onSave(request)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDateTimeRange(start: OffsetDateTime?, end: OffsetDateTime?): String {
    if (start == null) return "TBD"
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
    val startStr = start.format(formatter)
    if (end == null) return startStr
    val endStr = end.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return "$startStr - $endStr"
}
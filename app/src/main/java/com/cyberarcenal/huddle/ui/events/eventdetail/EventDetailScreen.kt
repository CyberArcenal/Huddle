package com.cyberarcenal.huddle.ui.events.eventdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.cyberarcenal.huddle.api.models.EventDetail
import com.cyberarcenal.huddle.api.models.EventStatistics
import com.cyberarcenal.huddle.api.models.StatusDecEnum
import com.cyberarcenal.huddle.data.repositories.events.EventsRepository
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    navController: NavController,
    eventId: Int,
    viewModel: EventDetailViewModel = viewModel(
        factory = EventDetailViewModelFactory(eventId, EventsRepository())
    )
) {
    val event by viewModel.eventState.collectAsState()
    val stats by viewModel.statsState.collectAsState()
    val loading by viewModel.eventLoading.collectAsState()
    val error by viewModel.eventError.collectAsState()
    val rsvpState by viewModel.rsvpState.collectAsState()
    val userAttendance by viewModel.currentUserAttendance.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(rsvpState) {
        when (rsvpState) {
            is RsvpState.Success -> {
                snackbarHostState.showSnackbar((rsvpState as RsvpState.Success).message)
                viewModel.clearRsvpState()
            }
            is RsvpState.Error -> {
                snackbarHostState.showSnackbar((rsvpState as RsvpState.Error).message)
                viewModel.clearRsvpState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(event?.title ?: "Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            event?.let { ev ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // Nagdagdag ng spacing sa pagitan ng buttons
                    ) {
                        // Going
                        RSVPButton(
                            text = "Going",
                            selected = userAttendance?.status == StatusDecEnum.GOING,
                            onClick = { viewModel.rsvp(StatusDecEnum.GOING) },
                            enabled = rsvpState !is RsvpState.Loading,
                            modifier = Modifier.weight(1f) // FIX: Dito inilagay ang weight
                        )
                        // Maybe
                        RSVPButton(
                            text = "Maybe",
                            selected = userAttendance?.status == StatusDecEnum.MAYBE,
                            onClick = { viewModel.rsvp(StatusDecEnum.MAYBE) },
                            enabled = rsvpState !is RsvpState.Loading,
                            modifier = Modifier.weight(1f) // FIX: Dito inilagay ang weight
                        )
                        // Decline
                        RSVPButton(
                            text = "Decline",
                            selected = userAttendance?.status == StatusDecEnum.DECLINED,
                            onClick = { viewModel.rsvp(StatusDecEnum.DECLINED) },
                            enabled = rsvpState !is RsvpState.Loading,
                            modifier = Modifier.weight(1f) // FIX: Dito inilagay ang weight
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            event?.let { ev ->
                item {
                    EventDetailContent(
                        event = ev,
                        stats = stats
                    )
                }
            }
        }

        if (loading && event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error?.let { err ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(err, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = viewModel::refresh) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun RSVPButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Idinagdag ang modifier parameter para sa weight
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier // Dito gagamitin ang weight modifier na pinasa mula sa Row
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun EventDetailContent(
    event: EventDetail,
    stats: EventStatistics?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title and organizer
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Organized by ${event.organizer?.username ?: "Unknown"}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date and time
        InfoRow(
            icon = Icons.Outlined.Event,
            text = event.startTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
        )
        InfoRow(
            icon = Icons.Outlined.Schedule,
            text = "${event.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        )
        InfoRow(
            icon = Icons.Outlined.LocationOn,
            text = event.location
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Stats
        stats?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = it.goingCount.toString(), label = "Going")
                    StatItem(value = it.maybeCount.toString(), label = "Maybe")
                    StatItem(value = it.declinedCount.toString(), label = "Declined")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = event.description,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// Factory
class EventDetailViewModelFactory(
    private val eventId: Int,
    private val eventsRepository: EventsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventDetailViewModel(eventId, eventsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
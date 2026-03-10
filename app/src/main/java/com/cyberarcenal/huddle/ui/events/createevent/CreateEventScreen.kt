package com.cyberarcenal.huddle.ui.events.createevent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.EventType8c2Enum
import com.cyberarcenal.huddle.data.repositories.events.EventsRepository
import com.cyberarcenal.huddle.ui.theme.Gradients
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    viewModel: CreateEventViewModel = viewModel(
        factory = CreateEventViewModelFactory(EventsRepository())
    )
) {
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val location by viewModel.location.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val eventType by viewModel.eventType.collectAsState()
    val maxAttendees by viewModel.maxAttendees.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val titleError by viewModel.titleError.collectAsState()
    val dateError by viewModel.dateError.collectAsState()

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    LaunchedEffect(createState) {
        when (createState) {
            is CreateEventState.Success -> {
                val event = (createState as CreateEventState.Success).event
                navController.navigate("eventdetail/${event.id}") {
                    popUpTo("createevent") { inclusive = true }
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Event Title") },
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(titleError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::updateLocation,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Date/Time selection
            Text(
                text = "Start Date & Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(startDate?.format(dateFormatter) ?: "Select Date")
                }
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(startDate?.format(timeFormatter) ?: "Select Time")
                }
            }

            Text(
                text = "End Date & Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(endDate?.format(dateFormatter) ?: "Select Date")
                }
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(endDate?.format(timeFormatter) ?: "Select Time")
                }
            }

            if (dateError != null) {
                Text(
                    text = dateError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Event type
            Text(
                text = "Event Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventTypeChip(
                    selected = eventType == EventType8c2Enum.PUBLIC,
                    label = "Public",
                    onClick = { viewModel.updateEventType(EventType8c2Enum.PUBLIC) },
                    modifier = Modifier.weight(1f)
                )
                EventTypeChip(
                    selected = eventType == EventType8c2Enum.PUBLIC,
                    label = "Private",
                    onClick = { viewModel.updateEventType(EventType8c2Enum.PRIVATE) },
                    modifier = Modifier.weight(1f)
                )
                EventTypeChip(
                    selected = eventType == EventType8c2Enum.GROUP,
                    label = "Group",
                    onClick = { viewModel.updateEventType(EventType8c2Enum.GROUP) },
                            modifier = androidx . compose . ui . Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Group ID (optional) – could be a dropdown, but for simplicity just a text field
            if (eventType == EventType8c2Enum.GROUP) {
                OutlinedTextField(
                    value = viewModel.groupId.collectAsState().value?.toString() ?: "",
                    onValueChange = { viewModel.updateGroupId(it.toIntOrNull()) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Max attendees (optional)
            OutlinedTextField(
                value = maxAttendees?.toString() ?: "",
                onValueChange = viewModel::updateMaxAttendees,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Max Attendees (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            // Create button
            Button(
                onClick = viewModel::createEvent,
                enabled = createState !is CreateEventState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (createState !is CreateEventState.Loading) Gradients.buttonGradient else Gradients.disabledGradient,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (createState is CreateEventState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            "Create Event",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            if (createState is CreateEventState.Error) {
                Text(
                    text = (createState as CreateEventState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date picker dialogs
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { date ->
                val newDateTime = date?.atTime(startDate?.toLocalTime() ?: LocalTime.NOON)?.atZone(ZoneId.systemDefault())?.toOffsetDateTime()
                viewModel.updateStartDate(newDateTime)
                showStartDatePicker = false
            }
        )
    }
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { time ->
                val newDateTime = time?.let { startDate?.with(it) } ?: startDate
                viewModel.updateStartDate(newDateTime)
                showStartTimePicker = false
            }
        )
    }
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { date ->
                val newDateTime = date?.atTime(endDate?.toLocalTime() ?: LocalTime.NOON)?.atZone(ZoneId.systemDefault())?.toOffsetDateTime()
                viewModel.updateEndDate(newDateTime)
                showEndDatePicker = false
            }
        )
    }
    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = { time ->
                val newDateTime = time?.let { endDate?.with(it) } ?: endDate
                viewModel.updateEndDate(newDateTime)
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // IDINAGDAG PARA SA WEIGHT
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        modifier = modifier // DITO GAGAMITIN ANG WEIGHT
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate?) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                DatePicker(state = datePickerState)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            onDateSelected(selectedDate)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime?) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// Factory
class CreateEventViewModelFactory(
    private val eventsRepository: EventsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateEventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateEventViewModel(eventsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
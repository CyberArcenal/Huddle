package com.cyberarcenal.huddle.ui.events.createEvent

import android.icu.util.Calendar
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleLocationStep(
    uiState: EventCreateUiState,
    onStartTimeChange: (OffsetDateTime) -> Unit,
    onEndTimeChange: (OffsetDateTime) -> Unit,
    onLocationChange: (String) -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Schedule & Location", style = MaterialTheme.typography.headlineSmall)

        // Start date/time
        OutlinedTextField(
            value = uiState.startTime?.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")) ?: "",
            onValueChange = {},
            label = { Text("Start Date & Time *") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = { IconButton({ showStartDatePicker = true }) { Icon(Icons.Default.DateRange, null) } }
        )

        // End date/time
        OutlinedTextField(
            value = uiState.endTime?.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")) ?: "",
            onValueChange = {},
            label = { Text("End Date & Time (optional)") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = { IconButton({ showEndDatePicker = true }) { Icon(Icons.Default.DateRange, null) } }
        )

        // Location
        OutlinedTextField(
            value = uiState.location,
            onValueChange = onLocationChange,
            label = { Text("Location *") },
            modifier = Modifier.fillMaxWidth()
        )

        if (showStartDatePicker) {
            DateTimePickerDialog(
                initialDateTime = uiState.startTime,
                onConfirm = { dateTime ->
                    onStartTimeChange(dateTime)
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }

        if (showEndDatePicker) {
            DateTimePickerDialog(
                initialDateTime = uiState.endTime,
                onConfirm = { dateTime ->
                    onEndTimeChange(dateTime)
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialDateTime: OffsetDateTime?,
    onConfirm: (OffsetDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime?.hour ?: 12,
        initialMinute = initialDateTime?.minute ?: 0
    )

    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val localDateTime = LocalDateTime.of(
                        date.year,
                        date.month,
                        date.dayOfMonth,
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    val offsetDateTime = localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime()
                    onConfirm(offsetDateTime)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Back")
                }
            },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

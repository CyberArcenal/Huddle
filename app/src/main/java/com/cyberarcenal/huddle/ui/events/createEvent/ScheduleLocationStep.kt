package com.cyberarcenal.huddle.ui.events.createEvent

import android.icu.util.Calendar
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
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
            .padding(16.dp),
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
                onConfirm = { dateTime ->
                    onStartTimeChange(dateTime)
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }

        if (showEndDatePicker) {
            DateTimePickerDialog(
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
fun DateTimePickerDialog(onConfirm: (OffsetDateTime) -> Unit, onDismiss: () -> Unit) {
    // Simple date/time picker using native or custom
    // For brevity, using a basic dialog with DatePicker + TimePicker
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date & Time") },
        text = {
            Column {
                DatePicker(state = rememberDatePickerState())
                TimePicker(state = rememberTimePickerState())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val offsetDateTime = OffsetDateTime.now()
                    .withYear(selectedDate.get(Calendar.YEAR))
                    .withMonth(selectedDate.get(Calendar.MONTH) + 1)
                    .withDayOfMonth(selectedDate.get(Calendar.DAY_OF_MONTH))
                    .withHour(selectedTime.get(Calendar.HOUR_OF_DAY))
                    .withMinute(selectedTime.get(Calendar.MINUTE))
                    .withSecond(0)
                    .withNano(0)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                onConfirm(offsetDateTime)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
package com.cyberarcenal.huddle.ui.events.createEvent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttendeesGroupStep(
    uiState: EventCreateUiState,
    onMaxAttendeesChange: (Long?) -> Unit,
    onGroupIdChange: (Int?) -> Unit
) {
    var unlimited by remember { mutableStateOf(uiState.maxAttendees == null) }
    var maxAttendeesText by remember { mutableStateOf(uiState.maxAttendees?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Attendees & Group", style = MaterialTheme.typography.headlineSmall)

        // Max attendees
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = unlimited,
                onCheckedChange = { checked ->
                    unlimited = checked
                    if (checked) onMaxAttendeesChange(null)
                }
            )
            Text("Unlimited attendees")
        }
        if (!unlimited) {
            OutlinedTextField(
                value = maxAttendeesText,
                onValueChange = { text ->
                    maxAttendeesText = text
                    val value = text.toLongOrNull()
                    onMaxAttendeesChange(value)
                },
                label = { Text("Max Attendees") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }

        // Group selector (simplified – in real app, fetch user's groups)
        // For now, just a text field
        OutlinedTextField(
            value = uiState.groupId?.toString() ?: "",
            onValueChange = { text ->
                onGroupIdChange(text.toIntOrNull())
            },
            label = { Text("Group ID (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
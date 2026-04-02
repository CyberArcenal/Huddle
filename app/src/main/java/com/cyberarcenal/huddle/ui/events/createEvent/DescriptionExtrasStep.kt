package com.cyberarcenal.huddle.ui.events.createEvent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DescriptionExtrasStep(
    uiState: EventCreateUiState,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Description & Extras", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Event Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8
        )

        // Future: Tags / Advanced options
        Text("Advanced options coming soon...", style = MaterialTheme.typography.bodySmall)
    }
}
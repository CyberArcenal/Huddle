package com.cyberarcenal.huddle.ui.events.createEvent

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.EventType8c2Enum

@Composable
fun BasicInfoStep(
    uiState: EventCreateUiState,
    onTitleChange: (String) -> Unit,
    onEventTypeChange: (EventType8c2Enum) -> Unit,
    onAddCoverImage: () -> Unit,
    onRemoveMedia: (Uri) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            label = { Text("Event Title *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Event Type chips
        Text("Event Type")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventTypeChip(
                label = "Public",
                selected = uiState.eventType == EventType8c2Enum.PUBLIC,
                onClick = { onEventTypeChange(EventType8c2Enum.PUBLIC) }
            )
            EventTypeChip(
                label = "Private",
                selected = uiState.eventType == EventType8c2Enum.PRIVATE,
                onClick = { onEventTypeChange(EventType8c2Enum.PRIVATE) }
            )
            EventTypeChip(
                label = "Group",
                selected = uiState.eventType == EventType8c2Enum.GROUP,
                onClick = { onEventTypeChange(EventType8c2Enum.GROUP) }
            )
        }

        // Cover image
        Text("Cover Image (optional)")
        if (uiState.selectedMedia.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.selectedMedia) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemoveMedia(uri) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
        Button(onClick = onAddCoverImage) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Image")
        }
    }
}

@Composable
fun EventTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
package com.cyberarcenal.huddle.ui.groups.management.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.ui.groups.management.GroupManagementViewModel

@Composable
fun EventsManagementTab(
    events: LazyPagingItems<EventList>,
    viewModel: GroupManagementViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = { /* Navigate to create event screen */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create New Event")
            }
        }
        items(events.itemCount) { index ->
            val event = events[index]
            event?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(event.title ?: "Event", fontWeight = FontWeight.Bold)
                            Text(event.startTime?.toString() ?: "No date", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { /* Edit event */ }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteEvent(event.id ?: return@IconButton) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
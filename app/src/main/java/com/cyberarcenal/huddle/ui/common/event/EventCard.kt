package com.cyberarcenal.huddle.ui.common.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.StatusDecEnum
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventCard(
    event: EventList,
    onClick: () -> Unit,
    onRsvp: (StatusDecEnum) -> Unit
) {
    var showRsvpMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row: Title + Overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showRsvpMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "RSVP options")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Group / Organizer info
            if (event.group != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "organizer"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.group.name ?: "Group", style = MaterialTheme.typography.labelSmall)
                }
            } else if (event.organizer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "organizer"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.organizer.username ?: "Organizer", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, modifier = Modifier.size(16.dp), tint =
                    MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = "")
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDateTime(event.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Location
            if (!event.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, modifier = Modifier.size(16.dp), tint =
                        MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = "")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attendees count + max
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, modifier = Modifier.size(16.dp), tint =
                        MaterialTheme.colorScheme.primary, contentDescription = "")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${event.attendeesCount ?: 0} / ${event.maxAttendees?.toString() ?: "∞"} attending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (event.isFull == true) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                        Text("FULL", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                } else {
                    // RSVP button (small)
                    Button(
                        onClick = { onRsvp(StatusDecEnum.GOING) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("RSVP", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // RSVP options dropdown
    if (showRsvpMenu) {
        DropdownMenu(
            expanded = showRsvpMenu,
            onDismissRequest = { showRsvpMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Going") },
                onClick = {
                    onRsvp(StatusDecEnum.GOING)
                    showRsvpMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Maybe") },
                onClick = {
                    onRsvp(StatusDecEnum.MAYBE)
                    showRsvpMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Decline") },
                onClick = {
                    onRsvp(StatusDecEnum.DECLINED)
                    showRsvpMenu = false
                }
            )
        }
    }
}

private fun formatDateTime(dateTime: OffsetDateTime?): String {
    if (dateTime == null) return "TBD"
    return dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault()))
}
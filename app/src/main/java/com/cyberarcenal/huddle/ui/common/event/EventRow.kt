package com.cyberarcenal.huddle.ui.common.event
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.cyberarcenal.huddle.api.models.EventList

@Composable
fun EventsRow(
    title: String = "Upcoming Events",
    events: List<EventList>,
    onEventClick: (EventList) -> Unit,
    onShowMoreClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Horizontal scroll of events
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(events, key = { "event_${it.id ?: it.hashCode()}" }) { event ->
                EventItem(
                    event = event,
                    isVertical = true,
                    onItemClick = { onEventClick(event) }
                )
            }

            // Show More card sa dulo
            item(key = "events_show_more") {
                SeeMoreEventCard(
                    onSeeMoreClick = { onShowMoreClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

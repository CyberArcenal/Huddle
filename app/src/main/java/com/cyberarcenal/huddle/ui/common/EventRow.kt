package com.cyberarcenal.huddle.ui.common
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.GroupMinimal
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.UserMinimal
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
            items(events, key = { it.id ?: it.hashCode() }) { event ->
                EventItem(
                    event = event,
                    isVertical = true,
                    onItemClick = { onEventClick(event) }
                )
            }

            // Show More card sa dulo
            item {
                    ShowMoreCard(
                        onClick = {onShowMoreClick}
                    )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

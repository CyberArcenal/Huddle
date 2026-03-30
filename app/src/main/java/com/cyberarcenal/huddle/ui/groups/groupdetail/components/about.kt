package com.cyberarcenal.huddle.ui.groups.groupdetail.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.GroupDisplay


@Composable
fun AboutContent(group: GroupDisplay?) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExpandableText(
            text = group?.description ?: "No description provided.",
            maxLines = 3,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Group Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group?.groupTypeDisplay ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Rules",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "1. Be respectful\n2. No spam\n3. Follow the group's theme",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Created",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group?.createdAt?.let { DateUtils.formatDateTime(
                LocalContext.current,
                it.toInstant().toEpochMilli(),
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
            ) } ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
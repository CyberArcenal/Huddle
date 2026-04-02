package com.cyberarcenal.huddle.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.LoginSession

@Composable
fun SessionItem(
    session: LoginSession,
    onTerminate: () -> Unit,
    isCurrent: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon based on device type
            Icon(
                when (session.deviceType?.lowercase()) {
                    "mobile" -> Icons.Outlined.PhoneAndroid
                    "desktop" -> Icons.Outlined.Computer
                    else -> Icons.Outlined.Devices
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Session details
            Column(modifier = Modifier.weight(1f)) {
                session.deviceName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Last used: ${session.formattedLastUsed ?: session.lastUsed?.toString() ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                session.ipAddress?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            if (!isCurrent) {
                IconButton(onClick = onTerminate) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Terminate",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
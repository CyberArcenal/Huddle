package com.cyberarcenal.huddle.ui.storyviewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryOptionsBottomSheet(
    story: Story,
    onDismiss: () -> Unit,
    onDelete: (Int) -> Unit,
    onArchive: (Int) -> Unit,
    onAddToHighlight: (Int) -> Unit,
    onSave: (Int) -> Unit,
) {
    val storyId = story.id ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            OptionItem(
                icon = Icons.Default.Delete,
                text = "Delete Story",
                onClick = { onDelete(storyId) },
                tint = MaterialTheme.colorScheme.error
            )
            Divider()
            OptionItem(
                icon = Icons.Default.Archive,
                text = "Archive",
                onClick = { onArchive(storyId) }
            )
            Divider()
            OptionItem(
                icon = Icons.Default.Favorite,
                text = "Add to Highlight",
                onClick = { onAddToHighlight(storyId) }
            )
            Divider()
            OptionItem(
                icon = Icons.Default.Download,
                text = "Save to Gallery",
                onClick = { onSave(storyId) }
            )
        }
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(text = text, color = tint)
    }
}
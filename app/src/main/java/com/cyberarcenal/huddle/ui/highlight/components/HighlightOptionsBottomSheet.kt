package com.cyberarcenal.huddle.ui.highlight.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.StoryHighlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightOptionsBottomSheet(
    highlight: StoryHighlight,
    onDismiss: () -> Unit,
    onEdit: (StoryHighlight) -> Unit,
    onDelete: (StoryHighlight) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = highlight.title ?: "Highlight Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Edit Highlight") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.clickable {
                    onEdit(highlight)
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Delete Highlight", color = MaterialTheme.colorScheme.error) },
                leadingContent = { 
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.error 
                    ) 
                },
                modifier = Modifier.clickable {
                    onDelete(highlight)
                    onDismiss()
                }
            )
        }
    }
}

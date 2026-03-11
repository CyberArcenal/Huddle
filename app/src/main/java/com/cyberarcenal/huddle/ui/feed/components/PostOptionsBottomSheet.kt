package com.cyberarcenal.huddle.ui.feed.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.PostFeed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: PostFeed,
    isCurrentUser: Boolean,
    onDismiss: () -> Unit,
    onDelete: (Int) -> Unit,
    onReport: (Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        ReportDialog(
            postId = post.id!!,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                onReport(post.id!!, reason)
                showReportDialog = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (isCurrentUser) {
                // Delete option
                OptionsItem(
                    icon = Icons.Default.Delete,
                    text = "Delete Post",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            onDelete(post.id!!)
                        }
                    }
                )
                // Edit option (if needed)
                OptionsItem(
                    icon = Icons.Default.Edit,
                    text = "Edit Post",
                    onClick = {
                        // Navigate to edit screen
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        // TODO: navigate to edit post
                    }
                )
            } else {
                // Report option
                OptionsItem(
                    icon = Icons.Default.Report,
                    text = "Report Post",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showReportDialog = true
                    }
                )
            }
            // Cancel option
            OptionsItem(
                icon = Icons.Default.Close,
                text = "Cancel",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            )
        }
    }
}




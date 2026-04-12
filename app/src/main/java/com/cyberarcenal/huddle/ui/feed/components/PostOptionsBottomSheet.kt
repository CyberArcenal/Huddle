package com.cyberarcenal.huddle.ui.feed.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
                // Edit option
                OptionsItem(
                    icon = Icons.Outlined.Edit,
                    text = "Edit Post",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        // TODO: navigate to edit post
                    }
                )

                // Archive option
                OptionsItem(
                    icon = Icons.Outlined.Archive,
                    text = "Move to Archive",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            // TODO: viewModel.archivePost(post.id!!)
                        }
                    }
                )

                // Delete/Trash option
                OptionsItem(
                    icon = Icons.Outlined.Delete,
                    text = "Move to Trash",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            onDelete(post.id!!)
                        }
                    }
                )
            } else {
                // Report option
                OptionsItem(
                    icon = Icons.Outlined.Report,
                    text = "Report Post",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showReportDialog = true
                    }
                )
            }
            // Cancel option
            OptionsItem(
                icon = Icons.Outlined.Close,
                text = "Cancel",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            )
        }
    }
}




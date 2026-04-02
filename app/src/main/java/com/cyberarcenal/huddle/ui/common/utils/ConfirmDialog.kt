package com.cyberarcenal.huddle.ui.common.utils

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Confirm",
    message: String = "Are you sure?",
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isConfirmDangerous: Boolean = false
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = if (isConfirmDangerous) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissText)
                }
            }
        )
    }
}
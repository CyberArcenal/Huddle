package com.cyberarcenal.huddle.ui.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class ConfirmState(
    initialShow: Boolean = false,
    initialTitle: String = "Confirm",
    initialMessage: String = "Are you sure?",
    initialConfirmText: String = "Confirm",
    initialDismissText: String = "Cancel",
    initialOnConfirm: () -> Unit = {},
    initialIsDangerous: Boolean = false
) {
    var showDialog by mutableStateOf(initialShow)
    var title by mutableStateOf(initialTitle)
    var message by mutableStateOf(initialMessage)
    var confirmText by mutableStateOf(initialConfirmText)
    var dismissText by mutableStateOf(initialDismissText)
    var onConfirm by mutableStateOf(initialOnConfirm)
    var isDangerous by mutableStateOf(initialIsDangerous)

    fun show(
        title: String = this.title,
        message: String = this.message,
        confirmText: String = this.confirmText,
        dismissText: String = this.dismissText,
        onConfirm: () -> Unit,
        isDangerous: Boolean = false
    ) {
        this.title = title
        this.message = message
        this.confirmText = confirmText
        this.dismissText = dismissText
        this.onConfirm = onConfirm
        this.isDangerous = isDangerous
        showDialog = true
    }

    fun hide() {
        showDialog = false
    }
}

@Composable
fun rememberConfirmState(): ConfirmState = remember { ConfirmState() }
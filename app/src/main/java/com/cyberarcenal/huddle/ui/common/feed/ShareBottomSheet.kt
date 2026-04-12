package com.cyberarcenal.huddle.ui.common.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    onDismiss: () -> Unit,
    onShare: (ShareRequestData) -> Unit,
    contentType: String,
    contentId: Int
) {
    var caption by remember { mutableStateOf("") }
    var selectedPrivacy by remember { mutableStateOf(PrivacyB23Enum.PUBLIC) }
    var showPrivacyOptions by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Share Content",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Caption Input
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Say something about this...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Selector
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyOptions = !showPrivacyOptions }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when(selectedPrivacy) {
                        PrivacyB23Enum.PUBLIC -> Icons.Default.Public
                        PrivacyB23Enum.FOLLOWERS -> Icons.Default.Group
                        PrivacyB23Enum.SECRET -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedPrivacy.value.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showPrivacyOptions) {
                PrivacyOptionsList(
                    currentPrivacy = selectedPrivacy,
                    onPrivacySelected = {
                        selectedPrivacy = it
                        showPrivacyOptions = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share Button
            Button(
                onClick = {
                    onShare(
                        ShareRequestData(
                            contentType = contentType,
                            contentId = contentId,
                            caption = caption.ifBlank { null },
                            privacy = selectedPrivacy
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Share Now")
            }
        }
    }
}

@Composable
fun PrivacyOptionsList(
    currentPrivacy: PrivacyB23Enum,
    onPrivacySelected: (PrivacyB23Enum) -> Unit
) {
    val options = listOf(
        PrivacyB23Enum.PUBLIC to "Anyone can see this",
        PrivacyB23Enum.FOLLOWERS to "Only your followers",
        PrivacyB23Enum.SECRET to "Only you can see this"
    )

    Column(modifier = Modifier.padding(start = 32.dp)) {
        options.forEach { (privacy, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPrivacySelected(privacy) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentPrivacy == privacy,
                    onClick = { onPrivacySelected(privacy) }
                )
                Column {
                    Text(text = privacy.value.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                    Text(text = description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

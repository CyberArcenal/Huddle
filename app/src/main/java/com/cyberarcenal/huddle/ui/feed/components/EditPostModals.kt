package com.cyberarcenal.huddle.ui.feed.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostModal(
    post: PostFeed,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, List<Int>) -> Unit
) {
    var content by remember { mutableStateOf(post.content) }
    var location by remember { mutableStateOf(post.location ?: "") }
    // Note: Feelings and TagUsers would normally need their own selection logic/models
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Post", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onSave(content, location.ifBlank { null }, null, emptyList()) }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Placeholder for Feelings and Tag Users
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Add Feeling / Activity")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Tag Users")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPrivacyModal(
    currentPrivacy: PrivacyB23Enum?,
    onDismiss: () -> Unit,
    onPrivacySelected: (PrivacyB23Enum) -> Unit
) {
    val options = listOf(
        PrivacyOption(PrivacyB23Enum.PUBLIC, "Public", "Anyone can see this post", Icons.Default.Public),
        PrivacyOption(PrivacyB23Enum.FOLLOWERS, "Followers", "Only your followers", Icons.Default.Group),
        PrivacyOption(PrivacyB23Enum.SECRET, "Secret", "Only you can see this", Icons.Default.Lock)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Post Privacy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPrivacySelected(option.value) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(option.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.label, fontWeight = FontWeight.Bold)
                        Text(option.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    RadioButton(
                        selected = currentPrivacy == option.value,
                        onClick = { onPrivacySelected(option.value) }
                    )
                }
            }
        }
    }
}

private data class PrivacyOption(
    val value: PrivacyB23Enum,
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

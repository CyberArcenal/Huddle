package com.cyberarcenal.huddle.ui.groups.management.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.GroupDisplay
import com.cyberarcenal.huddle.api.models.GroupTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyC6eEnum
import com.cyberarcenal.huddle.ui.groups.management.GroupManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoTab(
    group: GroupDisplay?,
    viewModel: GroupManagementViewModel
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var description by remember { mutableStateOf(group?.description ?: "") }
    var privacy by remember { mutableStateOf(group?.privacy ?: PrivacyC6eEnum.PUBLIC) }
    var groupType by remember { mutableStateOf(group?.groupType ?: GroupTypeEnum.HOBBY) }

    // Update when group loads
    LaunchedEffect(group) {
        group?.let {
            name = it.name
            description = it.description
            privacy = it.privacy ?: PrivacyC6eEnum.PUBLIC
            groupType = it.groupType ?: GroupTypeEnum.HOBBY
        }
    }

    val scrollState = rememberScrollState()

    // Image pickers
    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadCoverPhoto(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile picture
        Text("Profile Picture", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = group?.profilePicture,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Button(onClick = { profilePicker.launch("image/*") }) {
                Text("Change")
            }
        }

        // Cover photo
        Text("Cover Photo", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = group?.coverPhoto,
                contentDescription = null,
                modifier = Modifier.height(100.dp).fillMaxWidth()
            )
            Button(onClick = { coverPicker.launch("image/*") }) {
                Text("Change")
            }
        }

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // Privacy
        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = {}
        ) {
            Text("Privacy: ${privacy.value}", modifier = Modifier.fillMaxWidth())
            // You can implement dropdown here
        }

        // Group Type
        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = {}
        ) {
            Text("Group Type: ${groupType.value}", modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { viewModel.updateGroup(name, description, privacy, groupType) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}
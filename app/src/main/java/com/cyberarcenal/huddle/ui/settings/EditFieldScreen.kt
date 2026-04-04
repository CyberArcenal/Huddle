package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFieldScreen(
    navController: NavController,
    fieldName: String,
    currentValue: String,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            userProfileRepository = UsersRepository(),
            userSecurityRepository = UserSecurityRepository(),
            passwordResetRepository = PasswordResetRepository(),
            logOutRepository = LogOutRepository()
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    var value by remember { mutableStateOf(currentValue) }
    var isLoading by remember { mutableStateOf(false) }

    // Map field name to user-friendly label
    val label = when (fieldName) {
        "first_name" -> "First Name"
        "last_name" -> "Last Name"
        "phone" -> "Phone Number"
        "bio" -> "Bio"
        "location" -> "Location"
        "date_of_birth" -> "Date of Birth"
        else -> fieldName.replace("_", " ").capitalize()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit $label") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = fieldName != "bio"  // bio can be multi-line
            )
            Button(
                onClick = {
                    isLoading = true
                    viewModel.updateProfileField(fieldName, value) { success, message ->
                        isLoading = false
                        if (success) {
                            globalSnackbarHostState.showSnackbar("$label updated")
                            navController.popBackStack()
                        } else {
                            globalSnackbarHostState.showSnackbar(message ?: "Update failed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = value != currentValue && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
                }
            }
        }
    }
}
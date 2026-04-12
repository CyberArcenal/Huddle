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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
    val profileUpdateState by viewModel.profileUpdateState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var value by remember { mutableStateOf(currentValue) }

    // Sync local value when currentValue changes (e.g., after refresh)
    LaunchedEffect(currentValue) {
        value = currentValue
    }

    // Map field name to user-friendly label
    val label = when (fieldName) {
        "first_name" -> "First Name"
        "last_name" -> "Last Name"
        "phone" -> "Phone Number"
        "bio" -> "Bio"
        "location" -> "Location"
        "date_of_birth" -> "Date of Birth"
        else -> fieldName.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    // Observe snackbar messages and show them, then navigate on success
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            coroutineScope.launch {
                val snackbarText = when (message) {
                    is SnackbarMessage.Success -> message.message
                    is SnackbarMessage.Error -> message.message
                    is SnackbarMessage.Info -> message.message
                }
                globalSnackbarHostState.showSnackbar(snackbarText)
                viewModel.clearSnackbar()

                // If update succeeded, go back
                if (message is SnackbarMessage.Success &&
                    message.message.contains(label, ignoreCase = true)
                ) {
                    navController.popBackStack()
                }
            }
        }
    }

    val isLoading = profileUpdateState is ProfileUpdateState.Loading
    val isChanged = value != currentValue && value.isNotBlank()
    val isButtonEnabled = isChanged && !isLoading

    // Optional: Validate date format
    fun isValidDate(dateStr: String): Boolean {
        return if (fieldName == "date_of_birth") {
            try {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                true
            } catch (e: DateTimeParseException) {
                false
            }
        } else true
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                singleLine = fieldName != "bio",
                isError = fieldName == "date_of_birth" && value.isNotBlank() && !isValidDate(value),
                supportingText = {
                    when {
                        fieldName == "date_of_birth" && value.isNotBlank() && !isValidDate(value) -> {
                            Text("Use format YYYY-MM-DD", color = MaterialTheme.colorScheme.error)
                        }
                        fieldName == "bio" -> {
                            Text("Tell others about yourself (max 500 characters)")
                        }
                        else -> {}
                    }
                }
            )
            Button(
                onClick = {
                    if (fieldName == "date_of_birth" && !isValidDate(value)) {
                        // Invalid date, don't call update
                        coroutineScope.launch {
                            globalSnackbarHostState.showSnackbar("Invalid date format. Use YYYY-MM-DD")
                        }
                        return@Button
                    }
                    viewModel.updateProfileField(fieldName, value) // No callback needed
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isButtonEnabled
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
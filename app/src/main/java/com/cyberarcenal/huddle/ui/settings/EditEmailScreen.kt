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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmailScreen(
    navController: NavController,
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
    val profile by viewModel.userProfile.collectAsState()
    val profileUpdateState by viewModel.profileUpdateState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf(profile?.email ?: "") }

    // Update local email when profile changes (e.g., after refresh)
    LaunchedEffect(profile?.email) {
        email = profile?.email ?: ""
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

                // If email update succeeded, go back
                if (message is SnackbarMessage.Success &&
                    message.message.contains("Email updated", ignoreCase = true)
                ) {
                    navController.popBackStack()
                }
            }
        }
    }

    val isLoading = profileUpdateState is ProfileUpdateState.Loading
    val isButtonEnabled = email.isNotBlank() && email != profile?.email && !isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Email") },
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
                value = email,
                onValueChange = { email = it },
                label = { Text("New Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("Enter a valid email address")
                }
            )
            Button(
                onClick = {
                    viewModel.updateEmail(email) // no callback needed
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
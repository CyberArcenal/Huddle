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
fun EditUsernameScreen(
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

    var username by remember { mutableStateOf(profile?.username ?: "") }

    // Sync local value when profile changes (after refresh)
    LaunchedEffect(profile?.username) {
        username = profile?.username ?: ""
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

                // If username update succeeded, go back
                if (message is SnackbarMessage.Success &&
                    message.message.contains("Username updated", ignoreCase = true)
                ) {
                    navController.popBackStack()
                }
            }
        }
    }

    val isLoading = profileUpdateState is ProfileUpdateState.Loading
    val isButtonEnabled = username.isNotBlank() && username != profile?.username && !isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Username") },
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
                value = username,
                onValueChange = { username = it },
                label = { Text("New Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("3-30 characters, letters, numbers, underscores, dots")
                }
            )
            Button(
                onClick = {
                    viewModel.updateUsername(username) // no callback needed
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
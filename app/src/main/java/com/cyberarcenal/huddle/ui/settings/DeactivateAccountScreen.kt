package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeactivateAccountScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            userProfileRepository = UsersRepository(),
            userSecurityRepository = UserSecurityRepository(),
            passwordResetRepository = PasswordResetRepository(),
            logOutRepository = LogOutRepository()
        )
    )
) {
    var password by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    val deactivationState by viewModel.deactivationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deactivationState) {
        when (deactivationState) {
            is AccountDeactivationState.Success -> {
                snackbarHostState.showSnackbar((deactivationState as AccountDeactivationState.Success).message)
                viewModel.clearDeactivationState()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
            is AccountDeactivationState.Error -> {
                snackbarHostState.showSnackbar((deactivationState as AccountDeactivationState.Error).message)
                viewModel.clearDeactivationState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Deactivate Account") },
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
            Text(
                "This action is irreversible. Your account will be deactivated and you will lose access to all data.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text("I understand the consequences")
            }
            Button(
                onClick = { viewModel.deactivateAccount(password, confirmed) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = deactivationState !is AccountDeactivationState.Loading && confirmed && password.isNotBlank()
            ) {
                if (deactivationState is AccountDeactivationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onError)
                } else {
                    Text("Deactivate Account")
                }
            }
        }
    }
}
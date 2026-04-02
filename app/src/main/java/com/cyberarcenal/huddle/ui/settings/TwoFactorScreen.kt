package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorScreen(
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
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsState()
    val twoFactorState by viewModel.twoFactorState.collectAsState()
    var otpCode by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(twoFactorState) {
        when (twoFactorState) {
            is TwoFactorState.Success -> {
                snackbarHostState.showSnackbar(if ((twoFactorState as TwoFactorState.Success).enabled) "2FA enabled" else "2FA disabled")
                viewModel.clearTwoFactorState()
                navController.popBackStack()
            }
            is TwoFactorState.Error -> {
                snackbarHostState.showSnackbar((twoFactorState as TwoFactorState.Error).message)
                viewModel.clearTwoFactorState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (twoFactorEnabled) "Disable 2FA" else "Enable 2FA") },
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
            if (twoFactorEnabled) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.disable2FA(currentPassword) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disable 2FA")
                }
            } else {
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("Verification Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.enable2FA(otpCode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable 2FA")
                }
            }
        }
    }
}
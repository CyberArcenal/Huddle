package com.cyberarcenal.huddle.ui.auth.forgotpassword

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            viewModel.resetSuccess()
            navController.navigate("login") {
                popUpTo("forgot_password") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    if (uiState.step != ForgotPasswordStep.Request) {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.step) {
                ForgotPasswordStep.Request -> RequestStep(uiState, viewModel, navController)
                ForgotPasswordStep.VerifyOtp -> VerifyOtpStep(uiState, viewModel)
                ForgotPasswordStep.ResetPassword -> ResetPasswordStep(uiState, viewModel)
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun RequestStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter your email address to receive a password reset code.")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::onRequestResetClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Reset Code")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Back to Login")
        }
        if (!uiState.error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
        }
        if (!uiState.message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.message!!, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun VerifyOtpStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter the 6‑digit code sent to ${uiState.email}")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.otp,
            onValueChange = viewModel::onOtpChange,
            label = { Text("OTP Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::onVerifyOtpClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify Code")
        }
        if (!uiState.error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ResetPasswordStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter your new password")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            label = { Text("New Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::onResetPasswordClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Password")
        }
        if (!uiState.error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
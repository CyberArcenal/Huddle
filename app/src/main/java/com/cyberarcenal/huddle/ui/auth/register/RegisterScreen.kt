package com.cyberarcenal.huddle.ui.auth.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            viewModel.resetSuccess()
            navController.navigate("login") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Show success message as a snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.currentStep == 2) "Verify Email" else "Register") },
                navigationIcon = {
                    if (uiState.currentStep > 0) {
                        IconButton(onClick = { viewModel.previousStep() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Progress Indicator - fixed
            LinearProgressIndicator(
                progress = (uiState.currentStep + 1) / 3f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (uiState.currentStep) {
                0 -> PersonalInfoStep(uiState, viewModel)
                1 -> SecurityStep(uiState, viewModel)
                2 -> VerificationStep(uiState, viewModel)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.currentStep < 2) {
                Button(
                    onClick = {
                        if (uiState.currentStep == 0) viewModel.nextStep()
                        else viewModel.onRegisterSubmit()
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(if (uiState.currentStep == 0) "Next" else "Create Account")
                    }
                }
            } else {
                Button(
                    onClick = viewModel::onVerifyOtp,
                    enabled = !uiState.isLoading && uiState.otp.length == 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Verify and Join")
                    }
                }

                TextButton(onClick = viewModel::resendOtp) {
                    Text("Didn't receive code? Resend")
                }
            }

            // Error Message with Surface (like login)
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalInfoStep(uiState: RegisterUiState, viewModel: RegisterViewModel) {
    Column {
        Text("Personal Information", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tell us a bit about yourself", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Last Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = viewModel::onPhoneNumberChange,
            label = { Text("Phone Number (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
    }
}

@Composable
fun SecurityStep(uiState: RegisterUiState, viewModel: RegisterViewModel) {
    Column {
        Text("Security", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Create a strong password for your account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Must match and be at least 8 characters.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VerificationStep(uiState: RegisterUiState, viewModel: RegisterViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Check your email", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We've sent a 6-digit verification code to\n${uiState.email}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.otp,
            onValueChange = { if (it.length <= 6) viewModel.onOtpChange(it) },
            label = { Text("Verification Code") },
            modifier = Modifier.fillMaxWidth(0.7f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                letterSpacing = 8.sp,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}
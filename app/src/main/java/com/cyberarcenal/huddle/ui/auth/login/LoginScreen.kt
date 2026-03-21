package com.cyberarcenal.huddle.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.network.AuthManager
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle navigation and data saving
    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            val accessToken = uiState.accessToken
            val refreshToken = uiState.refreshToken
            val userProfile = uiState.userProfile

            if (accessToken != null && refreshToken != null && userProfile != null) {
                // We are already in a coroutine scope provided by LaunchedEffect
                AuthManager.saveTokens(context, accessToken, refreshToken)
                TokenManager.saveUser(context, userProfile)
                TokenManager.updateToken(accessToken)
            }
            
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
            viewModel.resetNavigation()
        }
    }

    LaunchedEffect(uiState.navigateTo2FA) {
        if (uiState.navigateTo2FA) {
            val token = uiState.checkpointToken ?: return@LaunchedEffect
            navController.navigate("verify_2fa/$token")
            viewModel.resetNavigation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / App Name
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "H",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                "Sign in to continue to Huddle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email Field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email Address") },
                placeholder = { Text("example@email.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { /* Handle forgot password */ }) {
                    Text("Forgot Password?", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = { viewModel.onLoginClick() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { navController.navigate("register") }) {
                    Text(
                        "Sign Up",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

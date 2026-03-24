package com.cyberarcenal.huddle.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.TokenVerifyRequestRequest
import com.cyberarcenal.huddle.data.repositories.TokenRepository
import com.cyberarcenal.huddle.network.AuthManager
import com.cyberarcenal.huddle.network.TokenManager
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSessionExpiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load token from AuthManager (shared preferences / DataStore)
        val token = AuthManager.getAccessToken(context)
        TokenManager.updateToken(token)

        if (token == null) {
            // No token, go to login
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // Verify token
            val result = TokenRepository().verifyToken(TokenVerifyRequestRequest(token = token))
            result.onSuccess { response ->
                if (response.valid) {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    // Token invalid, show dialog
                    showSessionExpiredDialog = true
                }
            }.onFailure {
                // Network error or invalid token
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Session Expired") },
            text = { Text("Your session has expired. Please log in again.") },
            confirmButton = {
                TextButton(onClick = {
                    showSessionExpiredDialog = false
                    scope.launch {
                        // Token invalid, clear and go to login
                        AuthManager.clearTokens(context)
                        TokenManager.updateToken(null)
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

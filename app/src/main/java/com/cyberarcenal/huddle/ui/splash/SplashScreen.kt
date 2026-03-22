package com.cyberarcenal.huddle.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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

    LaunchedEffect(Unit) {
        scope.launch {
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
                    // Assume response has a field like "is_valid" or "valid"
                    // If not, we can treat 200 as valid and any failure as invalid
                    if (response.valid) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        // Token invalid, clear and go to login
                        AuthManager.clearTokens(context)
                        TokenManager.updateToken(null)
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }.onFailure {
                    // Network error or invalid token
                    AuthManager.clearTokens(context)
                    TokenManager.updateToken(null)
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
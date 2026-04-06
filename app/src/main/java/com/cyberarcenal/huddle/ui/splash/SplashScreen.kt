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
            // OPTIMISTIC NAVIGATION: Go to home immediately if we have a token
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
            
            // BACKGROUND VERIFICATION:
            // Pwede ring i-verify ang token sa background dito (gamit ang GlobalScope o sa HomeViewModel)
            // Pero sa ngayon, ang pag-navigate agad ang magpapabilis sa launch experience.
        }
    }

    // Tinanggal muna natin ang AlertDialog dito dahil madi-dispose ang SplashScreen
    // kapag nag-navigate na sa Home. Ang session verification ay dapat handle na ng 
    // network interceptor o ng Home Screen para hindi blocking sa start-up.


    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

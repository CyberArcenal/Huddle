package com.cyberarcenal.huddle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyberarcenal.huddle.network.ApiService
import com.cyberarcenal.huddle.ui.home.HomeScreen
import com.cyberarcenal.huddle.ui.auth.login.LoginScreen
import com.cyberarcenal.huddle.ui.auth.register.RegisterScreen
import com.cyberarcenal.huddle.ui.notifications.NotificationsScreen
import com.cyberarcenal.huddle.ui.theme.HuddleTheme
import com.cyberarcenal.huddle.data.reactionPicker.ReactionPickerLayout
import com.cyberarcenal.huddle.data.videoPlayer.DefaultVideoPositionProvider
import com.cyberarcenal.huddle.data.videoPlayer.VideoPlayerLayout
import com.cyberarcenal.huddle.ui.auth.forgotpassword.ForgotPasswordScreen
import com.cyberarcenal.huddle.network.TokenManager

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiService.init(this)
        enableEdgeToEdge()
        setContent {
            HuddleTheme {
                VideoPlayerLayout(
                    modifier = Modifier.fillMaxSize(),
                    positionProvider = DefaultVideoPositionProvider(minVisiblePercentage = 0.3f)
                ) {
                    ReactionPickerLayout(modifier = Modifier.fillMaxSize().padding(0.dp)) {
                        Surface(modifier = Modifier.fillMaxSize().padding(0.dp)) {
                            HuddleApp()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HuddleApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = TokenManager.getAccessToken(context)
        startDestination = if (token != null) "home" else "login"
    }

    AnimatedContent(
        targetState = startDestination,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
        },
        label = "StartDestinationAnimation"
    ) { targetDestination ->
        if (targetDestination != null) {
            NavHost(
                navController = navController,
                startDestination = targetDestination,
                enterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400)) { it / 3 } },
                exitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400)) { -it / 3 } },
                popEnterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400)) { -it / 3 } },
                popExitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400)) { it / 3 } }
            ) {
                composable("login") { LoginScreen(navController) }
                composable("register") { RegisterScreen(navController = navController) }
                composable("home") { HomeScreen(navController) }
                composable("notifications") { NotificationsScreen(navController = navController) }
                composable("forgot-password") { ForgotPasswordScreen(navController) }
            }
        }
    }
}
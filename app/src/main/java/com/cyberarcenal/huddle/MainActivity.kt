package com.cyberarcenal.huddle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyberarcenal.huddle.network.ApiService
import com.cyberarcenal.huddle.ui.home.HomeScreen
import com.cyberarcenal.huddle.ui.auth.login.LoginScreen
import com.cyberarcenal.huddle.ui.auth.register.RegisterScreen
import com.cyberarcenal.huddle.ui.notifications.NotificationsScreen
import com.cyberarcenal.huddle.ui.splash.SplashScreen
import com.cyberarcenal.huddle.ui.theme.HuddleTheme
import com.cyberarcenal.huddle.data.reactionPicker.ReactionPickerLayout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiService.init(this)
        enableEdgeToEdge()
        setContent {
            HuddleTheme {
                ReactionPickerLayout(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        HuddleApp()
                    }
                }
            }
        }
    }
}

@Composable
fun HuddleApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }

        // DAGDAG MO ITO:
        composable("register") {
            RegisterScreen(navController = navController)
        }

        composable("home") { HomeScreen(navController) }

        composable("notifications") { NotificationsScreen(navController = navController) }
    }
}
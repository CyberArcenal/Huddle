package com.cyberarcenal.huddle.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    secondary = Pink,
    onSecondary = Color.White,
    tertiary = Orange,
    onTertiary = Color.White,
    background = Color(0xFF121212), // Madilim na background para sa Dark Mode
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    secondary = Pink,
    onSecondary = Color.White,
    tertiary = Orange,
    onTertiary = Color.White,
    background = OffWhite,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal
)

@Composable
fun HuddleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // I-set sa false kung gusto mong laging brand colors ang gamit imbes na wallpaper colors
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Gawing Transparent ang status bar at nav bar
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            val windowInsetsController = WindowCompat.getInsetsController(window, view)

            // Kontrolin ang kulay ng icons (Signal, Battery, etc.)
            // Sa Light Theme (puting bg), dapat dark icons.
            // Sa Dark Theme (itim na bg), dapat light icons.
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

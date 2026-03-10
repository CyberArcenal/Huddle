package com.cyberarcenal.huddle.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Gradients {
    val primaryGradient = Brush.linearGradient(
        colors = listOf(DeepPurple, Pink, Orange)
    )

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Pink, Orange)
    )

    // IDAGDAG ITO: Kulay para sa disabled buttons
    val disabledGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD1D1D1), Color(0xFFA8A8A8))
    )
}
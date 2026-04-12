package com.cyberarcenal.huddle.ui.dating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.ui.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatingProfileScreen(
    userId: Int,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    // For now, we can either wrap the existing ProfileScreen 
    // or build a custom one. Let's start by using the existing one 
    // but maybe adding a "Like" button if it's from the dating flow.
    
    Box(modifier = Modifier.fillMaxSize()) {
        ProfileScreen(
            userId = userId,
            navController = navController,
            globalSnackbarHostState = globalSnackbarHostState
        )
        
        // Custom Back Button for Dating Flow
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

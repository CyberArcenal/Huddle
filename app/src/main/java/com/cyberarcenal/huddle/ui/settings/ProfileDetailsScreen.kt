package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            userProfileRepository = UsersRepository(),
            userSecurityRepository = UserSecurityRepository(),
            passwordResetRepository = PasswordResetRepository(),
            logOutRepository = LogOutRepository()
        )
    )
) {
    val profile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoRow(label = "Username", value = profile?.username ?: "")
            }
            item {
                InfoRow(label = "Email", value = profile?.email ?: "")
            }
            item {
                InfoRow(label = "Phone", value = profile?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set")
            }
            item {
                InfoRow(label = "Bio", value = profile?.bio?.takeIf { it.isNotBlank() } ?: "No bio")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.utils.ConfirmDialog
import com.cyberarcenal.huddle.ui.common.utils.rememberConfirmState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            userProfileRepository = UsersRepository(),
            userSecurityRepository = UserSecurityRepository(),
            passwordResetRepository = PasswordResetRepository(),
            logOutRepository = LogOutRepository()
        )
    ),
    mainNav: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val confirmState = rememberConfirmState()
    val logoutState by viewModel.logoutState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(logoutState) {
        when (logoutState) {
            is LogoutState.Success -> {
                globalSnackbarHostState.showSnackbar((logoutState as LogoutState.Success).message)
                viewModel.clearLogoutState()
                mainNav.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
            is LogoutState.Error -> {
                globalSnackbarHostState.showSnackbar((logoutState as LogoutState.Error).message)
                viewModel.clearLogoutState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(globalSnackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings items
            item {
                SettingsNavItem(
                    icon = Icons.Outlined.Person,
                    title = "Profile Details",
                    onClick = { navController.navigate("settings_profile_details") }
                )
            }
            item {
                SettingsNavItem(
                    icon = Icons.Outlined.Lock,
                    title = "Security",
                    onClick = { navController.navigate("settings_security") }
                )
            }
            item {
                SettingsNavItem(
                    icon = Icons.Outlined.Devices,
                    title = "Where You're Logged In",
                    onClick = { navController.navigate("settings_sessions") }
                )
            }
            item {
                SettingsNavItem(
                    icon = Icons.Outlined.MoreHoriz,
                    title = "More",
                    onClick = { navController.navigate("settings_more") }
                )
            }

            // Logout button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        confirmState.show(
                            title = "Logout",
                            message = "Are you sure you want to logout?",
                            confirmText = "Logout",
                            isDangerous = true,
                            onConfirm = {
                                viewModel.logout(context)
                                confirmState.hide()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = logoutState !is LogoutState.Loading
                ) {
                    if (logoutState is LogoutState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text("Logout")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    // Ilagay ang ConfirmDialog sa pinakadulo ng composable
    ConfirmDialog(
        showDialog = confirmState.showDialog,
        onDismiss = { confirmState.hide() },
        onConfirm = confirmState.onConfirm,
        title = confirmState.title,
        message = confirmState.message,
        confirmText = confirmState.confirmText,
        dismissText = confirmState.dismissText,
        isConfirmDangerous = confirmState.isDangerous
    )
}

@Composable
fun SettingsNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
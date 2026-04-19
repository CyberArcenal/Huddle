package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.cyberarcenal.huddle.ui.common.utils.ConfirmDialog
import com.cyberarcenal.huddle.ui.common.utils.rememberConfirmState
import com.cyberarcenal.huddle.ui.settings.components.SessionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            userProfileRepository = UsersRepository(),
            userSecurityRepository = UserSecurityRepository(),
            passwordResetRepository = PasswordResetRepository(),
            logOutRepository = LogOutRepository()
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val confirmState = rememberConfirmState()
    val sessions by viewModel.sessions.collectAsState()
    val sessionState by viewModel.sessionActionState.collectAsState()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionActionState.Success -> {
                globalSnackbarHostState.showSnackbar((sessionState as SessionActionState.Success).message)
                viewModel.clearSessionActionState()
            }
            is SessionActionState.Error -> {
                globalSnackbarHostState.showSnackbar((sessionState as SessionActionState.Error).message)
                viewModel.clearSessionActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(globalSnackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Where You're Logged In") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sessions.size > 1) {
                        TextButton(
                            onClick = {
                                confirmState.show(
                                    title = "Log Out of All Other Sessions",
                                    message = "This will log you out of all devices except this one.",
                                    confirmText = "Log Out All",
                                    isDangerous = true,
                                    onConfirm = {
                                        viewModel.terminateAllOtherSessions()
                                        confirmState.hide()
                                    }
                                )
                            },
                            enabled = sessionState !is SessionActionState.Loading,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Log Out Others", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(sessions) { session ->
                SessionItem(
                    session = session,
                    onTerminate = {
                        confirmState.show(
                            title = "Terminate Session",
                            message = "Are you sure you want to log out of this device?",
                            confirmText = "Log Out",
                            isDangerous = true,
                            onConfirm = {
                                viewModel.terminateSession(session.id)
                                confirmState.hide()
                            }
                        )
                    },
                    isCurrent = session.isCurrent == true
                )
                if (session != sessions.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    ConfirmDialog(
        showDialog = confirmState.showDialog,
        onDismiss = { confirmState.hide() },
        onConfirm = confirmState.onConfirm,
        title = confirmState.title,
        message = confirmState.message,
        confirmText = confirmState.confirmText,
        isConfirmDangerous = confirmState.isDangerous
    )
}
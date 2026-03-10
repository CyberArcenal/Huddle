package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.LoginSession
import com.cyberarcenal.huddle.data.repositories.users.UsersRepository
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(UsersRepository())
    )
) {
    val profile by viewModel.userProfile.collectAsState()
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val passwordState by viewModel.passwordChangeState.collectAsState()
    val twoFactorState by viewModel.twoFactorState.collectAsState()
    val sessionState by viewModel.sessionActionState.collectAsState()
    val deactivationState by viewModel.deactivationState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialogs state
    var showPasswordDialog by remember { mutableStateOf(false) }
    var show2FADialog by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Observe action results
    LaunchedEffect(passwordState) {
        when (passwordState) {
            is PasswordChangeState.Success -> {
                snackbarHostState.showSnackbar((passwordState as PasswordChangeState.Success).message)
                viewModel.clearPasswordState()
                showPasswordDialog = false
            }
            is PasswordChangeState.Error -> {
                snackbarHostState.showSnackbar((passwordState as PasswordChangeState.Error).message)
                viewModel.clearPasswordState()
            }
            else -> {}
        }
    }
    LaunchedEffect(twoFactorState) {
        when (twoFactorState) {
            is TwoFactorState.Success -> {
                val enabled = (twoFactorState as TwoFactorState.Success).enabled
                snackbarHostState.showSnackbar(if (enabled) "2FA enabled" else "2FA disabled")
                viewModel.clearTwoFactorState()
                show2FADialog = false
            }
            is TwoFactorState.Error -> {
                snackbarHostState.showSnackbar((twoFactorState as TwoFactorState.Error).message)
                viewModel.clearTwoFactorState()
            }
            else -> {}
        }
    }
    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionActionState.Success -> {
                snackbarHostState.showSnackbar((sessionState as SessionActionState.Success).message)
                viewModel.clearSessionActionState()
            }
            is SessionActionState.Error -> {
                snackbarHostState.showSnackbar((sessionState as SessionActionState.Error).message)
                viewModel.clearSessionActionState()
            }
            else -> {}
        }
    }
    LaunchedEffect(deactivationState) {
        when (deactivationState) {
            is AccountDeactivationState.Success -> {
                snackbarHostState.showSnackbar((deactivationState as AccountDeactivationState.Success).message)
                viewModel.clearDeactivationState()
                // Navigate to login
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
            is AccountDeactivationState.Error -> {
                snackbarHostState.showSnackbar((deactivationState as AccountDeactivationState.Error).message)
                viewModel.clearDeactivationState()
            }
            else -> {}
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Account Section
            item {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = "Username",
                    subtitle = profile?.username ?: "",
                    onClick = { /* edit username? */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Email,
                    title = "Email",
                    subtitle = profile?.email ?: "",
                    onClick = { /* edit email? */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Phone,
                    title = "Phone",
                    subtitle = profile?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set",
                    onClick = { /* edit phone */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Bio",
                    subtitle = profile?.bio?.takeIf { it.isNotBlank() } ?: "No bio",
                    onClick = { /* edit bio */ }
                )
            }

            // Security Section
            item {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = "Password",
                    subtitle = "Change your password",
                    onClick = { showPasswordDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Shield,
                    title = "Two-Factor Authentication",
                    subtitle = if (twoFactorEnabled) "Enabled" else "Disabled",
                    onClick = { show2FADialog = true }
                )
            }

            // Sessions Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (sessions.size > 1) {
                        TextButton(
                            onClick = { viewModel.terminateAllOtherSessions() },
                            enabled = sessionState !is SessionActionState.Loading
                        ) {
                            Text("Terminate All Others")
                        }
                    }
                }
            }
            items(sessions) { session ->
                SessionItem(
                    session = session,
                    onTerminate = { viewModel.terminateSession(session.id) },
                    isCurrent = session.isCurrent
                )
            }

            // Danger Zone
            item {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Delete,
                    title = "Deactivate Account",
                    subtitle = "Permanently deactivate your account",
                    onClick = { showDeactivateDialog = true },
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                    },
                    enabled = passwordState !is PasswordChangeState.Loading
                ) {
                    if (passwordState is PasswordChangeState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Change")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2FA Dialog
    if (show2FADialog) {
        AlertDialog(
            onDismissRequest = { show2FADialog = false },
            title = { Text(if (twoFactorEnabled) "Disable 2FA" else "Enable 2FA") },
            text = {
                if (twoFactorEnabled) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("Verification Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (twoFactorEnabled) {
                            viewModel.disable2FA(currentPassword)
                        } else {
                            viewModel.enable2FA(otpCode)
                        }
                    },
                    enabled = twoFactorState !is TwoFactorState.Loading
                ) {
                    if (twoFactorState is TwoFactorState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(if (twoFactorEnabled) "Disable" else "Enable")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { show2FADialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Deactivate Account Dialog
    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Deactivate Account") },
            text = {
                Column {
                    Text("This action is irreversible. Your account will be deactivated and you will lose access to all data.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = remember { mutableStateOf(false) }.value,
                            onCheckedChange = { /* handle confirm */ }
                        )
                        Text("I understand the consequences")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deactivateAccount(currentPassword, true) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = deactivationState !is AccountDeactivationState.Loading
                ) {
                    if (deactivationState is AccountDeactivationState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Deactivate")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = tint)
        },
        headlineContent = {
            Text(title, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(subtitle, color = Color.Gray)
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    )
}

@Composable
fun SessionItem(
    session: LoginSession,
    onTerminate: () -> Unit,
    isCurrent: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (session.deviceType.lowercase()) {
                    "mobile" -> Icons.Outlined.PhoneAndroid
                    "desktop" -> Icons.Outlined.Computer
                    else -> Icons.Outlined.Devices
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Last used: ${session.formattedLastUsed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = session.ipAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (!isCurrent) {
                IconButton(onClick = onTerminate) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Terminate", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// Factory
class SettingsViewModelFactory(
    private val usersRepository: UsersRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(usersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
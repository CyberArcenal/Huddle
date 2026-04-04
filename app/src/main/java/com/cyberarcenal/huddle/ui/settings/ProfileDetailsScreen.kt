package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.*
import java.time.format.DateTimeFormatter

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
    ),
    globalSnackbarHostState: SnackbarHostState
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
            // Username (critical)
            item {
                EditableInfoRow(
                    label = "Username",
                    value = profile?.username ?: "",
                    onEdit = {
                        navController.navigate("edit_username")
                    }
                )
            }
            // Email (critical)
            item {
                EditableInfoRow(
                    label = "Email",
                    value = profile?.email ?: "",
                    onEdit = {
                        navController.navigate("edit_email")
                    }
                )
            }
            // First Name
            item {
                EditableInfoRow(
                    label = "First Name",
                    value = profile?.firstName?.takeIf { it.isNotBlank() } ?: "Not set",
                    onEdit = {
                        navController.navigate("edit_field/first_name/${profile?.firstName ?: ""}")
                    }
                )
            }
            // Last Name
            item {
                EditableInfoRow(
                    label = "Last Name",
                    value = profile?.lastName?.takeIf { it.isNotBlank() } ?: "Not set",
                    onEdit = {
                        navController.navigate("edit_field/last_name/${profile?.lastName ?: ""}")
                    }
                )
            }
            // Phone
            item {
                EditableInfoRow(
                    label = "Phone",
                    value = profile?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set",
                    onEdit = {
                        navController.navigate("edit_field/phone/${profile?.phoneNumber ?: ""}")
                    }
                )
            }
            // Bio
            item {
                EditableInfoRow(
                    label = "Bio",
                    value = profile?.bio?.takeIf { it.isNotBlank() } ?: "No bio",
                    onEdit = {
                        navController.navigate("edit_field/bio/${profile?.bio ?: ""}")
                    }
                )
            }
            // Location
            item {
                EditableInfoRow(
                    label = "Location",
                    value = profile?.location?.takeIf { it.isNotBlank() } ?: "Not set",
                    onEdit = {
                        navController.navigate("edit_field/location/${profile?.location ?: ""}")
                    }
                )
            }
            // Date of Birth
            item {
                val dob = profile?.dateOfBirth?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
                EditableInfoRow(
                    label = "Date of Birth",
                    value = dob.takeIf { it.isNotBlank() } ?: "Not set",
                    onEdit = {
                        navController.navigate("edit_field/date_of_birth/$dob")
                    }
                )
            }
        }
    }
}

@Composable
fun EditableInfoRow(label: String, value: String, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit $label")
            }
        }
    }
}
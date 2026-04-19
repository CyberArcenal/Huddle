package com.cyberarcenal.huddle.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            // 1. Name Section - Transparent & Minimal
            item {
                MinimalEditableRow(
                    label = "Full Name",
                    value = "${profile?.firstName ?: ""} ${profile?.middleName ?: ""} ${profile?.lastName ?: ""}".replace("\\s+".toRegex(), " ").trim(),
                    onClick = {
                        val first = profile?.firstName ?: ""
                        val middle = profile?.middleName ?: ""
                        val last = profile?.lastName ?: ""
                        navController.navigate("edit_name/$first/$middle/$last")
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Username
            item {
                MinimalEditableRow(
                    label = "Username",
                    value = profile?.username ?: "",
                    onClick = { navController.navigate("edit_username") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Email
            item {
                MinimalEditableRow(
                    label = "Email",
                    value = profile?.email ?: "",
                    onClick = { navController.navigate("edit_email") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Phone
            item {
                MinimalEditableRow(
                    label = "Phone",
                    value = profile?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set",
                    onClick = { navController.navigate("edit_field/phone/${profile?.phoneNumber ?: ""}") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Bio
            item {
                MinimalEditableRow(
                    label = "Bio",
                    value = profile?.bio?.takeIf { it.isNotBlank() } ?: "No bio",
                    onClick = { navController.navigate("edit_field/bio/${profile?.bio ?: ""}") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Location
            item {
                MinimalEditableRow(
                    label = "Location",
                    value = profile?.location?.takeIf { it.isNotBlank() } ?: "Not set",
                    onClick = { navController.navigate("edit_field/location/${profile?.location ?: ""}") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Date of Birth
            item {
                val dob = profile?.dateOfBirth?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
                MinimalEditableRow(
                    label = "Date of Birth",
                    value = dob.takeIf { it.isNotBlank() } ?: "Not set",
                    onClick = { navController.navigate("edit_field/date_of_birth/$dob") }
                )
            }
        }
    }
}

@Composable
fun MinimalEditableRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
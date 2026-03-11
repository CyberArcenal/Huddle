package com.cyberarcenal.huddle.ui.groups.creategroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.PrivacyC6eEnum
import com.cyberarcenal.huddle.data.repositories.groups.GroupsRepository
import com.cyberarcenal.huddle.ui.theme.Gradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavController,
    viewModel: CreateGroupViewModel = viewModel(
        factory = CreateGroupViewModelFactory(GroupsRepository())
    )
) {
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val privacy by viewModel.privacy.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val descriptionError by viewModel.descriptionError.collectAsState()

    LaunchedEffect(createState) {
        when (createState) {
            is CreateGroupState.Success -> {
                val group = (createState as CreateGroupState.Success).group
                navController.navigate("groupdetail/${group.id}") {
                    popUpTo("creategroup") { inclusive = true }
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Group") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::updateName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Group Name") },
                isError = nameError != null,
                supportingText = {
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                isError = descriptionError != null,
                supportingText = {
                    if (descriptionError != null) {
                        Text(descriptionError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Privacy selection
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // FIX: Ipinapasa ang weight(1f) dito
                PrivacyChip(
                    selected = privacy == PrivacyC6eEnum.PUBLIC,
                    icon = Icons.Outlined.Public,
                    label = "Public",
                    onClick = { viewModel.updatePrivacy(PrivacyC6eEnum.PUBLIC) },
                    modifier = Modifier.weight(1f)
                )
                PrivacyChip(
                    selected = privacy == PrivacyC6eEnum.PRIVATE,
                    icon = Icons.Outlined.Lock,
                    label = "Private",
                    onClick = { viewModel.updatePrivacy(PrivacyC6eEnum.PRIVATE) },
                    modifier = Modifier.weight(1f)
                )
                PrivacyChip(
                    selected = privacy ==PrivacyC6eEnum.SECRET,
                    icon = Icons.Outlined.VisibilityOff,
                    label = "Secret",
                    onClick = { viewModel.updatePrivacy(PrivacyC6eEnum.SECRET) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            // Create button
            Button(
                onClick = viewModel::createGroup,
                enabled = createState !is CreateGroupState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (createState !is CreateGroupState.Loading) Gradients.buttonGradient else Gradients.disabledGradient,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (createState is CreateGroupState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            "Create Group",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            if (createState is CreateGroupState.Error) {
                Text(
                    text = (createState as CreateGroupState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // FIX: Idinagdag ang modifier parameter
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier // FIX: Dito gagamitin ang weight mula sa Row
    )
}

class CreateGroupViewModelFactory(
    private val groupsRepository: GroupsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateGroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateGroupViewModel(groupsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
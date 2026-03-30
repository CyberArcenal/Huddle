package com.cyberarcenal.huddle.ui.groups.creategroup

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.GroupTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyC6eEnum
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.data.repositories.UserSearchRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreationScreen(
    navController: NavController,
    viewModel: GroupCreationViewModel = viewModel(
        factory = GroupCreationViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            groupRepository = GroupRepository(),
            userSearchRepository = UserSearchRepository()
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val groupName by viewModel.groupName.collectAsState()
    val shortDescription by viewModel.shortDescription.collectAsState()
    val longDescription by viewModel.longDescription.collectAsState()
    val groupType by viewModel.groupType.collectAsState()
    val privacy by viewModel.privacy.collectAsState()
    val profilePictureUri by viewModel.profilePictureUri.collectAsState()
    val coverPhotoUri by viewModel.coverPhotoUri.collectAsState()
    val invitedUsers by viewModel.invitedUsers.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    // Image pickers
    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.setPendingImage(ImageType.PROFILE, it) }
        }
    )
    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.setPendingImage(ImageType.COVER, it) }
        }
    )

    // Crop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val croppedUri = result.data?.let { UCrop.getOutput(it) }
                val (type, _) = viewModel.getPending()
                if (type == ImageType.PROFILE) {
                    viewModel.setProfilePictureUri(croppedUri)
                } else if (type == ImageType.COVER) {
                    viewModel.setCoverPhotoUri(croppedUri)
                }
                viewModel.clearPending()
            }
            UCrop.RESULT_ERROR -> {
                val error = result.data?.let { UCrop.getError(it) }
                viewModel.clearPending()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Crop failed: ${error?.message ?: "Unknown error"}")
                }
            }
            else -> viewModel.clearPending()
        }
    }

    // When a pending image is set, start cropping
    LaunchedEffect(viewModel.getPending()) {
        val (type, uri) = viewModel.getPending()
        if (uri != null && type != null) {
            val intent = when (type) {
                ImageType.PROFILE -> createProfileCropIntent(context, uri)
                ImageType.COVER -> createCoverCropIntent(context, uri)
            }
            cropLauncher.launch(intent)
        }
    }

    // Handle action state
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar((actionState as ActionState.Success).message)
                navController.popBackStack()
            }
            is ActionState.Error -> snackbarHostState.showSnackbar((actionState as ActionState.Error).message)
            else -> {}
        }
        viewModel.resetActionState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create a New Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Info section
            Text("Basic Info", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = groupName,
                onValueChange = viewModel::updateGroupName,
                label = { Text("Group Name *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = shortDescription,
                onValueChange = viewModel::updateShortDescription,
                label = { Text("Short Description *") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            OutlinedTextField(
                value = longDescription,
                onValueChange = viewModel::updateLongDescription,
                label = { Text("Long Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Group Type dropdown
            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = {}
            ) {
                Text("Group Type: ${groupType.value}", modifier = Modifier.fillMaxWidth())
                // We'll create a proper dropdown; for simplicity, we'll use a button
                DropdownMenu(
                    expanded = false,
                    onDismissRequest = {}
                ) {
                    // Not implemented here; we'll use a separate button later
                }
            }
            // Replace with a proper dropdown
            GroupTypeDropdown(
                selected = groupType,
                onSelect = viewModel::updateGroupType
            )

            // Privacy dropdown
            PrivacyDropdown(
                selected = privacy,
                onSelect = viewModel::updatePrivacy
            )

            // Profile picture
            Text("Profile Picture (optional)", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profilePictureUri != null) {
                    // Show thumbnail
                    AsyncImage(
                        model = profilePictureUri,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                }
                Button(onClick = { profilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (profilePictureUri != null) "Change" else "Upload")
                }
            }

            // Cover photo
            Text("Cover Photo (optional)", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (coverPhotoUri != null) {
                    AsyncImage(
                        model = coverPhotoUri,
                        contentDescription = null,
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                    )
                }
                Button(onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (coverPhotoUri != null) "Change" else "Upload")
                }
            }

            // Member invites section
            Text("Invite Members", style = MaterialTheme.typography.titleLarge)
            SearchAndInviteSection(viewModel)

            // Invited users list
            if (invitedUsers.isNotEmpty()) {
                Text("Invited Users", style = MaterialTheme.typography.titleMedium)
                invitedUsers.forEach { invited ->
                    InvitedUserItem(
                        user = invited,
                        onRemove = { viewModel.removeInvitedUser(it.userId) },
                        onRoleChange = { role -> viewModel.updateInvitedUserRole(invited.userId, role) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.createGroup() },
                modifier = Modifier.fillMaxWidth(),
                enabled = actionState !is ActionState.Loading
            ) {
                if (actionState is ActionState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Group")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTypeDropdown(
    selected: GroupTypeEnum,
    onSelect: (GroupTypeEnum) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Group Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            GroupTypeEnum.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.value) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDropdown(
    selected: PrivacyC6eEnum,
    onSelect: (PrivacyC6eEnum) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Privacy") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PrivacyC6eEnum.values().forEach { privacy ->
                DropdownMenuItem(
                    text = { Text(privacy.value) },
                    onClick = {
                        onSelect(privacy)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SearchAndInviteSection(viewModel: GroupCreationViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { query ->
            viewModel.updateSearchQuery(query)
            viewModel.searchUsers(query)
        },
        label = { Text("Search users to invite") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }
    )

    if (searchResults.isNotEmpty()) {
        Column {
            searchResults.forEach { user ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(user.username ?: "User", modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.addInvitedUser(user) }) {
                        Text("Invite")
                    }
                }
            }
        }
    }
}

@Composable
fun InvitedUserItem(
    user: InvitedUser,
    onRemove: (InvitedUser) -> Unit,
    onRoleChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(user.username, fontWeight = FontWeight.Medium)
            Row {
                // Role dropdown
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = true }) {
                    Text(user.role)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("member", "moderator", "admin").forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                onRoleChange(role)
                                expanded = false
                            }
                        )
                    }
                }
                IconButton(onClick = { onRemove(user) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
        }
    }
}



// In GroupCreationScreen.kt or a separate file
fun createProfileCropIntent(context: Context, sourceUri: Uri): Intent {
    val destinationUri = Uri.fromFile(
        File(
            context.cacheDir,
            "cropped_profile_${System.currentTimeMillis()}.jpg"
        )
    )
    val uCrop = UCrop.of(sourceUri, destinationUri)
    uCrop.withAspectRatio(1f, 1f)
    uCrop.withMaxResultSize(500, 500)
    return uCrop.getIntent(context)
}

fun createCoverCropIntent(context: Context, sourceUri: Uri): Intent {
    val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg"))
    val uCrop = UCrop.of(sourceUri, destinationUri)
    // Cover photo can have a fixed aspect ratio, e.g., 16:9
    uCrop.withAspectRatio(16f, 9f)
    uCrop.withMaxResultSize(1200, 675)
    return uCrop.getIntent(context)
}

private fun uriToFile(uri: Uri, context: Context): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
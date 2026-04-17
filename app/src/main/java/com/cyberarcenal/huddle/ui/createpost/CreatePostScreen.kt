package com.cyberarcenal.huddle.ui.createpost

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Poll
import com.google.gson.Gson
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.MediaDisplay
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.api.models.UserMinimal
import androidx.compose.material.icons.outlined.PersonAdd
import com.cyberarcenal.huddle.data.repositories.FriendshipsRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import com.cyberarcenal.huddle.ui.common.feed.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.post.PollOption
import com.cyberarcenal.huddle.ui.common.post.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    postType: String? = null,
    viewModel: CreatePostViewModel = viewModel(
        factory = CreatePostViewModelFactory(
            feedRepository = UserPostsRepository(),
            friendshipsRepository = FriendshipsRepository(),
            contentResolver = LocalContext.current.contentResolver,
            groupRepository = GroupRepository(),
            context = LocalContext.current
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Read groupId from navigation arguments
    val groupId = navController.currentBackStackEntry?.arguments?.getInt("groupId")
    LaunchedEffect(groupId) {
        viewModel.setGroupId(groupId)
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { uris -> viewModel.onMediaSelected(uris) }
    )

    LaunchedEffect(postType) {
        when (postType?.lowercase()) {
            "image", "video" -> {
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            }
            "poll" -> {
                viewModel.togglePoll(true)
            }
        }
    }

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.step == CreatePostViewModel.Step.CREATE) "Create Post" else "Preview",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == CreatePostViewModel.Step.PREVIEW) {
                            viewModel.setStep(CreatePostViewModel.Step.CREATE)
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            if (uiState.step == CreatePostViewModel.Step.PREVIEW) Icons.Default.ArrowBack else Icons.Default.Close,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val buttonText = if (uiState.step == CreatePostViewModel.Step.CREATE) "Next" else "Post"
                        val enabled = (uiState.content.isNotBlank() || uiState.selectedMedia.isNotEmpty()) &&
                                (uiState.groupId == null || uiState.isGroupMember) // Disable if not group member

                        TextButton(
                            onClick = {
                                if (uiState.step == CreatePostViewModel.Step.CREATE) {
                                    viewModel.setStep(CreatePostViewModel.Step.PREVIEW)
                                } else {
                                    viewModel.createPost(context = context)
                                }
                            },
                            enabled = enabled
                        ) {
                            Text(
                                buttonText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Uploading post... please wait", 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.step) {
                    CreatePostViewModel.Step.CREATE -> {
                        CreatePostContent(
                            uiState = uiState,
                            onContentChange = viewModel::onContentChange,
                            onPrivacyChange = viewModel::onPrivacyChange,
                            onRemoveMedia = viewModel::removeMedia,
                            onAddMedia = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onTogglePoll = viewModel::togglePoll,
                            onPollOptionChange = viewModel::onPollOptionChange,
                            onAddPollOption = viewModel::addPollOption,
                            onRemovePollOption = viewModel::removePollOption,
                            onFeelingChange = viewModel::onFeelingChange,
                            onLocationChange = viewModel::onLocationChange,
                            onTagUser = viewModel::onTagUser,
                            onRemoveTaggedUser = viewModel::onRemoveTaggedUser,
                            onLoadFriends = viewModel::loadFriends,
                            scrollState = scrollState
                        )
                    }
                    CreatePostViewModel.Step.PREVIEW -> {
                        PostPreview(uiState = uiState)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePostContent(
    uiState: CreatePostUiState,
    onContentChange: (String) -> Unit,
    onPrivacyChange: (PrivacyB23Enum) -> Unit,
    onRemoveMedia: (Uri) -> Unit,
    onAddMedia: () -> Unit,
    onTogglePoll: (Boolean) -> Unit,
    onPollOptionChange: (Int, String) -> Unit,
    onAddPollOption: () -> Unit,
    onRemovePollOption: (Int) -> Unit,
    onFeelingChange: (String?) -> Unit,
    onLocationChange: (String?) -> Unit,
    onTagUser: (UserMinimal) -> Unit,
    onRemoveTaggedUser: (UserMinimal) -> Unit,
    onLoadFriends: () -> Unit,
    scrollState: ScrollState
) {
    var showFeelingDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    data class FeelingOption(val key: String, val label: String, val emoji: String)
    val feelings = listOf(
        FeelingOption("happy", "Happy", "😊"),
        FeelingOption("sad", "Sad", "😢"),
        FeelingOption("love", "Love", "🥰"),
        FeelingOption("crazy", "Crazy", "🤪"),
        FeelingOption("cool", "Cool", "😎"),
        FeelingOption("excited", "Excited", "🤩"),
        FeelingOption("angry", "Angry", "😠"),
        FeelingOption("bored", "Bored", "😑"),
        FeelingOption("tired", "Tired", "😴"),
        FeelingOption("confused", "Confused", "😕"),
        FeelingOption("anxious", "Anxious", "😰"),
        FeelingOption("proud", "Proud", "😤"),
        FeelingOption("lonely", "Lonely", "😔"),
        FeelingOption("blessed", "Blessed", "😇"),
        FeelingOption("relaxed", "Relaxed", "😌"),
        FeelingOption("thinking", "Thinking", "🤔"),
        FeelingOption("grateful", "Grateful", "🙏")
    )

    if (showTagDialog) {
        LaunchedEffect(Unit) {
            onLoadFriends()
        }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Tag Friends") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    uiState.availableFriends.forEach { friend ->
                        val isTagged = uiState.taggedUsers.any { it.id == friend.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isTagged) onRemoveTaggedUser(friend) else onTagUser(friend)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = friend.profilePictureUrl,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(friend.fullName ?: friend.username ?: "Unknown", modifier = Modifier.weight(1f))
                            Checkbox(checked = isTagged, onCheckedChange = {
                                if (isTagged) onRemoveTaggedUser(friend) else onTagUser(friend)
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showFeelingDialog) {
        AlertDialog(
            onDismissRequest = { showFeelingDialog = false },
            title = { Text("How are you feeling?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(feelings.size) { index ->
                            val feeling = feelings[index]
                            Surface(
                                onClick = {
                                    onFeelingChange(feeling.key)
                                    showFeelingDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${feeling.label} ${feeling.emoji}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            onFeelingChange(null)
                            showFeelingDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Feeling", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showLocationDialog) {
        var locationText by remember { mutableStateOf(uiState.location ?: "") }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Where are you?") },
            text = {
                TextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    placeholder = { Text("Enter location") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onLocationChange(locationText.takeIf { it.isNotBlank() })
                    showLocationDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onLocationChange(null)
                    showLocationDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header: User/Group info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                when {
                    uiState.groupId != null && uiState.groupProfilePicture != null -> {
                        AsyncImage(
                            model = uiState.groupProfilePicture,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    uiState.currentUserAvatarUrl != null -> {
                        AsyncImage(
                            model = uiState.currentUserAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        // Placeholder – maybe show an icon or initials
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.currentUserName?.take(1)?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.groupId != null) uiState.groupName ?: "Group" else uiState.currentUserName ?: "Your Name",
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.feeling != null) {
                        val feelingDisplay = feelings.find { it.key == uiState.feeling }?.let { "${it.label} ${it.emoji}" } ?: uiState.feeling
                        Text(
                            text = " is feeling $feelingDisplay",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    if (uiState.location != null) {
                        Text(
                            text = " at ${uiState.location}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                if (uiState.taggedUsers.isNotEmpty()) {
                    Text(
                        text = "with ${uiState.taggedUsers.joinToString(", ") { it.fullName ?: it.username ?: "" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.groupId == null) {
                    PrivacyDropdown(
                        selectedPrivacy = uiState.privacy,
                        onPrivacyChange = onPrivacyChange
                    )
                } else {
                    Text(
                        text = "Posting in group",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        TextField(
            value = uiState.content,
            onValueChange = onContentChange,
            placeholder = { 
                Text(
                    "What's on your mind?", 
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
        )

        if (uiState.isPoll) {
            PollCreator(
                options = uiState.pollOptions,
                onOptionChange = onPollOptionChange,
                onAddOption = onAddPollOption,
                onRemoveOption = onRemovePollOption,
                onClose = { onTogglePoll(false) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.selectedMedia.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(uiState.selectedMedia) { uri ->
                    MediaPreviewItem(uri = uri, onRemove = { onRemoveMedia(uri) })
                }
                item {
                    AddMoreMediaButton(onClick = onAddMedia)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onAddMedia() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Photos/Video", color = MaterialTheme.colorScheme.onSurface)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onTogglePoll(true) }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Poll, contentDescription = null, tint = Color(0xFF2196F3))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Create Poll", color = MaterialTheme.colorScheme.onSurface)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showFeelingDialog = true }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Mood, contentDescription = null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(12.dp))
            val selectedFeelingLabel = feelings.find { it.key == uiState.feeling }?.let { "${it.label} ${it.emoji}" }
            Text(selectedFeelingLabel?.let { "Feeling: $it" } ?: "Feeling", color = MaterialTheme.colorScheme.onSurface)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showLocationDialog = true }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFFE91E63))
            Spacer(modifier = Modifier.width(12.dp))
            Text(uiState.location?.let { "At $it" } ?: "Check in", color = MaterialTheme.colorScheme.onSurface)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showTagDialog = true }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                if (uiState.taggedUsers.isEmpty()) "Tag Friends" 
                else "Tagged ${uiState.taggedUsers.size} friends", 
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PollCreator(
    options: List<String>,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Poll Options", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Poll")
                }
            }

            options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = option,
                        onValueChange = { onOptionChange(index, it) },
                        placeholder = { Text("Option ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    if (options.size > 2) {
                        IconButton(onClick = { onRemoveOption(index) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (options.size < 5) {
                TextButton(
                    onClick = onAddOption,
                    colors = ButtonDefaults.textButtonColors(MaterialTheme.colorScheme
                        .onSurfaceVariant),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Option")
                }
            }
        }
    }
}

@Composable
fun PrivacyDropdown(
    selectedPrivacy: PrivacyB23Enum,
    onPrivacyChange: (PrivacyB23Enum) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val icon = when (selectedPrivacy) {
        PrivacyB23Enum.PUBLIC -> Icons.Default.Public
        PrivacyB23Enum.FOLLOWERS -> Icons.Default.People
        PrivacyB23Enum.SECRET -> Icons.Default.Lock
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    selectedPrivacy.name.lowercase().replaceFirstChar { it.uppercase() }, 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Public") },
                leadingIcon = { Icon(Icons.Default.Public, null) },
                onClick = { onPrivacyChange(PrivacyB23Enum.PUBLIC); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Followers") },
                leadingIcon = { Icon(Icons.Default.People, null) },
                onClick = { onPrivacyChange(PrivacyB23Enum.FOLLOWERS); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Secret") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                onClick = { onPrivacyChange(PrivacyB23Enum.SECRET); expanded = false }
            )
        }
    }
}

@Composable
fun MediaPreviewItem(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true

    Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (isVideo) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(32.dp)
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun AddMoreMediaButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PostPreview(uiState: CreatePostUiState) {
    val previewPost = remember(uiState.content, uiState.selectedMedia, uiState.isPoll, uiState.pollOptions, uiState.feeling, uiState.location, uiState.taggedUsers) {
        val postType = when {
            uiState.isPoll -> PostTypeEnum.POLL
            uiState.selectedMedia.isEmpty() -> PostTypeEnum.TEXT
            else -> PostTypeEnum.IMAGE
        }
        
        val previewData = mutableMapOf<String, Any?>()
        if (uiState.isPoll) {
            previewData["poll"] = uiState.pollOptions.filter { it.isNotBlank() }.mapIndexed { index, s ->
                PollOption(id = index, text = s, votes = 0, isVoted = false)
            }
        }
        if (uiState.feeling != null) previewData["feeling"] = uiState.feeling
        if (uiState.location != null) previewData["location"] = uiState.location
        if (uiState.taggedUsers.isNotEmpty()) {
            previewData["tag_users"] = uiState.taggedUsers.map { user ->
                mapOf(
                    "id" to user.id,
                    "username" to user.username,
                    "full_name" to user.fullName
                )
            }
        }

        val previewJson = if (previewData.isNotEmpty()) Gson().toJson(previewData) else null

        PostFeed(
            id = 0,
            user = UserMinimal(id = 0, username = "you", fullName = "You"),
            content = uiState.content,
            postType = postType,
            preview = previewJson,
            media = uiState.selectedMedia.mapIndexed { index, uri ->
                MediaDisplay(
                    id = index + 1, fileUrl = uri.toString(),
                    order = 0,
                    createdAt = null,
                    metadata = {},
                )
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FeedItemFrame(
            user = previewPost.user,
            createdAt = null,
            statistics = null,
            caption = previewPost.content,
            onReactionClick = {},
            onCommentClick = {},
            onShareClick = {},
            onMoreClick = {},
            onProfileClick = {},
            showBottomDivider = false,
            content = {
                PostItem(
                    post = previewPost,
                    onImageClick = {}
                )
            },
            onGroupClick = {},
        )
    }
}

// Updated ViewModel Factory
class CreatePostViewModelFactory(
    private val feedRepository: UserPostsRepository,
    private val friendshipsRepository: FriendshipsRepository,
    private val contentResolver: ContentResolver,
    private val groupRepository: GroupRepository,
    private val context: Context // Added context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePostViewModel(feedRepository, friendshipsRepository, contentResolver, groupRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
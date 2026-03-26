package com.cyberarcenal.huddle.ui.createpost

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
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
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostMediaDisplay
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import com.cyberarcenal.huddle.ui.common.feed.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.post.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    viewModel: CreatePostViewModel = viewModel(
        factory = CreatePostViewModelFactory(
            feedRepository = UserPostsRepository(),
            contentResolver = LocalContext.current.contentResolver
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { uris -> viewModel.onMediaSelected(uris) }
    )

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = Color.White,
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
                        val enabled = uiState.content.isNotBlank() || uiState.selectedMedia.isNotEmpty()
                        
                        TextButton(
                            onClick = {
                                if (uiState.step == CreatePostViewModel.Step.CREATE) {
                                    viewModel.setStep(CreatePostViewModel.Step.PREVIEW)
                                } else {
                                    viewModel.createPost(
                                        context = context
                                    )
                                }
                            },
                            enabled = enabled
                        ) {
                            Text(
                                buttonText,
                                fontWeight = FontWeight.Bold,
                                color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                    Text("Uploading post... please wait", style = MaterialTheme.typography.labelMedium)
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
                                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) 
                            },
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
    scrollState: ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color.LightGray,
                modifier = Modifier.size(40.dp)
            ) {
                // Placeholder for user avatar
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Your Name", fontWeight = FontWeight.Bold)
                PrivacyDropdown(
                    selectedPrivacy = uiState.privacy,
                    onPrivacyChange = onPrivacyChange
                )
            }
        }

        TextField(
            value = uiState.content,
            onValueChange = onContentChange,
            placeholder = { Text("What's on your mind?", fontSize = 18.sp) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
        )

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

        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onAddMedia() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Photos/Video")
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
            color = Color.LightGray.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(selectedPrivacy.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
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
        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray.copy(alpha = 0.3f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
private fun PostPreview(uiState: CreatePostUiState) {
    val previewPost = remember(uiState.content, uiState.selectedMedia) {
        PostFeed(
            id = 0,
            user = UserMinimal(id = 0, username = "you", fullName = "You"),
            content = uiState.content,
            media = uiState.selectedMedia.mapIndexed { index, uri ->
                PostMediaDisplay(id = index + 1, fileUrl = uri.toString())
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
            }
        )
    }
}

class CreatePostViewModelFactory(
    private val feedRepository: UserPostsRepository,
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePostViewModel(feedRepository, contentResolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

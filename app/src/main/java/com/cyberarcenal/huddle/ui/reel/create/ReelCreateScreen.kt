package com.cyberarcenal.huddle.ui.reel.create

import android.content.ContentResolver
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
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
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.ui.common.story.StoryVideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelCreateScreen(
    navController: NavController,
    viewModel: ReelCreateViewModel = viewModel(
        factory = ReelCreateViewModelFactory(
            contentResolver = LocalContext.current.contentResolver,
            context = LocalContext.current
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.setSelectedVideoUri(uri) }
    )

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.setThumbnailUri(uri) }
    )

    LaunchedEffect(uiState.reelCreated) {
        if (uiState.reelCreated) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Reel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.createReel() }, // No context needed, ViewModel has it
                        enabled = uiState.selectedVideoUri != null && !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Video Preview / Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .clickable {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.selectedVideoUri != null) {
                    StoryVideoPlayer(
                        videoUrl = uiState.selectedVideoUri.toString(),
                        isPlaying = true,
                        onVideoFinished = {},
                        onProgressUpdate = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Overlay to change video
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select Video", color = Color.White)
                    }
                }
            }

            // Caption Field
            OutlinedTextField(
                value = uiState.caption,
                onValueChange = { viewModel.setCaption(it) },
                label = { Text("Write a caption...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            // Thumbnail Selector
            Text("Thumbnail", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable {
                            thumbnailPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.thumbnailUri != null) {
                        AsyncImage(
                            model = uiState.thumbnailUri,
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                    }
                }
                Text(
                    "Choose a cover for your reel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Settings
            HorizontalDivider()
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            ListItem(
                headlineContent = { Text("Privacy") },
                supportingContent = { Text(uiState.privacy.value.replaceFirstChar { it.uppercase() }) },
                leadingContent = { Icon(Icons.Default.Public, contentDescription = null) },
                trailingContent = {
                    var showPrivacyMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showPrivacyMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(expanded = showPrivacyMenu, onDismissRequest = { showPrivacyMenu = false }) {
                        PrivacyB23Enum.entries.forEach { privacy ->
                            DropdownMenuItem(
                                text = { Text(privacy.value.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.setPrivacy(privacy)
                                    showPrivacyMenu = false
                                }
                            )
                        }
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Allow Remix") },
                leadingContent = { Icon(Icons.Default.Repeat, contentDescription = null) },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )

            ListItem(
                headlineContent = { Text("Allow Comments") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null) },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Updated factory to accept both ContentResolver and Context
class ReelCreateViewModelFactory(
    private val contentResolver: ContentResolver,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReelCreateViewModel(contentResolver, context) as T
    }
}
package com.cyberarcenal.huddle.ui.createStory

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.data.repositories.StoriesRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    navController: NavController,
    viewModel: CreateStoryViewModel = viewModel(
        factory = CreateStoryViewModelFactory(
            storiesRepository = StoriesRepository(context = LocalContext.current),
            contentResolver = LocalContext.current.contentResolver
        )
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.setSelectedMediaUri(uri) }
    )

    LaunchedEffect(Unit) {
        if (uiState.selectedMediaUri == null && uiState.caption.isBlank()) {
            mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }
    }

    LaunchedEffect(uiState.storyCreated) {
        if (uiState.storyCreated) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Media Preview / Text Editor Area
        when {
            uiState.selectedMediaUri != null && uiState.storyType == StoryTypeEnum.VIDEO -> {
                VideoPreview(uri = uiState.selectedMediaUri!!)
            }
            uiState.selectedMediaUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uiState.selectedMediaUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // Text Story Mode: Full-screen background editor
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(uiState.backgroundColor)
                        .clickable { viewModel.cycleBackgroundColor() },
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = uiState.caption,
                        onValueChange = viewModel::setCaption,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        placeholder = { 
                            Text(
                                "Type something...", 
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Header
        TopBar(
            onClose = { navController.popBackStack() },
            onPost = { viewModel.createStory(context) },
            isLoading = uiState.isLoading,
            canPost = uiState.selectedMediaUri != null || uiState.caption.isNotBlank()
        )

        // Editing Tools (Floating Side Bar)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.selectedMediaUri == null) {
                ToolButton(
                    icon = Icons.Default.Palette, 
                    label = "Color", 
                    onClick = viewModel::cycleBackgroundColor
                )
            }
            ToolButton(icon = Icons.Default.TextFields, label = "Text")
            ToolButton(icon = Icons.AutoMirrored.Filled.StickyNote2, label = "Stickers")
            ToolButton(icon = Icons.Default.Brush, label = "Draw")
            ToolButton(icon = Icons.Default.MusicNote, label = "Music")
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Media Caption Input (only if media is selected)
            if (uiState.selectedMediaUri != null) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    OutlinedTextField(
                        value = uiState.caption,
                        onValueChange = viewModel::setCaption,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a caption...", color = Color.White.copy(alpha = 0.6f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )
                }
            }

            // Privacy & Gallery Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrivacyChip(
                    selectedPrivacy = uiState.privacy,
                    onPrivacyChange = viewModel::setPrivacy
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { /* Settings */ },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }
        }

        // Error message
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = viewModel::clearError) { Text("OK") }
                }
            ) { Text(error) }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPreview(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TopBar(
    onClose: () -> Unit,
    onPost: () -> Unit,
    isLoading: Boolean,
    canPost: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Text(
            "Create Story",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Button(
            onClick = onPost,
            enabled = canPost && !isLoading,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Share", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String,
    onClick: () -> Unit = {}
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

@Composable
private fun PrivacyChip(
    selectedPrivacy: PrivacyB23Enum,
    onPrivacyChange: (PrivacyB23Enum) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        SuggestionChip(
            onClick = { expanded = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when(selectedPrivacy) {
                            PrivacyB23Enum.PUBLIC -> Icons.Default.Public
                            PrivacyB23Enum.FOLLOWERS -> Icons.Default.People
                            PrivacyB23Enum.SECRET -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedPrivacy.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White.copy(alpha = 0.2f)),
            border = null,
            shape = RoundedCornerShape(16.dp)
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PrivacyB23Enum.entries.forEach { privacy ->
                DropdownMenuItem(
                    text = {
                        Text(privacy.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                    },
                    onClick = {
                        onPrivacyChange(privacy)
                        expanded = false
                    }
                )
            }
        }
    }
}

class CreateStoryViewModelFactory(
    private val storiesRepository: StoriesRepository,
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateStoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateStoryViewModel(storiesRepository, contentResolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

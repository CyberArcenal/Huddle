package com.cyberarcenal.huddle.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.network.TokenManager
import com.cyberarcenal.huddle.ui.comments.CommentBottomSheet
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.ui.common.feed.mapCurrentReaction
import com.cyberarcenal.huddle.ui.common.feed.getReactionIcon
import io.livekit.android.LiveKit
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.TrackPublication
import io.livekit.android.room.track.LocalTrackPublication
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.renderer.TextureViewRenderer
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.border
import com.cyberarcenal.huddle.ui.common.utils.ConfirmDialog
import com.cyberarcenal.huddle.ui.common.utils.rememberConfirmState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
    liveId: Int,
    viewModel: LiveViewModel,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val currentStream by viewModel.currentLiveStream.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val liveKitToken by viewModel.liveKitToken.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentComments by viewModel.commentComments.collectAsState()
    val commentSheetState by viewModel.commentSheetState.collectAsState()
    val replies by viewModel.replies.collectAsState()
    val expandedReplies by viewModel.expandedReplies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    val joinRequestStatus by viewModel.joinRequestStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var room by remember { mutableStateOf<Room?>(null) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var isFrontCamera by remember { mutableStateOf(true) }
    val confirmState = rememberConfirmState()

    val isHost = currentStream?.host?.id == currentUser?.id
    var showRequestsSheet by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isInputFocused by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    val reactionItems = remember {
        listOf(
            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", painterResource = com.cyberarcenal.huddle.R.drawable.like),
            Reaction(key = ReactionTypeEnum.DISLIKE, label = "Dislike", painterResource = com.cyberarcenal.huddle.R.drawable.dislike),
            Reaction(key = ReactionTypeEnum.LOVE, label = "Love", painterResource = com.cyberarcenal.huddle.R.drawable.love),
            Reaction(key = ReactionTypeEnum.CARE, label = "Care", painterResource = com.cyberarcenal.huddle.R.drawable.care),
            Reaction(key = ReactionTypeEnum.HAHA, label = "Haha", painterResource = com.cyberarcenal.huddle.R.drawable.haha),
            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", painterResource = com.cyberarcenal.huddle.R.drawable.wow),
            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", painterResource = com.cyberarcenal.huddle.R.drawable.sad),
            Reaction(key = ReactionTypeEnum.ANGRY, label = "Angry", painterResource = com.cyberarcenal.huddle.R.drawable.angry),
        )
    }

    val currentReaction by viewModel.currentReaction.collectAsState()

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == currentReaction }
    )

    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionTypeEnum
        if (selectedKey != null && selectedKey != currentReaction) {
            viewModel.sendReaction(
                ReactionCreateRequest(
                    contentType = "livestream",
                    objectId = liveId,
                    reactionType = selectedKey
                )
            )
        }
    }

    // Permission Handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (allGranted && isHost) {
            coroutineScope.launch {
                room?.localParticipant?.setCameraEnabled(true)
                room?.localParticipant?.setMicrophoneEnabled(true)
            }
        }
    }
    LaunchedEffect(joinRequestStatus) {
        when (joinRequestStatus) {
            JoinRequestStatus.PENDING -> {
                snackbarHostState.showSnackbar("Join request sent. Waiting for host approval.")
            }

            JoinRequestStatus.APPROVED -> {
                snackbarHostState.showSnackbar("Request approved! You can now join the stream.")
            }

            JoinRequestStatus.REJECTED -> {
                snackbarHostState.showSnackbar("Your join request was rejected.")
            }

            else -> {}
        }
    }
    LaunchedEffect(LocalContext.current) {
        viewModel.initUser(context)
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            )
        )
    }

    LaunchedEffect(liveId) {
        viewModel.joinStream(liveId)
    }

    // LiveKit Connection Logic
    LaunchedEffect(liveKitToken) {
        val tokenData = liveKitToken ?: return@LaunchedEffect

        try {
            val newRoom = LiveKit.create(context)
            room = newRoom

            coroutineScope.launch {
                newRoom.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            if (track is VideoTrack) {
                                // For viewers, we want to ensure we are seeing the host
                                remoteVideoTrack = track
                            }
                        }

                        is RoomEvent.TrackPublished -> {
                            if (event.participant == newRoom.localParticipant) {
                                val track = event.publication.track
                                if (track is VideoTrack) {
                                    localVideoTrack = track
                                }
                            }
                        }

                        is RoomEvent.Disconnected -> {
                            remoteVideoTrack = null
                            localVideoTrack = null
                        }

                        else -> {}
                    }
                }
            }

            newRoom.connect(tokenData.url, tokenData.token)

            // If Host or Approved Guest, enable camera/mic immediately upon connection (ONLY if permissions are granted)
            if ((isHost || joinRequestStatus == JoinRequestStatus.APPROVED) && permissionsGranted) {
                coroutineScope.launch {
                    newRoom.localParticipant.setCameraEnabled(true)
                    newRoom.localParticipant.setMicrophoneEnabled(true)
                    
                    // Instant preview for host
                    val publication = newRoom.localParticipant.videoTrackPublications.firstOrNull()?.first
                    localVideoTrack = publication?.track as? VideoTrack
                }
            }
        } catch (e: Exception) {
            Log.e("LiveStreamScreen", "Failed to connect to LiveKit", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            room?.disconnect()
            viewModel.leaveStream()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video Layer
        if (isHost || joinRequestStatus == JoinRequestStatus.APPROVED) {
            // Broadcaster View: Show local camera (mirrored for front camera)
            if (localVideoTrack != null && room != null) {
                VideoRendererView(videoTrack = localVideoTrack!!, room = room!!, mirrored = isFrontCamera)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        } else {
            // Viewer View: Show remote track (host)
            if (remoteVideoTrack != null && room != null) {
                VideoRendererView(videoTrack = remoteVideoTrack!!, room = room!!)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Connecting to stream...", color = Color.White)
                    }
                }
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top Bar: TikTok Style Pill and Close Button
            Row(
                modifier = Modifier
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Host Info Pill
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = currentStream?.host?.profilePictureUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                currentStream?.host?.fullName?: currentStream?.host?.username ?:
                                "Host",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.People,
                                    null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "${currentStream?.viewerCount ?: 0}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHost) {
                        IconButton(
                            onClick = {
                                showRequestsSheet = true
                                viewModel.fetchPendingRequests()
                            },
                            modifier = Modifier
                                .size(32.dp)
//                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (joinRequests.isNotEmpty()) {
                                        Badge(
                                            modifier = Modifier.size(12.dp),
                                            containerColor = Color.Red
                                        ) {
                                            Text("${joinRequests.size}", fontSize = 8.sp)
                                        }
                                    }
                                }) {
                                Icon(
                                    Icons.Default.People,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    if (!isHost) {
                        when (joinRequestStatus) {
                            JoinRequestStatus.NONE -> {
                                Surface(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clickable { viewModel.requestToJoin("I want to join the stream!") },
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(15.dp)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                        Text("Join", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            JoinRequestStatus.PENDING -> {
                                Surface(
                                    modifier = Modifier.height(30.dp),
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(15.dp)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                        Text("Pending", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }
                            JoinRequestStatus.APPROVED -> {
                                Surface(
                                    color = Color.Green.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(15.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color.Green, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Joined", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                            JoinRequestStatus.REJECTED -> {
                                Surface(
                                    color = Color.Red.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(15.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                        Text("Rejected", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = {
                            if (isHost) {
                                confirmState.show(
                                    title = "End Live",
                                    message = "Are you sure do you want to end this live?",
                                    confirmText = "Yes",
                                    isDangerous = false,
                                    onConfirm = {
                                        viewModel.endLive()
                                        confirmState.hide()
                                    })
                            } else viewModel.leaveStream()
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .size(32.dp)
//                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Participants Row
            AnimatedVisibility(visible = participants.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participants) { participant ->
                        if (participant.id != currentUser?.id){
                            ParticipantAvatar(participant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Section: Comments and Input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Comments List (TikTok style: bottom-left, semi-transparent)
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    reverseLayout = true
                ) {
                    items(comments.asReversed()) { comment ->
                        CommentItem(comment)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Input Bar and Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isInputFocused = it.isFocused },
                        placeholder = {
                            Text(
                                "Add comment...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commentText.isNotBlank()) {
                                viewModel.sendComment(commentText)
                                commentText = ""
                            }
                            focusManager.clearFocus()
                        })
                    )

                    if (!isInputFocused) {
                        if (isHost) {
                            var isMicOn by remember { mutableStateOf(true) }
                            var isVideoOn by remember { mutableStateOf(true) }

                            LiveActionButton(
                                icon = Icons.Default.FlipCameraAndroid,
                                onClick = {
                                    isFrontCamera = !isFrontCamera
                                    coroutineScope.launch {
                                        (localVideoTrack as? io.livekit.android.room.track.LocalVideoTrack)?.switchCamera()
                                    }
                                }
                            )

                            LiveActionButton(
                                icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                                onClick = {
                                    isMicOn = !isMicOn
                                    coroutineScope.launch {
                                        room?.localParticipant?.setMicrophoneEnabled(isMicOn)
                                    }
                                }
                            )

                            LiveActionButton(
                                icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                onClick = {
                                    isVideoOn = !isVideoOn
                                    coroutineScope.launch {
                                        room?.localParticipant?.setCameraEnabled(isVideoOn)
                                    }
                                }
                            )
                        }

                        LiveActionButton(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            onClick = {
                                viewModel.openCommentSheet("livestream", liveId, null)
                            }
                        )

                        val (reactionIcon, reactionTint) = getReactionIcon(currentReaction)
                        val interactionTint = if (currentReaction != null) reactionTint else MaterialTheme.colorScheme.onSurfaceVariant

                        LiveActionButton(
                            icon = reactionIcon,
                            tint = interactionTint,
                            onClick = {
                                val newReaction = if (currentReaction != null) null else ReactionTypeEnum.LIKE
                                viewModel.sendReaction(
                                    ReactionCreateRequest(
                                        contentType = "livestream",
                                        objectId = liveId,
                                        reactionType = newReaction ?: ReactionTypeEnum.LIKE
                                    )
                                )
                            },
                            modifier = Modifier.reactionPickerAnchor(pickerState)
                        )
                    }
                }
            }
        }

        // Comment Bottom Sheet
        commentSheetState?.let { state ->
            CommentBottomSheet(
                comments = commentComments,
                replies = replies,
                expandedReplies = expandedReplies,
                currentUserId = currentUser?.id,
                isLoadingMore = isLoadingMore,
                onLoadMore = { viewModel.loadMoreComments() },
                onToggleReplyExpanded = { viewModel.toggleReplyExpansion(it) },
                onLoadReplies = { viewModel.loadReplies(it) },
                onReactToComment = { commentId, reactionType ->
                    viewModel.sendReaction(
                        ReactionCreateRequest(
                            contentType = "comment",
                            objectId = commentId,
                            reactionType = reactionType ?: ReactionTypeEnum.LIKE
                        )
                    )
                },
                onReplyToComment = { parentId, content -> viewModel.addReply(parentId, content) },
                onReportComment = { /* Handle report */ },
                onDismiss = { viewModel.dismissCommentSheet() },
                onSendComment = { viewModel.addComment(it) },
                onDeleteComment = { viewModel.deleteComment(it) },
                actionState = actionState,
                errorMessage = commentsError,
                statistics = state.statistics
            )
        }

        // Host Requests Bottom Sheet
        if (showRequestsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRequestsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                JoinRequestsList(
                    requests = joinRequests, onRespond = { requestId, approve ->
                        viewModel.respondToJoinRequest(requestId, approve)
                    })
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
        dismissText = confirmState.dismissText,
        isConfirmDangerous = confirmState.isDangerous
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveActionButton(
    icon: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .scale(scale.value)
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
            .combinedClickable(
                onClick = {
                    isPressed = true
                    onClick()
                    coroutineScope.launch {
                        delay(100)
                        isPressed = false
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (icon) {
            is ImageVector -> Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            is Int -> Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ParticipantAvatar(participant: LiveParticipant) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = participant.user?.profilePictureUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
        if (participant.role == LiveParticipantRoleEnum.HOST || participant.role == LiveParticipantRoleEnum.CO_HOST) {
            Icon(
                Icons.Default.Mic,
                null,
                tint = Color.Green,
                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
                    .background(Color.Black, CircleShape).padding(2.dp)
            )
        }
    }
}

@Composable
fun JoinRequestsList(
    requests: List<LiveJoinRequest>, onRespond: (Int, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Join Requests",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        if (requests.isEmpty()) {
            Text(
                "No pending requests",
                modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(requests) { request ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = request.user?.profilePictureUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    request.user?.username ?: "Unknown",
                                    fontWeight = FontWeight.Bold
                                )
                                if (!request.message.isNullOrBlank()) {
                                    Text(
                                        request.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { request.id?.let { onRespond(it, false) } }) {
                                Icon(Icons.Default.Close, null, tint = Color.Red)
                            }
                            IconButton(onClick = { request.id?.let { onRespond(it, true) } }) {
                                Icon(Icons.Default.Check, null, tint = Color.Green)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun VideoRendererView(videoTrack: VideoTrack, room: Room, mirrored: Boolean = false) {
    VideoTrackView(
        videoTrack = videoTrack,
        modifier = Modifier.fillMaxSize(),
        passedRoom = room,
        mirror = mirrored
    )
}

@Composable
fun CommentItem(comment: LiveComment) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${comment.username}: ",
                color = Color(0xFF81D4FA), // Light Blue like TikTok names
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = comment.content,
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

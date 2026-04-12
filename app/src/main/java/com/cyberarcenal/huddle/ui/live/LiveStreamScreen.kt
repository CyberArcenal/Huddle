package com.cyberarcenal.huddle.ui.live

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.LiveJoinRequest
import com.cyberarcenal.huddle.api.models.LiveParticipant
import com.cyberarcenal.huddle.api.models.LiveParticipantRoleEnum
import com.cyberarcenal.huddle.network.TokenManager
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.renderer.TextureViewRenderer
import android.view.ViewGroup
import android.widget.FrameLayout
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
    val participants by viewModel.participants.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    val joinRequestStatus by viewModel.joinRequestStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var room by remember { mutableStateOf<Room?>(null) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }

    val isHost = currentStream?.host?.id == currentUser?.id
    var showRequestsSheet by remember { mutableStateOf(false) }

    // Permission Handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
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

            // Use collect (or onEach) instead of collectLatest
            coroutineScope.launch {
                newRoom.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            if (track is VideoTrack) {
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

            if (isHost) {
                newRoom.localParticipant.setCameraEnabled(true)
                newRoom.localParticipant.setMicrophoneEnabled(true)
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
        val activeVideoTrack = if (isHost) localVideoTrack else remoteVideoTrack

        if (activeVideoTrack != null) {
            VideoRendererView(videoTrack = activeVideoTrack)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("Connecting to stream...", color = Color.White)
                }
            }
        }

        // Overlay UI
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = currentStream?.host?.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            currentStream?.host?.username ?: "Host",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = Color.Red, shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "LIVE",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHost) {
                        // Host Dashboard Button
                        Box {
                            IconButton(
                                onClick = {
                                    showRequestsSheet = true
                                    viewModel.fetchPendingRequests()
                                }, modifier = Modifier.background(
                                    Color.Black.copy(alpha = 0.4f), CircleShape
                                )
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (joinRequests.isNotEmpty()) {
                                            Badge { Text("${joinRequests.size}") }
                                        }
                                    }) {
                                    Icon(Icons.Default.People, null, tint = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    };

                    if (!isHost) {
                        when (joinRequestStatus) {
                            JoinRequestStatus.NONE -> {
                                Button(
                                    onClick = { viewModel.requestToJoin("I want to join the stream!") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(
                                            alpha = 0.2f
                                        )
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp, vertical = 4.dp
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Join Request", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            JoinRequestStatus.PENDING -> {
                                OutlinedButton(
                                    onClick = { },
                                    enabled = false,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Request Pending", fontSize = 12.sp)
                                }
                            }

                            JoinRequestStatus.APPROVED -> {
                                // Hindi na kailangan ng button, kasi approved na at magkakaroon ng video
                                // Pero puwede ring magpakita ng "Joined" text
                                Surface(
                                    color = Color.Green.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp, vertical = 4.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color.Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Approved", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            JoinRequestStatus.REJECTED -> {
                                Button(
                                    onClick = { /* Puwede mag-retry? Pero depende sa backend */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Request Rejected", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${currentStream?.viewerCount ?: 0}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (isHost) viewModel.endLive() else viewModel.leaveStream()
                            navController.popBackStack()
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }

            // Participants Row
            AnimatedVisibility(visible = participants.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participants) { participant ->
                        ParticipantAvatar(participant)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Section: Comments and Input
            Box(
                modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    ).padding(16.dp)
            ) {
                Column {
                    // Comments List
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        items(comments.asReversed()) { comment ->
                            CommentItem(comment)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Comment Input & Controls
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var commentText by remember { mutableStateOf("") }
                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Say something...", color = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        if (isHost) {
                            var isMicOn by remember { mutableStateOf(true) }
                            var isVideoOn by remember { mutableStateOf(true) }

                            IconButton(
                                onClick = {
                                    isMicOn = !isMicOn
                                    coroutineScope.launch {
                                        room?.localParticipant?.setMicrophoneEnabled(
                                            isMicOn
                                        )
                                    }
                                }, modifier = Modifier.background(
                                    if (isMicOn) Color.White.copy(alpha = 0.2f) else Color.Red,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                                    null,
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    isVideoOn = !isVideoOn
                                    coroutineScope.launch {
                                        room?.localParticipant?.setCameraEnabled(
                                            isVideoOn
                                        )
                                    }
                                }, modifier = Modifier.background(
                                    if (isVideoOn) Color.White.copy(alpha = 0.2f) else Color.Red,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    null,
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.sendComment(commentText)
                                    commentText = ""
                                }
                            }, modifier = Modifier.background(
                                MaterialTheme.colorScheme.primary, CircleShape
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                        }
                    }
                }
            }
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
fun VideoRendererView(videoTrack: VideoTrack) {
    AndroidView(factory = { context ->
        TextureViewRenderer(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            videoTrack.addRenderer(this)
        }
    }, modifier = Modifier.fillMaxSize(), update = { /* Handle updates if needed */ })
}

@Composable
fun CommentItem(comment: LiveComment) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "${comment.username}: ",
            color = Color.Yellow,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = comment.content, color = Color.White, fontSize = 14.sp
        )
    }
}

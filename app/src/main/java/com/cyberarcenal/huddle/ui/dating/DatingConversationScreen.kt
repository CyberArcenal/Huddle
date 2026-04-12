package com.cyberarcenal.huddle.ui.dating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.DatingMessageDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatingConversationScreen(
    userId: Int,
    viewModel: DatingViewModel,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val messages by viewModel.messages.collectAsState()
    val conversationState by viewModel.conversationState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var partnerName by remember { mutableStateOf("Chat") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showScrollButton by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Connect to chat and load conversation
    LaunchedEffect(userId) {
        viewModel.loadConversationWith(userId)
        viewModel.connectToChat(userId)
        // Auto-focus text field after a short delay
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Extract partner name from messages
    LaunchedEffect(messages) {
        messages.firstOrNull()?.let { firstMsg ->
            partnerName = when {
                firstMsg.sender?.id == currentUserId -> firstMsg.receiver?.username ?: "User"
                else -> firstMsg.sender?.username ?: "User"
            }
        }
    }

    // Scroll to bottom (newest message) - since reverseLayout = true, index 0 is newest
    fun scrollToBottom() {
        coroutineScope.launch {
            listState.animateScrollToItem(0)
        }
    }

    // Auto-scroll when new message arrives, unless user scrolled up
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !showScrollButton) {
            scrollToBottom()
        }
    }

    // Detect scroll position to show/hide FAB
    LaunchedEffect(listState.isScrollInProgress) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                // Show FAB if scrolled away from top (newest messages)
                showScrollButton = index > 2
            }
    }

    // Clean up WebSocket on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectFromChat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        partnerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.imePadding(),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendSocketMessage(messageText)
                                    messageText = ""
                                    scrollToBottom()
                                    focusRequester.requestFocus()
                                }
                            }
                        ),
                        maxLines = 4,
                        singleLine = false
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendSocketMessage(messageText)
                                messageText = ""
                                scrollToBottom()
                                focusRequester.requestFocus()
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showScrollButton) {
                FloatingActionButton(
                    onClick = { scrollToBottom() },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Scroll to bottom")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (conversationState) {
                is DatingUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DatingUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            (conversationState as DatingUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadConversationWith(userId) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        reverseLayout = true
                    ) {
                        items(messages, key = { it.id ?: it.hashCode() }) { message ->
                            val isMe = message.sender?.id == currentUserId
                            ChatMessageItem(
                                message = message,
                                isMe = isMe,
                                currentUserId = currentUserId
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: DatingMessageDetail,
    isMe: Boolean,
    currentUserId: Int?
) {
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            border = if (!isMe) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            tonalElevation = 1.dp
        ) {
            Text(
                text = message.content ?: "",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = message.createdAt?.let { DateUtils.toRelativeTime(it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            // Read receipt: only show for messages sent by me and marked as read
            if (isMe && message.isRead == true) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.DoneAll,
                    contentDescription = "Read",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
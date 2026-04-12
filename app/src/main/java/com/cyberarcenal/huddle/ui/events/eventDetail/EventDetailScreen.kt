package com.cyberarcenal.huddle.ui.events.eventDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.EventAttendance
import com.cyberarcenal.huddle.api.models.EventAttendanceWithUser
import com.cyberarcenal.huddle.api.models.EventDetail
import com.cyberarcenal.huddle.api.models.GroupMinimal
import com.cyberarcenal.huddle.api.models.StatusDecEnum
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.common.user.DynamicIslandFollowButton
import com.cyberarcenal.huddle.ui.common.user.UserAvatar
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    navController: NavController,
    eventRepository: EventRepository,
    attendanceRepository: EventAttendanceRepository,
    followRepository: FollowRepository,
    groupRepository: GroupRepository,
    globalSnackbarHostState: SnackbarHostState
) {
    val viewModel: EventDetailViewModel = viewModel(
        factory = EventDetailViewModelFactory(
            eventId,
            eventRepository,
            attendanceRepository,
            followRepository,
            groupRepository
        )
    )

    val event by viewModel.event.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val attendees by viewModel.attendees.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isFollowingOrganizer by viewModel.isFollowingOrganizer.collectAsState()
    val isGroupMember by viewModel.isGroupMember.collectAsState()
    val isJoiningGroup by viewModel.isJoiningGroup.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                globalSnackbarHostState.showSnackbar((actionState as ActionState.Success).message)
                viewModel.clearActionState()
            }
            is ActionState.Error -> {
                globalSnackbarHostState.showSnackbar((actionState as ActionState.Error).message)
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        event?.title ?: "Event Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* Report */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Sticky RSVP Bar
            if (event != null) {
                StickyRsvpBar(
                    event = event!!,
                    attendance = attendance,
                    onRsvp = { status -> viewModel.rsvp(status) },
                    onCancelRsvp = { viewModel.cancelRsvp() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (event == null) {
                Text("Event not found", modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Title, Type Badge, Date/Time, Location
                    EventHeader(event = event!!)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cover / Banner with Group Info overlay
                    EventBanner(
                        event = event!!,
                        isGroupMember = isGroupMember,
                        isJoiningGroup = isJoiningGroup,
                        onJoinGroup = { viewModel.joinGroup() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Organizer Card
                    event!!.organizer?.let { organizer ->
                        OrganizerCard(
                            organizer = organizer,
                            isFollowing = isFollowingOrganizer,
                            onFollowClick = { viewModel.toggleFollowOrganizer() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description Section
                    event!!.description?.let { desc ->
                        DescriptionSection(description = desc)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Attendees Section
                    AttendeesSection(
                        event = event!!,
                        attendees = attendees,
                        onSeeAllClick = { navController.navigate("event_attendees/${event!!.id}") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Group Info Section (if group event)
                    event!!.group?.let { group ->
                        GroupInfoSection(group = group)
                    }

                    Spacer(modifier = Modifier.height(80.dp)) // extra space for bottom bar
                }
            }
        }
    }
}

@Composable
private fun EventHeader(event: EventDetail) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = event.title ?: "Untitled",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Type Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = when (event.eventType?.value) {
                    "public" -> "Public Event"
                    "private" -> "Private Event"
                    "group" -> "Group Event"
                    else -> "Event"
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date & Time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = formatDateTimeRange(event.startTime, event.endTime),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (event.startTime != null && event.endTime != null) {
                    Text(
                        text = formatDuration(event.startTime, event.endTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Location
        if (!event.location.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(event.location, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EventBanner(
    event: EventDetail,
    isGroupMember: Boolean,
    isJoiningGroup: Boolean,
    onJoinGroup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cover image: use group.profilePicture or fallback
            val imageUrl = event.group?.profilePicture?.toString()
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            // Gradient overlay for text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 300f
                        )
                    )
            )

            // Group info overlay (bottom left)
            event.group?.let { group ->
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = group.name ?: "Group",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                            , contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${group.memberCount ?: 0} members",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Join Group button (top right)
            if (event.group != null && !isGroupMember) {
                Button(
                    onClick = onJoinGroup,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    enabled = !isJoiningGroup,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isJoiningGroup) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Join Group")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizerCard(organizer: UserMinimal, isFollowing: Boolean, onFollowClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                username = organizer.username,
                profilePictureUrl = organizer.profilePictureUrl,
                size = 48.dp,
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organizer.fullName ?: organizer.username ?: "Organizer",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@${organizer.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Optional: show personality type or hobbies
                organizer.personalityType?.value?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            DynamicIslandFollowButton(
                isFollowing = isFollowing,
                isLoading = false,
                onClick = onFollowClick,
                height = 32.dp,
                modifier = Modifier.width(90.dp)
            )
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    var expanded by remember { mutableStateOf(false) }
    val maxLines = if (expanded) Int.MAX_VALUE else 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Description",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (description.length > 150) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(if (expanded) "Show less" else "Show more")
                }
            }
        }
    }
}

@Composable
private fun AttendeesSection(
    event: EventDetail,
    attendees: List<EventAttendanceWithUser>,
    onSeeAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendees",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${event.attendeesCount ?: 0} / ${event.maxAttendees?.toString() ?: "∞"} going",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.isFull == true) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                        Text("FULL", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal avatar list
            if (attendees.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attendees.take(8)) { attendance ->
                        Avatar(
                            url = attendance.user?.profilePictureUrl,
                            username = attendance.user?.username,
                            size = 40.dp
                        )
                    }
                    if (attendees.size > 8) {
                        item {
                            Button(
                                onClick = onSeeAllClick,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+${attendees.size - 8}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Text("No attendees yet", style = MaterialTheme.typography.bodySmall)
            }

            if ((event.attendeesCount ?: 0) > 0) {
                TextButton(
                    onClick = onSeeAllClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("See all attendees")
                }
            }
        }
    }
}

@Composable
private fun GroupInfoSection(group: GroupMinimal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = group.profilePicture?.toString(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name ?: "Group", fontWeight = FontWeight.Bold)
                Text(
                    text = "${group.memberCount ?: 0} members • ${group.groupTypeDisplay ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun StickyRsvpBar(
    event: EventDetail,
    attendance: EventAttendance?,
    onRsvp: (StatusDecEnum) -> Unit,
    onCancelRsvp: () -> Unit
) {
    val currentStatus = attendance?.status
    val isFull = event.isFull == true

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (currentStatus != null) "You are ${currentStatus.value}" else "Not RSVPed",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (currentStatus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${event.attendeesCount ?: 0} going",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (currentStatus != null) {
                OutlinedButton(onClick = onCancelRsvp) {
                    Text("Cancel RSVP")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isFull) {
                        Button(onClick = { onRsvp(StatusDecEnum.GOING) }) {
                            Text("Going")
                        }
                        OutlinedButton(onClick = { onRsvp(StatusDecEnum.MAYBE) }) {
                            Text("Maybe")
                        }
                    } else {
                        Button(
                            enabled = false,
                            onClick = {},
                        ) {
                            Text("Event Full")
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
private fun formatDateTimeRange(start: OffsetDateTime?, end: OffsetDateTime?): String {
    if (start == null) return "Date TBD"
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
    val startStr = start.format(formatter)
    if (end == null) return startStr
    val endStr = end.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return "$startStr - $endStr"
}

private fun formatDuration(start: OffsetDateTime, end: OffsetDateTime): String {
    val durationMinutes = Duration.between(start, end).toMinutes()
    return when {
        durationMinutes < 60 -> "${durationMinutes} minutes"
        durationMinutes < 1440 -> "${durationMinutes / 60} hours ${durationMinutes % 60} minutes"
        else -> "${durationMinutes / 1440} days"
    }
}
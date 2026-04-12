package com.cyberarcenal.huddle.ui.events.eventList

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository
import com.cyberarcenal.huddle.data.repositories.EventRepository
import com.cyberarcenal.huddle.ui.common.event.EventCard
import com.cyberarcenal.huddle.ui.common.managers.ActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMainScreen(
    navController: NavController,
    eventRepository: EventRepository,
    attendanceRepository: EventAttendanceRepository,
    globalSnackbarHostState: SnackbarHostState
) {
    val viewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventRepository, attendanceRepository)
    )
    val selectedTab by viewModel.selectedTab.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
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
                title = { Text("Events") },
                actions = {
                    IconButton(onClick = { navController.navigate("create_event") }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Event")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 0.dp
            ) {
                listOf("Discover", "My Events", "Organized").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> DiscoverEventsTab(viewModel, navController)
                1 -> MyEventsTab(viewModel, navController)
                2 -> OrganizedEventsTab(viewModel, navController)
            }
        }
    }
}

@Composable
private fun DiscoverEventsTab(viewModel: EventViewModel, navController: NavController) {
    val events = viewModel.getDiscoverEventsPager().collectAsLazyPagingItems()
    EventListContent(events, navController, viewModel)
}

@Composable
private fun MyEventsTab(viewModel: EventViewModel, navController: NavController) {
    val events = viewModel.getMyEventsPager().collectAsLazyPagingItems()
    EventListContent(events, navController, viewModel)
}

@Composable
private fun OrganizedEventsTab(viewModel: EventViewModel, navController: NavController) {
    val events = viewModel.getOrganizedEventsPager().collectAsLazyPagingItems()
    EventListContent(events, navController, viewModel)
}

@Composable
private fun EventListContent(
    events: LazyPagingItems<EventList>,
    navController: NavController,
    viewModel: EventViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            count = events.itemCount,
            key = events.itemKey { event -> event.id ?: event.hashCode() }
        ) { index ->
            val event = events[index]
            event?.let {
                EventCard(
                    event = it,
                    onClick = { navController.navigate("event_detail/${it.id}") },
                    onRsvp = { status -> viewModel.rsvp(it.id ?: return@EventCard, status) }
                )
            }
        }

        if (events.loadState.append is androidx.paging.LoadState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
package com.cyberarcenal.huddle.ui.events.createEvent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EventCreationScreen(
    navController: NavController,
    viewModel: EventCreateViewModel = viewModel(
        factory = EventCreateViewModelFactory(LocalContext.current)
    ),
    globalSnackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    // Handle successful creation: navigate back
    LaunchedEffect(uiState.eventCreated) {
        if (uiState.eventCreated) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    // Media picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        uris?.let { viewModel.addMedia(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    Button(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 3) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            // Submit
                            viewModel.createEvent()
                        }
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (pagerState.currentPage < 3) Text("Next")
                    else {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Create")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (pagerState.currentPage + 1) / 4f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BasicInfoStep(
                        uiState = uiState,
                        onTitleChange = viewModel::setTitle,
                        onEventTypeChange = viewModel::setEventType,
                        onAddCoverImage = { mediaPickerLauncher.launch("image/*") },
                        onRemoveMedia = viewModel::removeMedia
                    )
                    1 -> ScheduleLocationStep(
                        uiState = uiState,
                        onStartTimeChange = viewModel::setStartTime,
                        onEndTimeChange = viewModel::setEndTime,
                        onLocationChange = viewModel::setLocation
                    )
                    2 -> AttendeesGroupStep(
                        uiState = uiState,
                        onMaxAttendeesChange = viewModel::setMaxAttendees,
                        onGroupIdChange = viewModel::setGroup
                    )
                    3 -> DescriptionExtrasStep(
                        uiState = uiState,
                        onDescriptionChange = viewModel::setDescription
                    )
                }
            }
        }
    }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            globalSnackbarHostState.showSnackbar(errorMsg)
            viewModel.setError(null)
        }
    }
}

package com.cyberarcenal.huddle.ui.dating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.data.repositories.DatingMessagesRepository
import com.cyberarcenal.huddle.data.repositories.DatingPreferencesRepository
import com.cyberarcenal.huddle.data.repositories.MatchesRepository
import com.cyberarcenal.huddle.data.repositories.UserMatchingRepository
import com.cyberarcenal.huddle.network.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatingScreen(
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val viewModel: DatingViewModel = viewModel(
        factory = DatingViewModelFactory(
            DatingPreferencesRepository(),
            DatingMessagesRepository(),
            UserMatchingRepository(),
            MatchesRepository()
        )
    )
    // Current user ID
    var currentUserId by remember { mutableStateOf<Int?>(null) }
    var showPreferences by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentUserId = TokenManager.getUser(context)?.id
        viewModel.setCurrentUserId(currentUserId)
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discover", "Inbox", "Sent")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Dating",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreferences = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { paddingValues ->
        if (showPreferences) {
            Dialog(
                onDismissRequest = { showPreferences = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        TopAppBar(
                            title = { Text("Dating Preferences") },
                            navigationIcon = {
                                IconButton(onClick = { showPreferences = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                                }
                            }
                        )
                        DatingPreferencesScreen(viewModel, globalSnackbarHostState)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                tonalElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                ) 
                            }
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> DatingDiscoverScreen(viewModel, navController, globalSnackbarHostState)
                    1 -> DatingInboxScreen(viewModel, navController, globalSnackbarHostState)
                    2 -> DatingSentScreen(viewModel, navController, globalSnackbarHostState)
                }
            }
        }
    }
}
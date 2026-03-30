package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cyberarcenal.huddle.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    navController: NavController,
    onNavigateToNotifications: () -> Unit,
    onNavigateToConversations: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToCreateStory: () -> Unit,
    onNavigateToReel: () -> Unit,
    // Dagdag na callbacks
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    lineHeight = 25.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Create Post") },
                        onClick = {
                            showMenu = false
                            onNavigateToCreatePost()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Create Story") },
                        onClick = {
                            showMenu = false
                            onNavigateToCreateStory()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HistoryEdu,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Create Reel") },
                        onClick = {
                            showMenu = false
                            onNavigateToReel()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    // --- BAGONG ITEMS ---
                    HorizontalDivider() // Optional: divider para sa visual separation

                    DropdownMenuItem(
                        text = { Text("Create Event") },
                        onClick = {
                            showMenu = false
                            onNavigateToCreateEvent()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Create Group") },
                        onClick = {
                            showMenu = false
                            onNavigateToCreateGroup()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }

            IconButton(onClick = onNavigateToNotifications) {
                Icon(
                    painter = painterResource(R.drawable.notification),
                    contentDescription = stringResource(R.string.notifications),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToConversations) {
                Icon(
                    painter = painterResource(R.drawable.chat),
                    contentDescription = stringResource(R.string.conversations),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}
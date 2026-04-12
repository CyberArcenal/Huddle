package com.cyberarcenal.huddle.ui.dating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.UserMatchScore
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DatingDiscoverScreen(
    viewModel: DatingViewModel,
    navController: NavController,
    globalSnackbarHostState: SnackbarHostState
) {
    val discoverState by viewModel.discoverState.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    var exitDirection by remember { mutableStateOf(0f) } // positive = right (like), negative = left (skip)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (discoverState) {
            is DatingUiState.Loading -> {
                CircularProgressIndicator()
            }
            is DatingUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${(discoverState as DatingUiState.Error).message}")
                    Button(onClick = { viewModel.loadDiscoverUsers() }) { Text("Retry") }
                }
            }
            is DatingUiState.Success -> {
                val users = (discoverState as DatingUiState.Success).data as List<UserMatchScore>
                if (users.isEmpty()) {
                    EmptyDiscoverContent(onAdjustPreferences = {
                        viewModel.loadDiscoverUsers()
                    })
                } else {
                    val user = users.first()
                    AnimatedContent(
                        targetState = user,
                        transitionSpec = {
                            val direction = if (exitDirection > 0) 1f else -1f
                            (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { (it * -direction * 0.1f).toInt() }) togetherWith
                                (fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { (it * direction).toInt() }) using SizeTransform(clip = false)
                        },
                        label = "discovery_card"
                    ) { currentUser ->
                        DiscoveryCard(
                            userMatch = currentUser,
                            onLike = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    exitDirection = 1f
                                    viewModel.likeUser(currentUser.user?.id ?: 0) {
                                        isProcessing = false
                                    }
                                }
                            },
                            onDislike = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    exitDirection = -1f
                                    viewModel.skipUser()
                                    isProcessing = false
                                }
                            },
                            onClick = {
                                currentUser.user?.id?.let { id ->
                                    navController.navigate("dating_profile/$id")
                                }
                            },
                            isProcessing = isProcessing
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDiscoverContent(onAdjustPreferences: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No more people to discover",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your preferences to see more matches",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAdjustPreferences) {
            Text("Adjust Preferences")
        }
    }
}

@Composable
fun DiscoveryCard(
    userMatch: UserMatchScore,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onClick: () -> Unit,
    isProcessing: Boolean
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 150.dp.toPx() }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = offsetX / 20f
                alpha = 1f - (kotlin.math.abs(offsetX) / (swipeThreshold * 2f)).coerceAtMost(0.5f)
            }
            .then(
                if (!isProcessing) {
                    Modifier.draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            offsetX += delta
                        },
                        onDragStopped = {
                            if (offsetX > swipeThreshold) {
                                onLike()
                            } else if (offsetX < -swipeThreshold) {
                                onDislike()
                            }
                            offsetX = 0f
                        }
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Loading overlay when processing
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            AsyncImage(
                model = userMatch.user?.profilePictureUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(id = R.drawable.profile)
            )

            // Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 400f
                        )
                    )
            )

            // Like/Nope Indicators
            val likeAlpha = (offsetX / swipeThreshold).coerceIn(0f, 1f)
            val nopeAlpha = (-offsetX / swipeThreshold).coerceIn(0f, 1f)

            if (likeAlpha > 0f && !isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = "LIKE",
                        color = Color.Green.copy(alpha = likeAlpha),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .border(4.dp, Color.Green.copy(alpha = likeAlpha), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .graphicsLayer { rotationZ = -15f }
                    )
                }
            }

            if (nopeAlpha > 0f && !isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = "NOPE",
                        color = Color.Red.copy(alpha = nopeAlpha),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .border(4.dp, Color.Red.copy(alpha = nopeAlpha), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .graphicsLayer { rotationZ = 15f }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userMatch.user?.username ?: "Unknown",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${userMatch.score}% Match",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (userMatch.reasons?.isNotEmpty() == true) {
                    Text(
                        text = userMatch.reasons.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LargeIconButton(
                        onClick = onDislike,
                        icon = Icons.Default.Close,
                        color = Color.White,
                        backgroundColor = Color.Black.copy(alpha = 0.3f),
                        enabled = !isProcessing
                    )
                    LargeIconButton(
                        onClick = onLike,
                        icon = Icons.Default.Favorite,
                        color = Color.White,
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        enabled = !isProcessing
                    )
                }
            }
        }
    }
}

@Composable
fun LargeIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    color: Color,
    backgroundColor: Color,
    enabled: Boolean = true
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundColor,
            contentColor = color
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
    }
}
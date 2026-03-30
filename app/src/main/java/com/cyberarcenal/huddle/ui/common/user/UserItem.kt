package com.cyberarcenal.huddle.ui.common.user

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberarcenal.huddle.api.models.PersonalityTypeEnum
import com.cyberarcenal.huddle.api.models.UserMinimal

enum class AvatarShape {
    CIRCLE,
    SQUARE
}

@Composable
fun UserItem(
    user: UserMinimal,
    isVertical: Boolean = false,
    onFollowClick: (UserMinimal) -> Unit,
    onItemClick: () -> Unit,
    isFollowing: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    avatarShape: AvatarShape = AvatarShape.CIRCLE
) {
    if (isVertical) {
        VerticalStoryUserItem(
            username = user.username,
            fullName = user.fullName,
            profilePictureUrl = user.profilePictureUrl,
            personalityType = user.personalityType,
            capabilityScore = user.capabilityScore,
            reasons = user.reasons,
            isFollowing = isFollowing,
            isLoading = isLoading,
            onFollowClick = { onFollowClick(user) },
            onItemClick = onItemClick,
            modifier = modifier
        )
    } else {
        HorizontalListUserItem(
            username = user.username,
            displayName = user.fullName,
            profilePictureUrl = user.profilePictureUrl,
            personalityType = user.personalityType,
            capabilityScore = user.capabilityScore,
            reasons = user.reasons,
            isFollowing = isFollowing,
            isLoading = isLoading,
            onFollowClick = { onFollowClick(user) },
            onItemClick = onItemClick,
            avatarShape = avatarShape,
            modifier = modifier
        )
    }
}

@Composable
private fun VerticalStoryUserItem(
    username: String?,
    fullName: String?,
    profilePictureUrl: String?,
    personalityType: PersonalityTypeEnum?,
    capabilityScore: Int?,
    reasons: List<String>?,
    isFollowing: Boolean,
    isLoading: Boolean = false,
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(190.dp)
            .height(320.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                UserAvatar(
                    username = username,
                    profilePictureUrl = profilePictureUrl,
                    size = null,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxSize()
                )
                
                capabilityScore?.let { score ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$score%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = fullName ?: username ?: "Huddle User",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                personalityType?.value?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = reasons?.firstOrNull() ?: "Suggested for you",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            DynamicIslandFollowButton(
                isFollowing = isFollowing,
                isLoading = isLoading,
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
fun DynamicIslandFollowButton(
    isFollowing: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isLoading -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            isFollowing -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primary
        }, label = "btnColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isLoading -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            isFollowing -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onPrimary
        }, label = "btnContentColor"
    )

    Surface(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier.height(height),
        shape = RoundedCornerShape(height / 2),
        color = containerColor,
        contentColor = contentColor,
        enabled = !isLoading
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(height * 0.45f),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = if (height < 40.dp) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun HorizontalListUserItem(
    username: String?,
    displayName: String?,
    profilePictureUrl: String?,
    personalityType: PersonalityTypeEnum?,
    capabilityScore: Int?,
    reasons: List<String>?,
    onFollowClick: () -> Unit,
    onItemClick: () -> Unit,
    avatarShape: AvatarShape,
    isFollowing: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(
                    username = username,
                    profilePictureUrl = profilePictureUrl,
                    size = 50.dp,
                    shape = getAvatarShape(avatarShape, 12.dp)
                )

                capabilityScore?.let { score ->
                    Surface(
                        modifier = Modifier.offset(x = 4.dp, y = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "$score%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName ?: username ?: "User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    personalityType?.value?.let { type ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = type,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                
                Text(
                    text = reasons?.firstOrNull() ?: "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            DynamicIslandFollowButton(
                isFollowing = isFollowing,
                isLoading = isLoading,
                onClick = onFollowClick,
                height = 32.dp,
                modifier = Modifier.widthIn(min = 90.dp)
            )
        }
        
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun UserAvatar(
    username: String?,
    profilePictureUrl: String?,
    size: Dp? = null,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val finalModifier = if (size != null) {
        modifier.size(size).clip(shape)
    } else {
        modifier.clip(shape)
    }
    if (!profilePictureUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(profilePictureUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = finalModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = finalModifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}

@Composable
fun getAvatarShape(shape: AvatarShape, cornerRadius: Dp = 0.dp): Shape {
    return when (shape) {
        AvatarShape.CIRCLE -> CircleShape
        AvatarShape.SQUARE -> RoundedCornerShape(cornerRadius)
    }
}

@Composable
fun SeeMoreUserCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(320.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "See More",
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "See All\nMatches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

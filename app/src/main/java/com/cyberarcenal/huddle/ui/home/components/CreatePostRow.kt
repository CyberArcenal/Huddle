package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun CreatePostRow(
    profilePictureUrl: String?,
    onRowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Profile Picture
            if (!profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "H", // Default Initial
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // "What's on your mind?" Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onRowClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "What's on your mind?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Photo shortcut button
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "Photo",
                tint = Color(0xFF4CAF50),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onRowClick() }
            )
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.3f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

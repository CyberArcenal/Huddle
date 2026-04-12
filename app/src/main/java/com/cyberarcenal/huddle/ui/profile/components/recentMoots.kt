package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect

@Composable
fun RecentMootsRow(
    navController: NavController,
    moots: List<UserMinimal>,
    modifier: Modifier = Modifier
) {
    if (moots.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Moots",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
//                    navController.navigate("moots")
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(moots) { moot ->
                MootItem(moot = moot, onClick = { navController.navigate("profile/${moot.id}") })
            }
        }
    }
}

@Composable
private fun MootItem(
    moot: UserMinimal,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = moot.profilePictureUrl,
            contentDescription = moot.fullName ?: moot.username,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
//            loading = {
//                Box(
//                    modifier = Modifier.fillMaxWidth()
//                        .aspectRatio(1f).shimmerEffect()
//                )
//            },
//            error = {
//                Box(
//                    modifier = Modifier.fillMaxWidth()
//                        .aspectRatio(1f)
//                        .background(Color.LightGray),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.BrokenImage,
//                        contentDescription = "Error loading image",
//                        tint = Color.Gray
//                    )
//                }
//            })
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = moot.fullName?.take(10) ?: moot.username?.take(10) ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            fontSize = 10.sp
        )
    }
}
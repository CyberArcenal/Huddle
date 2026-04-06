package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.ui.common.user.Avatar

data class BottomNavItem(
    val route: String, val unselectedIconRes: Int, val selectedIconRes: Int, val labelRes: Int
)

@Composable
fun ModernBottomNavigation(
    navController: NavController,
    currentUser: UserProfile?,
    onHomeReselect: () -> Unit,
    onMoreClick: () -> Unit,
    onUnavailableClick: () -> Unit
) {
    val items = listOf(
        BottomNavItem("groups_main", R.drawable.group, R.drawable.group, R.string.nav_groups),
        BottomNavItem("reels", R.drawable.play, R.drawable.play, R.string.nav_reels),
        BottomNavItem("feed", R.drawable.home, R.drawable.home, R.string.nav_home),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))



        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // PROFILE BUTTON (Modern Style)
            val isProfileSelected =
                currentRoute == "profile" || currentRoute?.startsWith("profile/") == true
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentRoute != "profile") {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }, 
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .then(
                            if (isProfileSelected) {
                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                            }
                        )
                        .padding(2.dp) // Space between border and avatar
                ) {
                    Avatar(
                        url = currentUser?.profilePicture?.imageUrl ?: currentUser?.profilePictureUrl,
                        username = currentUser?.username,
                        size = 26.dp,
                        modifier = Modifier.clip(CircleShape)
                    )
                }
            }

            // Regular Bottom Nav Items
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentRoute == item.route && item.route == "feed") {
                            onHomeReselect()
                        } else if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) item.selectedIconRes else item.unselectedIconRes),
                        contentDescription = stringResource(item.labelRes),
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            // MORE BUTTON
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onMoreClick()
                }, contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_svgrepo_com),
                    contentDescription = "More",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

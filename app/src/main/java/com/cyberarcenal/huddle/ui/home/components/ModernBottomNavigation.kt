package com.cyberarcenal.huddle.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyberarcenal.huddle.R

data class BottomNavItem(
    val route: String,
    val unselectedIconRes: Int,
    val selectedIconRes: Int,
    @androidx.annotation.StringRes val labelRes: Int
)


@Composable
fun ModernBottomNavigation(
    navController: NavController,
    onHomeReselect: () -> Unit,
    onUnavailableClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("feed", R.drawable.home, R.drawable.home, R.string.nav_home),
        BottomNavItem("search", R.drawable.search, R.drawable.search, R.string.nav_explore),
        BottomNavItem("friends", R.drawable.profile_2user, R.drawable.profile_2user, R.string.nav_friends),
        BottomNavItem("groups_main", R.drawable.group, R.drawable.group, R.string.nav_groups),
        BottomNavItem("reels", R.drawable.play, R.drawable.play, R.string.nav_reels),
        BottomNavItem("profile", R.drawable.profile, R.drawable.profile, R.string.nav_profile)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute == item.route) {
                                if (item.route == "feed") onHomeReselect()
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) item.selectedIconRes else item.unselectedIconRes),
                        contentDescription = stringResource(item.labelRes),
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) Color.Black else Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModernBottomNavigationPreview() {
    val navController = rememberNavController()
    ModernBottomNavigation(
        navController = navController,
        onHomeReselect = {},
        onUnavailableClick = {}
    )
}

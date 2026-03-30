package com.cyberarcenal.huddle.ui.common.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.api.models.UserMutualCount


@Composable
fun SuggestedUserRow(
    title: String = "Suggested Users",
    suggested: List<UserMutualCount>,
    onUserClick: (UserMinimal) -> Unit,
    followStatuses: Map<Int, Boolean>,
    loadingUsers: Map<Int, Boolean>,
    onFollowClick: (UserMinimal) -> Unit,
    onShowMoreClick: (() -> Unit)? = null,
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {

            items(suggested, key = { item -> "suggested_user_${item.user?.id ?: item.hashCode()}" }) { item ->
                item.user?.let { user ->
                    val isFollowing = followStatuses[user.id] ?: user.isFollowing ?: false
                    val isLoading = loadingUsers[user.id] ?: false
                    
                    UserItem(
                        user = user,
                        isVertical = true,
                        onFollowClick = { onFollowClick(user) },
                        onItemClick = { onUserClick(user) },
                        isFollowing = isFollowing,
                        isLoading = isLoading,
                        modifier = Modifier.width(200.dp) // Consistent fixed width
                    )
                }
            }

            // Optional Show More card
            if (onShowMoreClick != null) {
                item(key = "suggested_suggested_show_more") {
                    SeeMoreUserCard(onClick = { onShowMoreClick() })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

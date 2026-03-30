package com.cyberarcenal.huddle.ui.common.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.cyberarcenal.huddle.api.models.UserMatchScore
import com.cyberarcenal.huddle.api.models.UserMinimal

@Composable
fun MatchUserRow(
    title: String,
    match: List<UserMatchScore>,
    onUserClick: (UserMinimal) -> Unit,
    onFollowClick: (UserMinimal) -> Unit,
    onShowMoreClick: () -> Unit,
    followStatuses: Map<Int, Boolean>,
    loadingUsers: Map<Int, Boolean>,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
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
            items(match, key = { item -> "match_user_${item.user?.id ?: item.hashCode()}" }) { matchItem ->
                matchItem.user?.let { user ->
                    val isFollowing = followStatuses[user.id] ?: user.isFollowing ?: false
                    val isLoading = loadingUsers[user.id] ?: false
                    
                    UserItem(
                        user = user,
                        isVertical = true,
                        onFollowClick = onFollowClick,
                        onItemClick = { onUserClick(user) },
                        isFollowing = isFollowing,
                        isLoading = isLoading,
                        modifier = Modifier.width(200.dp) // Fixed width for all items
                    )
                }
            }
            
            item {
                SeeMoreUserCard(onClick = onShowMoreClick)
            }
        }
    }
}

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
    // Filter out items where user is null to prevent crashes
    val validMatches = match.filter { it.user?.id != null }.distinctBy { it.user?.id }

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
            // Use user.id as unique key (guaranteed non-null after filter)
            items(
                items = validMatches,
                key = { matchItem -> "match_user_${matchItem.user!!.id}" }
            ) { matchItem ->
                val user = matchItem.user!!
                val isFollowing = followStatuses[user.id] ?: user.isFollowing ?: false
                val isLoading = loadingUsers[user.id] ?: false

                UserItem(
                    user = user,
                    isVertical = true,
                    onFollowClick = onFollowClick,
                    onItemClick = { onUserClick(user) },
                    isFollowing = isFollowing,
                    isLoading = isLoading,
                    modifier = Modifier.width(200.dp)
                )
            }

            item {
                SeeMoreUserCard(onClick = onShowMoreClick)
            }
        }
    }
}
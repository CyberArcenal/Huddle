package com.cyberarcenal.huddle.ui.common.user
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.feed.SuggestedUserItem


@Composable
fun SuggestedUserRow(
    title: String = "Suggested Users",
    suggested: List<SuggestedUserItem>,
    onUserClick: (UserMinimal) -> Unit,
    onFollowClick: (UserMinimal) -> Unit,
    onShowMoreClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Horizontal row of suggested suggested
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(suggested, key = { "suggested_user_${it.user.id ?: it.hashCode()}" }) { item ->
                UserItem(
                    user = item.user,
                    isVertical = true,
                    onFollowClick = { onFollowClick(item.user) },
                    onItemClick = { onUserClick(item.user) }
                )
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

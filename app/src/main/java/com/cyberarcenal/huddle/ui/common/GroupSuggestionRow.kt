package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.GroupMinimal
import com.cyberarcenal.huddle.ui.feed.RecommendedGroupItem

@Composable
fun GroupSuggestionsRow(
    title: String,
    groups: List<RecommendedGroupItem>,
    onGroupClick: (GroupMinimal) -> Unit,
    onShowMoreClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(groups, key = { "group_${it.group.id ?: it.hashCode()}" }) { recommended ->
                GroupCardVertical(
                    group = recommended.group,
                    onClick = {onGroupClick(recommended.group)}
                )
            }
            item(key = "group_show_more") {
                ShowMoreGroupCard(onClick = onShowMoreClick)
            }
        }
    }
}
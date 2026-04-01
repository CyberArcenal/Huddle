package com.cyberarcenal.huddle.ui.common.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.GroupMinimal
import com.cyberarcenal.huddle.api.models.GroupSuggestionItem

@Composable
fun GroupSuggestionsRow(
    title: String,
    groups: List<GroupSuggestionItem>,
    onGroupClick: (GroupMinimal) -> Unit,
    onJoinClick: (GroupMinimal) -> Unit,
    onShowMoreClick: () -> Unit,
    groupMembershipStatuses: Map<Int, Boolean>,
    joiningGroupIds: Map<Int, Boolean>
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
                    isMember = (groupMembershipStatuses[recommended.group.id]
                        ?: recommended.group.isMember) == true,
                    isJoining = joiningGroupIds[recommended.group.id] == true,
                    onClick = { onGroupClick(recommended.group) },
                    onJoinClick = { onJoinClick(recommended.group) }
                )
            }
            item(key = "group_show_more") {
                ShowMoreGroupCard(onClick = onShowMoreClick)
            }
        }
    }
}
package com.cyberarcenal.huddle.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.ReactionDisplay
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.ui.common.user.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionListBottomSheet(
    reactions: List<ReactionDisplay>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onMentionClick: (String) -> Unit,
    onProfileClick: (Int) -> Unit,
    onTabSelected: (ReactionTypeEnum?) -> Unit,
    selectedTab: ReactionTypeEnum?,
    reactionCounts: Map<ReactionTypeEnum, Int> = emptyMap()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = if (selectedTab == null) 0 else ReactionTypeEnum.entries.indexOf(selectedTab) + 1,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        val currentTab = if (selectedTab == null) 0 else ReactionTypeEnum.entries.indexOf(selectedTab) + 1
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                val totalCount = reactionCounts.values.sum()
                Tab(
                    selected = selectedTab == null,
                    onClick = { onTabSelected(null) },
                    text = {
                        Text("All ${if (totalCount > 0) totalCount else ""}")
                    }
                )
                ReactionTypeEnum.entries.forEach { type ->
                    val count = reactionCounts[type] ?: 0
                    Tab(
                        selected = selectedTab == type,
                        onClick = { onTabSelected(type) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(getReactionEmoji(type))
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(count.toString())
                                }
                            }
                        }
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.weight(1f)) {
                if (reactions.isEmpty() && !isLoading) {
                    EmptyReactions()
                } else {
                    ReactionList(
                        reactions = reactions,
                        onMentionClick = onMentionClick,
                        onProfileClick = onProfileClick
                    )
                }

                if (isLoading && reactions.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun ReactionList(
    reactions: List<ReactionDisplay>,
    onMentionClick: (String) -> Unit,
    onProfileClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(reactions, key = { it.id ?: it.hashCode() }) { reaction ->
            ReactionUserItem(
                reaction = reaction,
                onMentionClick = onMentionClick,
                onProfileClick = onProfileClick
            )
        }
    }
}

@Composable
private fun ReactionUserItem(
    reaction: ReactionDisplay,
    onMentionClick: (String) -> Unit,
    onProfileClick: (Int) -> Unit
) {
    val user = reaction.user ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { user.id?.let { onProfileClick(it) } },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Avatar(url = user.profilePictureUrl, username = user.fullName ?: user.username)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text(
                    text = getReactionEmoji(reaction.reactionType),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName ?: user.username ?: "Unknown User",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            user.username?.let {
                Text(
                    text = "@$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = { onMentionClick(user.username ?: "") },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Mention",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyReactions() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No reactions yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getReactionEmoji(type: ReactionTypeEnum): String {
    return when (type) {
        ReactionTypeEnum.LIKE -> "👍"
        ReactionTypeEnum.LOVE -> "❤️"
        ReactionTypeEnum.CARE -> "🥰"
        ReactionTypeEnum.HAHA -> "😂"
        ReactionTypeEnum.WOW -> "😮"
        ReactionTypeEnum.SAD -> "😢"
        ReactionTypeEnum.ANGRY -> "😡"
        else -> "👍"
    }
}

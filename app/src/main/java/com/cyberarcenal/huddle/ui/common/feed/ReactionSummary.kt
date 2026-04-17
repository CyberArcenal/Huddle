package com.cyberarcenal.huddle.ui.common.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum

@Composable
fun ReactionSummary(
    statistics: PostStatsSerializers?,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Gray,
    onReactionSummaryClick: () -> Unit = {},
    onCommentSummaryClick: () -> Unit = {}
) {
    if (statistics == null) return
    val reactionCount = statistics.reactionCount

    // Build list of (type, count) where count > 0, sorted by count descending
    // We include all reaction types from your list
    val reactions = listOf(
        ReactionTypeEnum.LIKE to (reactionCount.like ?: 0),
        ReactionTypeEnum.LOVE to (reactionCount.love ?: 0),
        ReactionTypeEnum.CARE to (reactionCount.care ?: 0),
        ReactionTypeEnum.HAHA to (reactionCount.haha ?: 0),
        ReactionTypeEnum.WOW to (reactionCount.wow ?: 0),
        ReactionTypeEnum.SAD to (reactionCount.sad ?: 0),
        ReactionTypeEnum.ANGRY to (reactionCount.angry ?: 0)
    ).filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(3)  // show at most top 3 reaction types

    val totalReactions = (reactionCount.like ?: 0) +
            (reactionCount.love ?: 0) +
            (reactionCount.care ?: 0) +
            (reactionCount.haha ?: 0) +
            (reactionCount.wow ?: 0) +
            (reactionCount.sad ?: 0) +
            (reactionCount.angry ?: 0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT SIDE: REACTIONS ---
        if (totalReactions > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onReactionSummaryClick() }
            ) {
                // Stacked reaction icons (overlapping)
                Box(modifier = Modifier.padding(end = 4.dp)) {
                    reactions.forEachIndexed { index, (type, _) ->
                        val (iconRes, _) = getReactionIcon(type)
                        if (iconRes is Int) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(start = (index * 12).dp) // Adjust padding for overlap
                            )
                        }
                    }
                }
                
                // Extra padding based on how many icons are shown
                val textPadding = if (reactions.size > 1) (reactions.size - 1) * 12 else 0
                
                Text(
                    text = totalReactions.toString(),
                    fontSize = 13.sp,
                    color = textColor,
                    modifier = Modifier.padding(start = textPadding.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- RIGHT SIDE: COMMENTS & SHARES ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp).clickable { onCommentSummaryClick() }) {
            if (statistics.commentCount > 0) {
                Text(
                    text = "${statistics.commentCount} comments",
                    fontSize = 13.sp,
                    color = textColor,
                    modifier = Modifier.clickable { onCommentSummaryClick() }
                )
            }
            
            if (statistics.commentCount > 0 && statistics.shareCount > 0) {
                Text(
                    text = " • ",
                    fontSize = 13.sp,
                    color = textColor
                )
            }

            if (statistics.shareCount > 0) {
                Text(
                    text = "${statistics.shareCount} shares",
                    fontSize = 13.sp,
                    color = textColor
                )
            }
        }
    }
}

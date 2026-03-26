package com.cyberarcenal.huddle.ui.common.feed

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
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType

@Composable
fun ReactionSummary(
    reactionCount: ReactionCount?,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Gray
) {
    if (reactionCount == null) return

    // Build list of (type, count) where count > 0, sorted by count descending
    val reactions = listOf(
        ReactionType.LIKE to reactionCount.like,
        ReactionType.LOVE to reactionCount.love,
        ReactionType.CARE to reactionCount.care,
        ReactionType.HAHA to reactionCount.haha,
        ReactionType.WOW to reactionCount.wow,
        ReactionType.SAD to reactionCount.sad,
        ReactionType.ANGRY to reactionCount.angry
    ).filter { it.second != null && it.second!! > 0 }
        .sortedByDescending { it.second }
        .take(3)  // show at most 3 types

    if (reactions.isEmpty()) return

    val total = reactions.sumOf { it.second ?: 0 }

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show icons with counts
        reactions.forEach { (type, count) ->
            val (iconRes, _) = getReactionIcon(type)  // from InteractionBar.kt
            if (iconRes is Int) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    color = textColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        // Show total reactions count
        Text(
            text = "$total reactions",
            fontSize = 12.sp,
            color = textColor
        )
    }
}
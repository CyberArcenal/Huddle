package com.cyberarcenal.huddle.ui.feed

import com.cyberarcenal.huddle.api.models.GroupMinimal
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.api.models.StoryTypeEnum
import com.cyberarcenal.huddle.api.models.UserMinimal


// Story item
data class StoryItem(
    val user: UserMinimal,
    val stories: List<StoryFeed>,   // Story is another data class you define
    val hasViewedAll: Boolean,
    val type: StoryTypeEnum
)

// Suggested user item
data class SuggestedUserItem(
    val user: UserMinimal,
    val mutualCount: Int,
    val reason: String? = null
)

// Match user item
data class MatchUserItem(
    val user: UserMinimal,
    val score: Int,
    val reasons: List<String>? = null
)

// Recommended group item
data class RecommendedGroupItem(
    val group: GroupMinimal,
    val score: Float,
    val reason: String? = null
)
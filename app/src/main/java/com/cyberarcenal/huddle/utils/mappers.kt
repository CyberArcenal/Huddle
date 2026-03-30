package com.cyberarcenal.huddle.utils

import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionResponse
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum

// Helper para i-map ang API response string sa ReactionTypeEnum enum
fun mapCurrentReaction(reaction: String?): ReactionTypeEnum? {
    return try {
        ReactionTypeEnum.entries.find { it.value == reaction }
    } catch (e: Exception) {
        null
    } as ReactionTypeEnum?
}

// Helper para i-handle ang total count mula sa statistics
fun getTotalReactionCount(reactionCount: Int?): Int {
    return reactionCount ?: 0
}
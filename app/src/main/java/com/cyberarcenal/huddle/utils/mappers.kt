package com.cyberarcenal.huddle.utils

import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionResponse

// Helper para i-map ang API response string sa ReactionType enum
fun mapCurrentReaction(reaction: String?): ReactionResponse.ReactionType? {
    return try {
        ReactionCreateRequest.ReactionType.entries.find { it.value == reaction }
    } catch (e: Exception) {
        null
    } as ReactionResponse.ReactionType?
}

// Helper para i-handle ang total count mula sa statistics
fun getTotalReactionCount(reactionCount: Int?): Int {
    return reactionCount ?: 0
}
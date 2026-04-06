package com.cyberarcenal.huddle.data.models

import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.api.models.StoryHighlight

// StoryFeedCache.kt
object StoryFeedCache {
    private val cache = mutableMapOf<String, List<StoryFeed>>()

    fun store(sessionId: String, feeds: List<StoryFeed>) {
        cache[sessionId] = feeds
    }

    fun retrieve(sessionId: String): List<StoryFeed>? = cache[sessionId]

    fun clear(sessionId: String) {
        cache.remove(sessionId)
    }
}

object HighlightCache {
    private val cache = mutableMapOf<String, List<StoryHighlight>>()

    fun store(sessionId: String, highlights: List<StoryHighlight>) {
        cache[sessionId] = highlights
    }

    fun retrieve(sessionId: String): List<StoryHighlight>? = cache[sessionId]

    fun clear(sessionId: String) {
        cache.remove(sessionId)
    }
}
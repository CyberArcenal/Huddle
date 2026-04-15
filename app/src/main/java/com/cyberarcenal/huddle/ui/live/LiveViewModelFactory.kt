package com.cyberarcenal.huddle.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.data.repositories.LiveRepository
import com.cyberarcenal.huddle.data.repositories.ReactionsRepository

class LiveViewModelFactory(
    private val repository: LiveRepository,
    private val commentsRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LiveViewModel(repository, commentsRepository, reactionsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

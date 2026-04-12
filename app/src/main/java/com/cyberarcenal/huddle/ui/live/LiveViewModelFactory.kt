package com.cyberarcenal.huddle.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.LiveRepository

class LiveViewModelFactory(
    private val repository: LiveRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LiveViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

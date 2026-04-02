package com.cyberarcenal.huddle.ui.events.createEvent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EventCreateViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventCreateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventCreateViewModel(context.contentResolver, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
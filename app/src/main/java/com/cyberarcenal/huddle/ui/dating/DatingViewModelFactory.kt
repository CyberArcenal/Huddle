// DatingViewModelFactory.kt
package com.cyberarcenal.huddle.ui.dating

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.DatingMessagesRepository
import com.cyberarcenal.huddle.data.repositories.DatingPreferencesRepository
import com.cyberarcenal.huddle.data.repositories.MatchesRepository
import com.cyberarcenal.huddle.data.repositories.UserMatchingRepository

class DatingViewModelFactory(
    private val preferencesRepo: DatingPreferencesRepository,
    private val messagesRepo: DatingMessagesRepository,
    private val matchingRepo: UserMatchingRepository,
    private val matchesRepo: MatchesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatingViewModel(preferencesRepo, messagesRepo, matchingRepo, matchesRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
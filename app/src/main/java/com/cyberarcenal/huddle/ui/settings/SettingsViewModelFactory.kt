package com.cyberarcenal.huddle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.*

class SettingsViewModelFactory(
    private val userProfileRepository: UsersRepository,
    private val userSecurityRepository: UserSecurityRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val logOutRepository: LogOutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                userProfileRepository,
                userSecurityRepository,
                passwordResetRepository,
                logOutRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.cyberarcenal.huddle.ui.groups.creategroup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.data.repositories.UserSearchRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository

class GroupCreationViewModelFactory(
    private val application: Application,
    private val groupRepository: GroupRepository,
    private val userSearchRepository: UserSearchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupCreationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupCreationViewModel(application, groupRepository, userSearchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
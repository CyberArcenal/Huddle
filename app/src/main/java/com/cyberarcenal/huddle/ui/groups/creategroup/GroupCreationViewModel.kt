package com.cyberarcenal.huddle.ui.groups.creategroup

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.GroupCreateRequest
import com.cyberarcenal.huddle.api.models.GroupMemberCreateRequest
import com.cyberarcenal.huddle.api.models.GroupTypeEnum
import com.cyberarcenal.huddle.api.models.PrivacyC6eEnum
import com.cyberarcenal.huddle.api.models.RoleBf6Enum
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.GroupRepository
import com.cyberarcenal.huddle.data.repositories.UserSearchRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

data class InvitedUser(
    val userId: Int,
    val username: String,
    val role: String = "member" // "admin", "moderator", "member"
)

class GroupCreationViewModel(
    application: Application,
    private val groupRepository: GroupRepository,
    private val userSearchsRepository: UserSearchRepository,
) : AndroidViewModel(application) {

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _shortDescription = MutableStateFlow("")
    val shortDescription: StateFlow<String> = _shortDescription.asStateFlow()

    private val _longDescription = MutableStateFlow("")
    val longDescription: StateFlow<String> = _longDescription.asStateFlow()

    private val _groupType = MutableStateFlow(GroupTypeEnum.HOBBY)
    val groupType: StateFlow<GroupTypeEnum> = _groupType.asStateFlow()

    private val _privacy = MutableStateFlow(PrivacyC6eEnum.PUBLIC)
    val privacy: StateFlow<PrivacyC6eEnum> = _privacy.asStateFlow()

    private val _profilePictureUri = MutableStateFlow<Uri?>(null)
    val profilePictureUri: StateFlow<Uri?> = _profilePictureUri.asStateFlow()

    private val _coverPhotoUri = MutableStateFlow<Uri?>(null)
    val coverPhotoUri: StateFlow<Uri?> = _coverPhotoUri.asStateFlow()

    private val _invitedUsers = MutableStateFlow<List<InvitedUser>>(emptyList())
    val invitedUsers: StateFlow<List<InvitedUser>> = _invitedUsers.asStateFlow()

    // For search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserMinimal>>(emptyList())
    val searchResults: StateFlow<List<UserMinimal>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // For image cropping (similar to profile)
    private var pendingImageType: ImageType? = null
    private var pendingUri: Uri? = null

    fun updateGroupName(name: String) { _groupName.value = name }
    fun updateShortDescription(desc: String) { _shortDescription.value = desc }
    fun updateLongDescription(desc: String) { _longDescription.value = desc }
    fun updateGroupType(type: GroupTypeEnum) { _groupType.value = type }
    fun updatePrivacy(privacy: PrivacyC6eEnum) { _privacy.value = privacy }
    fun setProfilePictureUri(uri: Uri?) { _profilePictureUri.value = uri }
    fun setCoverPhotoUri(uri: Uri?) { _coverPhotoUri.value = uri }

    fun setPendingImage(type: ImageType, uri: Uri) {
        pendingImageType = type
        pendingUri = uri
    }

    fun clearPending() {
        pendingImageType = null
        pendingUri = null
    }

    fun getPending(): Pair<ImageType?, Uri?> = pendingImageType to pendingUri

    fun addInvitedUser(user: UserMinimal, role: String = "member") {
        val existing = _invitedUsers.value.find { it.userId == user.id }
        if (existing == null) {
            _invitedUsers.value += InvitedUser(
                            userId = user.id ?: return,
                            username = user.username ?: "User",
                            role = role
                        )
        }
    }

    fun removeInvitedUser(userId: Int) {
        _invitedUsers.value = _invitedUsers.value.filter { it.userId != userId }
    }

    fun updateInvitedUserRole(userId: Int, role: String) {
        _invitedUsers.value = _invitedUsers.value.map {
            if (it.userId == userId) it.copy(role = role) else it
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            userSearchsRepository.searchUsers(query).fold(
                onSuccess = { results ->
                    _searchResults.value = results.data.results
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error("Search failed: ${error.message}")
                }
            )
            _isSearching.value = false
        }
    }

    fun createGroup() {
        viewModelScope.launch {
            if (_groupName.value.isBlank()) {
                _actionState.value = ActionState.Error("Group name is required")
                return@launch
            }
            if (_shortDescription.value.isBlank()) {
                _actionState.value = ActionState.Error("Short description is required")
                return@launch
            }

            _actionState.value = ActionState.Loading("Creating group...")

            // Prepare MultipartBody.Part from URIs (if any)
            val profilePart = _profilePictureUri.value?.let { uri ->
                createMultipartPartFromUri(uri, "profile_picture")
            }
            val coverPart = _coverPhotoUri.value?.let { uri ->
                createMultipartPartFromUri(uri, "cover_photo")
            }

            val request = GroupCreateRequest(
                name = _groupName.value,
                description = _longDescription.value.ifBlank { _shortDescription.value },
                privacy = _privacy.value,
                groupType = _groupType.value,
                profilePicture = profilePart,
                coverPhoto = coverPart
            )

            groupRepository.createGroup(request).fold(
                onSuccess = { response ->
                    if (response.status){
                        _actionState.value = ActionState.Success("Group created successfully!")
                        // After creation, invite members
                        if (_invitedUsers.value.isNotEmpty()) {
                            inviteMembers(response.data.group.id ?: return@fold)
                        }
                    }else{
                        _actionState.value = ActionState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to create group")
                }
            )
        }
    }

    private suspend fun inviteMembers(groupId: Int) {
        for (invite in _invitedUsers.value) {
            val roleEnum = when (invite.role) {
                "admin" -> RoleBf6Enum.ADMIN
                "moderator" -> RoleBf6Enum.MODERATOR
                else -> RoleBf6Enum.MEMBER
            }
            val request = GroupMemberCreateRequest(userId = invite.userId, role = roleEnum)
            groupRepository.addMemberById(groupId, invite.userId, request).fold(
                onSuccess = { /* member added */ },
                onFailure = { error ->
                    _actionState.value = ActionState.Error("Failed to invite ${invite.username}: ${error.message}")
                }
            )
        }
    }

    /**
     * Converts a content URI to a MultipartBody.Part.
     * Copies the file to a temporary cache file to avoid direct file access issues.
     */
    private suspend fun createMultipartPartFromUri(uri: Uri, fieldName: String): MultipartBody.Part? {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver

        // Get the file type from the URI
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val fileName = "$fieldName.${System.currentTimeMillis()}.$extension"

        // Create a temporary file in the app's cache directory
        val tempFile = File(context.cacheDir, fileName)
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(fieldName, tempFile.name, requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            // Optionally delete the temp file after upload? The upload happens later.
            // We can schedule deletion after the request is sent, but for now keep it.
            // For simplicity, we'll not delete immediately; the OS will clean cache eventually.
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

enum class ImageType {
    PROFILE, COVER
}
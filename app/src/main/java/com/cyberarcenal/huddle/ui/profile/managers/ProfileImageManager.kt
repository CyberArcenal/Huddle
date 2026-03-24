package com.cyberarcenal.huddle.ui.profile.managers

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.cyberarcenal.huddle.data.repositories.UserMediaRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.utils.ImageValidator
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ProfileImageManager(
    private val application: Application,
    private val userMediaRepository: UserMediaRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>,
    private val onProfileUpdated: () -> Unit
) {
    enum class UploadType { PROFILE, COVER }

    private val _uploadType = MutableStateFlow<UploadType?>(null)
    val uploadType: StateFlow<UploadType?> = _uploadType.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    fun onImagePickedForProfile(uri: Uri) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Validating image...")
            val validation = ImageValidator.validateImage(
                application.contentResolver, uri
            )
            if (!validation.isValid) {
                _validationError.value = validation.errorMessage
                actionState.value = ActionState.Error(validation.errorMessage ?: "Invalid image")
                return@launch
            }
            _uploadType.value = UploadType.PROFILE
            _selectedImageUri.value = uri
            actionState.value = ActionState.Idle
        }
    }

    fun onImagePickedForCover(uri: Uri) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Validating image...")
            val validation = ImageValidator.validateImage(
                application.contentResolver, uri,
                maxWidth = 4096, maxHeight = 2304
            )
            if (!validation.isValid) {
                _validationError.value = validation.errorMessage
                actionState.value = ActionState.Error(validation.errorMessage ?: "Invalid image")
                return@launch
            }
            _uploadType.value = UploadType.COVER
            _selectedImageUri.value = uri
            actionState.value = ActionState.Idle
        }
    }

    fun startProfileCropIntent(context: Context, sourceUri: Uri): Intent {
        val destName = "cropped_profile_${System.currentTimeMillis()}.jpg"
        val destUri = Uri.fromFile(File(context.cacheDir, destName))
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setCircleDimmedLayer(true)
            setShowCropFrame(false)
            setShowCropGrid(false)
            setHideBottomControls(false)
            setToolbarTitle("Crop profile photo")
        }
        return UCrop.of(sourceUri, destUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(400, 400)
            .getIntent(context)
    }

    fun startCoverCropIntent(context: Context, sourceUri: Uri): Intent {
        val destUri = Uri.fromFile(File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setShowCropFrame(true)
            setShowCropGrid(true)
        }
        return UCrop.of(sourceUri, destUri)
            .withOptions(options)
            .withAspectRatio(4f, 1f)
            .withMaxResultSize(800, 200)
            .getIntent(context)
    }

    fun onCropResult(croppedUri: Uri) {
        val type = _uploadType.value ?: run {
            actionState.value = ActionState.Error("No pending upload")
            return
        }
        uploadCroppedImage(croppedUri, type)
    }

    fun onCropError(errorMessage: String) {
        actionState.value = ActionState.Error(errorMessage)
        clearCropState()
    }

    fun cancelCrop() = clearCropState()

    fun removeProfilePicture() {
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Removing profile picture...")
            userMediaRepository.removeProfilePicture().fold(
                onSuccess = {
                    actionState.value = ActionState.Success("Profile picture removed")
                    onProfileUpdated()
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to remove")
                }
            )
        }
    }

    fun removeCoverPhoto() {
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Removing cover photo...")
            userMediaRepository.removeCoverPhoto().fold(
                onSuccess = {
                    actionState.value = ActionState.Success("Cover photo removed")
                    onProfileUpdated()
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to remove")
                }
            )
        }
    }

    private fun uploadCroppedImage(croppedUri: Uri, type: UploadType) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Uploading...")
            val croppedFile = getFileFromUri(croppedUri) ?: run {
                actionState.value = ActionState.Error("Failed to get cropped image file")
                return@launch
            }
            val mimeType = getMimeType(croppedUri) ?: "image/jpeg"
            val uploadResult = when (type) {
                UploadType.PROFILE -> userMediaRepository.uploadProfilePicture(croppedFile, mimeType)
                UploadType.COVER -> userMediaRepository.uploadCoverPhoto(croppedFile, mimeType)
            }
            uploadResult.fold(
                onSuccess = {
                    actionState.value = ActionState.Success(
                        if (type == UploadType.PROFILE) "Profile picture updated" else "Cover photo updated"
                    )
                    onProfileUpdated()
                    clearCropState()
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Upload failed")
                }
            )
            croppedFile.delete()
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = application.contentResolver
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("cropped_", ".jpg", application.cacheDir)
            tempFile.outputStream().use { inputStream.copyTo(it) }
            tempFile
        } catch (e: Exception) { null }
    }

    private fun getMimeType(uri: Uri) = application.contentResolver.getType(uri)

    private fun clearCropState() {
        _selectedImageUri.value = null
        _uploadType.value = null
        _validationError.value = null
    }
}
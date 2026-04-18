package com.cyberarcenal.huddle.ui.editprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.UpdateFullNameInputRequest
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditNameUiState(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val canEdit: Boolean = true,
    val cooldownMessage: String? = null
)

class EditNameViewModel(
    private val repository: UsersRepository,
    initialFirstName: String,
    initialMiddleName: String,
    initialLastName: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditNameUiState(
        firstName = initialFirstName,
        middleName = initialMiddleName,
        lastName = initialLastName
    ))
    val uiState: StateFlow<EditNameUiState> = _uiState.asStateFlow()

    init {
        checkEditStatus()
    }

    private fun checkEditStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getNameEditStatus().fold(
                onSuccess = { response ->
                    val status = response.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canEdit = status.canEdit,
                        cooldownMessage = if (!status.canEdit) {
                            "You can change your name again in ${status.daysRemaining ?: 0} days."
                        } else null
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    fun onFirstNameChange(name: String) { _uiState.value = _uiState.value.copy(firstName = name) }
    fun onMiddleNameChange(name: String) { _uiState.value = _uiState.value.copy(middleName = name) }
    fun onLastNameChange(name: String) { _uiState.value = _uiState.value.copy(lastName = name) }

    fun saveName() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.firstName.isBlank() || currentState.lastName.isBlank()) {
                _uiState.value = currentState.copy(error = "First and Last names are required")
                return@launch
            }

            _uiState.value = currentState.copy(isLoading = true, error = null)
            val request = UpdateFullNameInputRequest(
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                middleName = currentState.middleName.takeIf { it.isNotBlank() }
            )

            repository.changeFullName(request).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                }
            )
        }
    }
}

class EditNameViewModelFactory(
    private val repository: UsersRepository,
    private val firstName: String,
    private val middleName: String,
    private val lastName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EditNameViewModel(repository, firstName, middleName, lastName) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameScreen(
    navController: NavController,
    firstName: String,
    middleName: String,
    lastName: String,
    repository: UsersRepository = UsersRepository()
) {
    val viewModel: EditNameViewModel = viewModel(
        factory = EditNameViewModelFactory(repository, firstName, middleName, lastName)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Name", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.canEdit) {
                        TextButton(onClick = { viewModel.saveName() }, enabled = !uiState.isLoading) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            if (!uiState.canEdit) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.cooldownMessage ?: "Cooldown active",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canEdit && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.middleName,
                onValueChange = viewModel::onMiddleNameChange,
                label = { Text("Middle Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canEdit && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canEdit && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp)
            )
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Note: You can only change your name once every 30 days.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

package com.cyberarcenal.huddle.ui.dating

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.DatingPreferenceCreateUpdateRequest
import com.cyberarcenal.huddle.api.models.DatingPreferenceDetail
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatingPreferencesScreen(
    viewModel: DatingViewModel,
    globalSnackbarHostState: SnackbarHostState
) {
    val preferencesState by viewModel.preferencesState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    var preferredAgeMin by remember { mutableStateOf("") }
    var preferredAgeMax by remember { mutableStateOf("") }
    var preferredGender by remember { mutableStateOf("") }
    var maxDistanceKm by remember { mutableStateOf("") }
    var personalityMatch by remember { mutableStateOf(false) }
    var loveLanguageMatch by remember { mutableStateOf(false) }
    var relationshipGoalMatch by remember { mutableStateOf(false) }

    // Gender dropdown state
    var genderExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("male", "female", "other", "")

    // Load current preferences into state
    LaunchedEffect(preferencesState) {
        if (preferencesState is DatingUiState.Success) {
            val prefs = (preferencesState as DatingUiState.Success).data as DatingPreferenceDetail
            preferredAgeMin = prefs.preferredAgeMin?.toString() ?: ""
            preferredAgeMax = prefs.preferredAgeMax?.toString() ?: ""
            preferredGender = prefs.preferredGender ?: ""
            maxDistanceKm = prefs.maxDistanceKm?.toString() ?: ""
            personalityMatch = prefs.personalityMatch ?: false
            loveLanguageMatch = prefs.loveLanguageMatch ?: false
            relationshipGoalMatch = prefs.relationshipGoalMatch ?: false
        }
    }

    fun validateAndSave() {
        // Validate age min
        if (preferredAgeMin.isNotBlank()) {
            val minAge = preferredAgeMin.toLongOrNull()
            if (minAge == null || minAge < 18) {
                coroutineScope.launch {
                    globalSnackbarHostState.showSnackbar("Min age must be a number ≥ 18")
                }
                return
            }
        }
        // Validate age max
        if (preferredAgeMax.isNotBlank()) {
            val maxAge = preferredAgeMax.toLongOrNull()
            if (maxAge == null) {
                coroutineScope.launch {
                    globalSnackbarHostState.showSnackbar("Max age must be a valid number")
                }
                return
            }
        }
        // Validate range
        if (preferredAgeMin.isNotBlank() && preferredAgeMax.isNotBlank()) {
            val minAge = preferredAgeMin.toLong()
            val maxAge = preferredAgeMax.toLong()
            if (minAge > maxAge) {
                coroutineScope.launch {
                    globalSnackbarHostState.showSnackbar("Min age cannot be greater than max age")
                }
                return
            }
        }
        // Validate distance
        if (maxDistanceKm.isNotBlank()) {
            val distance = maxDistanceKm.toLongOrNull()
            if (distance == null || distance <= 0) {
                coroutineScope.launch {
                    globalSnackbarHostState.showSnackbar("Max distance must be a positive number")
                }
                return
            }
        }

        isSaving = true
        val request = DatingPreferenceCreateUpdateRequest(
            preferredAgeMin = preferredAgeMin.toLongOrNull(),
            preferredAgeMax = preferredAgeMax.toLongOrNull(),
            preferredGender = preferredGender.ifEmpty { null },
            maxDistanceKm = maxDistanceKm.toLongOrNull(),
            personalityMatch = personalityMatch,
            loveLanguageMatch = loveLanguageMatch,
            relationshipGoalMatch = relationshipGoalMatch
        )
        viewModel.updatePreferences(request)
    }

    // Observe update result
    LaunchedEffect(preferencesState) {
        if (preferencesState is DatingUiState.Error && (preferencesState as DatingUiState.Error).message.isNotBlank()) {
            isSaving = false
            coroutineScope.launch {
                globalSnackbarHostState.showSnackbar((preferencesState as DatingUiState.Error).message)
            }
        } else if (preferencesState is DatingUiState.Success) {
            isSaving = false
            coroutineScope.launch {
                globalSnackbarHostState.showSnackbar("Preferences updated successfully!")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            preferencesState is DatingUiState.Loading && !isSaving -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            preferencesState is DatingUiState.Error && !isSaving -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${(preferencesState as DatingUiState.Error).message}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadPreferences() }) { Text("Retry") }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Age Min
                    OutlinedTextField(
                        value = preferredAgeMin,
                        onValueChange = { preferredAgeMin = it },
                        label = { Text("Min Age (18+)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = preferredAgeMin.isNotBlank() && preferredAgeMin.toLongOrNull()?.let { it < 18 } == true,
                        supportingText = {
                            if (preferredAgeMin.isNotBlank() && preferredAgeMin.toLongOrNull()?.let { it < 18 } == true) {
                                Text("Must be at least 18", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    // Age Max
                    OutlinedTextField(
                        value = preferredAgeMax,
                        onValueChange = { preferredAgeMax = it },
                        label = { Text("Max Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Gender Dropdown
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = preferredGender.ifEmpty { "Any" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Preferred Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.ifEmpty { "Any" }) },
                                    onClick = {
                                        preferredGender = option
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Max Distance
                    OutlinedTextField(
                        value = maxDistanceKm,
                        onValueChange = { maxDistanceKm = it },
                        label = { Text("Max Distance (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = maxDistanceKm.isNotBlank() && maxDistanceKm.toLongOrNull()?.let { it <= 0 } == true
                    )
                    // Switches
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Personality Match", modifier = Modifier.weight(1f))
                        Switch(checked = personalityMatch, onCheckedChange = { personalityMatch = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Love Language Match", modifier = Modifier.weight(1f))
                        Switch(checked = loveLanguageMatch, onCheckedChange = { loveLanguageMatch = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Relationship Goal Match", modifier = Modifier.weight(1f))
                        Switch(checked = relationshipGoalMatch, onCheckedChange = { relationshipGoalMatch = it })
                    }
                    // Save Button with loading state
                    Button(
                        onClick = { validateAndSave() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text("Save Preferences")
                        }
                    }
                }
            }
        }
    }
}
package com.cyberarcenal.huddle.ui.profile.managers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.PersonalityQuestion
import com.cyberarcenal.huddle.api.models.PersonalityTypeResponse
import com.cyberarcenal.huddle.data.repositories.PersonalityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonalityViewModel(
    private val repository: PersonalityRepository = PersonalityRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonalityUiState>(PersonalityUiState.Intro)
    val uiState: StateFlow<PersonalityUiState> = _uiState.asStateFlow()

    private val _questions = MutableStateFlow<List<PersonalityQuestion>>(emptyList())
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, Int>>(emptyMap()) // questionId -> optionId

    fun startQuiz() {
        viewModelScope.launch {
            _uiState.value = PersonalityUiState.Loading
            repository.getQuestions().fold(
                onSuccess = { questions ->
                    _questions.value = questions
                    _currentQuestionIndex.value = 0
                    _answers.value = emptyMap()
                    if (questions.isNotEmpty()) {
                        _uiState.value = PersonalityUiState.Question(questions[0])
                    } else {
                        _uiState.value = PersonalityUiState.Error("No questions available")
                    }
                },
                onFailure = { error ->
                    _uiState.value = PersonalityUiState.Error(error.message ?: "Failed to load questions")
                }
            )
        }
    }

    fun submitAnswer(optionId: Int) {
        val currentQuestions = _questions.value
        val currentIndex = _currentQuestionIndex.value
        val questionId = currentQuestions[currentIndex].id?.toString() ?: return

        _answers.value = _answers.value + (questionId to optionId)

        if (currentIndex < currentQuestions.size - 1) {
            val nextIndex = currentIndex + 1
            _currentQuestionIndex.value = nextIndex
            _uiState.value = PersonalityUiState.Question(currentQuestions[nextIndex])
        } else {
            completeQuiz()
        }
    }

    private fun completeQuiz() {
        viewModelScope.launch {
            _uiState.value = PersonalityUiState.Loading
            repository.submitAnswers(_answers.value).fold(
                onSuccess = { response ->
                    _uiState.value = PersonalityUiState.Result(response)
                },
                onFailure = { error ->
                    _uiState.value = PersonalityUiState.Error(error.message ?: "Failed to submit answers")
                }
            )
        }
    }

    fun goBack() {
        val currentIndex = _currentQuestionIndex.value
        if (currentIndex > 0) {
            val prevIndex = currentIndex - 1
            _currentQuestionIndex.value = prevIndex
            _uiState.value = PersonalityUiState.Question(_questions.value[prevIndex])
        } else {
            _uiState.value = PersonalityUiState.Intro
        }
    }
    
    fun getTotalQuestions(): Int = _questions.value.size
}

sealed class PersonalityUiState {
    object Intro : PersonalityUiState()
    object Loading : PersonalityUiState()
    data class Question(val question: PersonalityQuestion) : PersonalityUiState()
    data class Result(val response: PersonalityTypeResponse) : PersonalityUiState()
    data class Error(val message: String) : PersonalityUiState()
}

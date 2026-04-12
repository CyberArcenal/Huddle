package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberarcenal.huddle.api.models.PersonalityQuestion
import com.cyberarcenal.huddle.api.models.PersonalityTypeEnum
import com.cyberarcenal.huddle.api.models.PersonalityTypeResponse
import com.cyberarcenal.huddle.ui.profile.Enums.getDisplayName
import com.cyberarcenal.huddle.ui.profile.managers.PersonalityUiState
import com.cyberarcenal.huddle.ui.profile.managers.PersonalityViewModel
import com.cyberarcenal.huddle.ui.theme.Gradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityQuizScreen(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    viewModel: PersonalityViewModel = viewModel(),
    globalSnackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val totalQuestions = viewModel.getTotalQuestions()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personality Test") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (uiState is PersonalityUiState.Question) {
                            viewModel.goBack()
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },

            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(targetState = uiState, label = "quiz_state") { state ->
                when (state) {
                    is PersonalityUiState.Intro -> IntroScreen(onStart = { viewModel.startQuiz() })
                    is PersonalityUiState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    is PersonalityUiState.Question -> QuestionScreen(
                        question = state.question,
                        currentIndex = currentIndex,
                        totalQuestions = totalQuestions,
                        onAnswerSelected = { viewModel.submitAnswer(it) }
                    )
                    is PersonalityUiState.Result -> ResultScreen(
                        result = state.response,
                        onFinish = { onComplete() }
                    )
                    is PersonalityUiState.Error -> ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.startQuiz() }
                    )
                }
            }
        }
    }
}

@Composable
fun IntroScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Discover Your Personality",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Answer a few questions to find out your MBTI personality type and how it affects your relationships.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Gradients.buttonGradient, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("Start Test", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuestionScreen(
    question: PersonalityQuestion,
    currentIndex: Int,
    totalQuestions: Int,
    onAnswerSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // LinearProgressIndicator lambda version
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Question ${currentIndex + 1} of $totalQuestions",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        // Likert scale options aligned with app theme colors
        val colors = MaterialTheme.colorScheme
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuizOption(text = "Strongly Agree", color = colors.primary) { onAnswerSelected(5) }
            QuizOption(text = "Agree", color = colors.secondary) { onAnswerSelected(4) }
            QuizOption(text = "Neutral", color = Color(0xFFFFB74D)) { onAnswerSelected(3) } // SoftGold
            QuizOption(text = "Disagree", color = colors.tertiary) { onAnswerSelected(2) }
            QuizOption(text = "Strongly Disagree", color = Color.Gray) { onAnswerSelected(1) }
        }
    }
}

@Composable
fun QuizOption(text: String, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ResultScreen(
    result: PersonalityTypeResponse,
    onFinish: () -> Unit
) {
    val typeString = result.data.personalityType ?: "Unknown"
    val typeEnum = PersonalityTypeEnum.decode(typeString)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Test Completed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Gradients.primaryGradient,
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = typeString,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = typeEnum?.getDisplayName() ?: "You are a $typeString",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your personality type has been updated on your profile.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Gradients.buttonGradient, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("Go back to Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

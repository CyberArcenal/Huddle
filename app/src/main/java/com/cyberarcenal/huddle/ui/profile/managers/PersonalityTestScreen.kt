package com.cyberarcenal.huddle.ui.profile.managers

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
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.PersonalityQuestion
import com.cyberarcenal.huddle.api.models.PersonalityTypeResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityTestScreen(
    navController: NavController,
    viewModel: PersonalityViewModel = viewModel()
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
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PersonalityUiState.Intro -> IntroScreen(onStart = { viewModel.startQuiz() })
                is PersonalityUiState.Loading -> LoadingScreen()
                is PersonalityUiState.Question -> QuestionScreen(
                    question = state.question,
                    currentIndex = currentIndex,
                    totalQuestions = totalQuestions,
                    onAnswerSelected = { viewModel.submitAnswer(it) }
                )
                is PersonalityUiState.Result -> ResultScreen(
                    response = state.response,
                    onFinish = { navController.popBackStack() }
                )
                is PersonalityUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.startQuiz() }
                )
            }
        }
    }
}

@Composable
fun IntroScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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
            text = "Understand yourself better and how you interact with others. This test will help us match you with like-minded people in the Huddle community.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoItem(title = "Time", value = "~5 minutes")
                InfoItem(title = "Questions", value = "20-40 short questions")
                InfoItem(title = "Accuracy", value = "Based on MBTI dimensions")
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Start Personality Test", fontSize = 18.sp)
        }
    }
}

@Composable
fun InfoItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontWeight = FontWeight.Medium)
        Text(text = value, color = MaterialTheme.colorScheme.primary)
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
    ) {
        // Progress bar
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        
        // Likert scale options (Strongly Disagree to Strongly Agree)
        // Since the backend expects Int optionId, we map 1-5
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnswerOption(text = "Strongly Agree", color = Color(0xFF4CAF50)) { onAnswerSelected(5) }
            AnswerOption(text = "Agree", color = Color(0xFF8BC34A)) { onAnswerSelected(4) }
            AnswerOption(text = "Neutral", color = Color(0xFF9E9E9E)) { onAnswerSelected(3) }
            AnswerOption(text = "Disagree", color = Color(0xFFFF9800)) { onAnswerSelected(2) }
            AnswerOption(text = "Strongly Disagree", color = Color(0xFFF44336)) { onAnswerSelected(1) }
        }
    }
}

@Composable
fun AnswerOption(text: String, color: Color, onClick: () -> Unit) {
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
fun ResultScreen(response: PersonalityTypeResponse, onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your Result Is Ready!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = response.data.personalityType ?: "???",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "You are now identified as ${response.data.personalityType}. This will be shown on your profile.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Finish", fontSize = 18.sp)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Oops!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

package com.example.ui.screens.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Quiz
import com.example.data.model.QuizQuestion
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.QuizPlayerSkeletonLoader
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayerScreen(
  quizId: String,
  repository: QuizPollRepository,
  onNavigateBack: () -> Unit,
  onQuizCompleted: (score: Int, total: Int, timeTaken: Int, answers: Map<Int, List<Int>>) -> Unit,
  modifier: Modifier = Modifier
) {
  var quiz by remember { mutableStateOf<Quiz?>(null) }
  var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var currentIndex by remember { mutableIntStateOf(0) }

  // Map of question index -> selected option indices
  var userAnswers by remember { mutableStateOf<Map<Int, List<Int>>>(emptyMap()) }
  var remainingSeconds by remember { mutableLongStateOf(60L) }
  var timeTakenSeconds by remember { mutableIntStateOf(0) }
  var isSubmitting by remember { mutableStateOf(false) }

  // Fetch quiz and questions
  LaunchedEffect(quizId) {
    try {
      val q = repository.getQuiz(quizId)
      val qs = repository.getQuizQuestions(quizId)
      quiz = q
      questions = qs
      if (q != null && q.timeLimitSeconds > 0) {
        remainingSeconds = q.timeLimitSeconds
      }
      isLoading = false
    } catch (_: Exception) {
      isLoading = false
    }
  }

  // Timer countdown
  LaunchedEffect(isLoading, remainingSeconds) {
    if (!isLoading && quiz != null && quiz!!.timeLimitSeconds > 0 && remainingSeconds > 0 && !isSubmitting) {
      delay(1000L)
      remainingSeconds -= 1
      timeTakenSeconds += 1
      if (remainingSeconds <= 0) {
        // Auto-submit on time expiry
        isSubmitting = true
        var score = 0
        questions.forEachIndexed { i, q ->
          val selected = userAnswers[i] ?: emptyList()
          if (selected.sorted() == q.correctAnswers.sorted()) {
            score++
          }
        }
        onQuizCompleted(score, questions.size, timeTakenSeconds, userAnswers)
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = quiz?.title ?: "Quiz Challenge",
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
          }
        },
        actions = {
          if (quiz != null && quiz!!.timeLimitSeconds > 0) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (remainingSeconds <= 10) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.padding(end = 12.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.Timer,
                  contentDescription = "Timer",
                  tint = if (remainingSeconds <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${remainingSeconds}s",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (remainingSeconds <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      )
    }
  ) { innerPadding ->
    if (isLoading) {
      QuizPlayerSkeletonLoader(modifier = Modifier.padding(innerPadding))
    } else if (questions.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Text("No questions found for this quiz.", style = MaterialTheme.typography.bodyLarge)
      }
    } else {
      val currentQ = questions[currentIndex]
      val selectedOptions = userAnswers[currentIndex] ?: emptyList()

      Column(
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        // Progress header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Question ${currentIndex + 1} of ${questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${((currentIndex + 1).toFloat() / questions.size * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
          progress = { (currentIndex + 1).toFloat() / questions.size },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Question Card
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = currentQ.questionText,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Options list
            currentQ.options.forEachIndexed { optIndex, optionText ->
              val isSelected = selectedOptions.contains(optIndex)

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    val updated = if (currentQ.questionType == "MULTIPLE_MCQ") {
                      if (selectedOptions.contains(optIndex)) selectedOptions - optIndex else selectedOptions + optIndex
                    } else {
                      listOf(optIndex)
                    }
                    userAnswers = userAnswers + (currentIndex to updated)
                  }
                  .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                  ),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(24.dp)
                  ) {
                    if (isSelected) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          Icons.Default.Check,
                          contentDescription = "Selected",
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation & Submit row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          OutlinedButton(
            onClick = { if (currentIndex > 0) currentIndex-- },
            enabled = currentIndex > 0,
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Previous")
          }

          if (currentIndex < questions.size - 1) {
            Button(
              onClick = { currentIndex++ },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag("quiz_next_btn")
            ) {
              Text("Next")
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
          } else {
            Button(
              onClick = {
                isSubmitting = true
                var score = 0
                questions.forEachIndexed { i, q ->
                  val selected = userAnswers[i] ?: emptyList()
                  if (selected.sorted() == q.correctAnswers.sorted()) {
                    score++
                  }
                }
                onQuizCompleted(score, questions.size, timeTakenSeconds, userAnswers)
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag("quiz_submit_btn")
            ) {
              Text("Finish & Submit", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

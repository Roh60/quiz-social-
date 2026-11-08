package com.example.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdsManager
import com.example.data.model.Quiz
import com.example.data.model.QuizQuestion
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.BannerAdView
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
  quizId: String,
  score: Int,
  totalQuestions: Int,
  timeTakenSeconds: Int,
  userAnswers: Map<Int, List<Int>>,
  repository: QuizPollRepository,
  onRetryQuiz: () -> Unit,
  onReturnHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  var quiz by remember { mutableStateOf<Quiz?>(null) }
  var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
  var isSavedToCloud by remember { mutableStateOf(false) }
  var displayedScore by remember(quizId) { mutableStateOf(score) }
  var saveError by remember { mutableStateOf<String?>(null) }
  val submissionId = rememberSaveable(quizId) { UUID.randomUUID().toString() }

  val context = LocalContext.current

  LaunchedEffect(quizId) {
    AdsManager.onQuizCompleted(context as? Activity)
    try {
      quiz = repository.getQuiz(quizId)
      questions = repository.getQuizQuestions(quizId)
      if (!isSavedToCloud) {
        val attempt = repository.submitQuizAttempt(
          quizId = quizId,
          quizTitle = quiz?.title ?: "Community Quiz",
          score = score,
          totalQuestions = totalQuestions,
          timeTakenSeconds = timeTakenSeconds,
          userAnswers = userAnswers,
          submissionId = submissionId
        )
        displayedScore = attempt.score
        isSavedToCloud = true
        try {
          val answerReview = repository.getQuizAnswerReview(quizId, attempt.id)
          questions = questions.mapIndexed { index, question ->
            question.copy(correctAnswers = answerReview[index] ?: emptyList())
          }
        } catch (_: Exception) {
          saveError = "Score saved; answer review is temporarily unavailable."
        }
      }
    } catch (e: Exception) {
      saveError = e.localizedMessage ?: "Could not sync quiz result."
    }
  }

  val accuracy = if (totalQuestions > 0) (displayedScore.toDouble() / totalQuestions * 100.0) else 0.0

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Quiz Results", fontWeight = FontWeight.Bold) }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Score Ring & Header
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(110.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$displayedScore / $totalQuestions",
              style = MaterialTheme.typography.displayMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "${accuracy.toInt()}% Score",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = when {
          accuracy >= 90.0 -> "Outstanding Mastery!"
          accuracy >= 70.0 -> "Great Job!"
          accuracy >= 50.0 -> "Good Effort!"
          else -> "Keep Practicing!"
        },
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "${quiz?.title ?: "Quiz Challenge"} • ${timeTakenSeconds}s taken",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (isSavedToCloud) {
        Text("Result verified and saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
      } else if (saveError != null) {
        Text("Result not synced: $saveError", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onRetryQuiz,
          modifier = Modifier.weight(1f).height(48.dp).testTag("quiz_retry_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Retry")
          Spacer(modifier = Modifier.width(6.dp))
          Text("Retry Quiz")
        }
        Button(
          onClick = onReturnHome,
          modifier = Modifier.weight(1f).height(48.dp).testTag("quiz_return_home_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Home Feed")
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Individual Quiz Competition Leaderboard
      com.example.ui.components.IndividualQuizLeaderboardView(
        quizId = quizId,
        repository = repository,
        currentUserId = repository.getCurrentUserId() ?: "",
        modifier = Modifier.fillMaxWidth().testTag("result_quiz_leaderboard")
      )

      Spacer(modifier = Modifier.height(30.dp))

      // Answer Breakdown Review
      Text(
        text = "Review Answers & Explanations",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(12.dp))

      questions.forEachIndexed { qIndex, q ->
        val userSelected = userAnswers[qIndex] ?: emptyList()
        val isCorrect = userSelected.sorted() == q.correctAnswers.sorted()

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (isCorrect) "Correct" else "Incorrect",
                tint = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Q${qIndex + 1}: ${q.questionText}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            q.options.forEachIndexed { optIndex, optText ->
              val wasSelectedByUser = userSelected.contains(optIndex)
              val isCorrectOption = q.correctAnswers.contains(optIndex)

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                  isCorrectOption -> Color(0xFF10B981).copy(alpha = 0.15f)
                  wasSelectedByUser -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                  else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = optText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCorrectOption) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCorrectOption) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                  )
                  if (isCorrectOption) {
                    Text("✓ Correct", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                  } else if (wasSelectedByUser) {
                    Text("✗ Your Choice", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            if (q.explanation.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Explanation: ${q.explanation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
      BannerAdView(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
    }
  }
}


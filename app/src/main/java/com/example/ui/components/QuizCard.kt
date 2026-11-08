package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizQuestion

@Composable
fun QuizCard(
  question: String,
  options: List<String>,
  correctAnswerIndex: Int,
  modifier: Modifier = Modifier,
  questionId: String = "quiz_q",
  category: String = "General Knowledge",
  difficulty: String = "Medium",
  explanation: String = "",
  onAnswerSubmitted: ((selectedIndex: Int, isCorrect: Boolean) -> Unit)? = null
) {
  var selectedIndex by remember(question) { mutableIntStateOf(-1) }
  var isSubmitted by remember(question) { mutableStateOf(false) }

  val isCorrect = selectedIndex == correctAnswerIndex
  val optionLetters = listOf("A", "B", "C", "D", "E", "F")

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("quiz_card_$questionId"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      // Header: Category & Difficulty Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = category,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when (difficulty.lowercase()) {
            "easy" -> Color(0xFF10B981).copy(alpha = 0.15f)
            "hard" -> Color(0xFFEF4444).copy(alpha = 0.15f)
            else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
          }
        ) {
          Text(
            text = difficulty.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when (difficulty.lowercase()) {
              "easy" -> Color(0xFF059669)
              "hard" -> Color(0xFFDC2626)
              else -> Color(0xFFD97706)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Question Title
      Text(
        text = question,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Selectable Answer Option Buttons
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, optionText ->
          val isOptionSelected = selectedIndex == index
          val isThisCorrect = index == correctAnswerIndex

          val backgroundColor by animateColorAsState(
            targetValue = when {
              isSubmitted && isThisCorrect -> Color(0xFF10B981).copy(alpha = 0.18f)
              isSubmitted && isOptionSelected && !isThisCorrect -> Color(0xFFEF4444).copy(alpha = 0.18f)
              isOptionSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
              else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
            label = "option_bg_$index"
          )

          val borderColor by animateColorAsState(
            targetValue = when {
              isSubmitted && isThisCorrect -> Color(0xFF059669)
              isSubmitted && isOptionSelected && !isThisCorrect -> Color(0xFFDC2626)
              isOptionSelected -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            },
            label = "option_border_$index"
          )

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable(enabled = !isSubmitted) {
                selectedIndex = index
              }
              .border(
                width = if (isOptionSelected || (isSubmitted && isThisCorrect)) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
              )
              .testTag("quiz_option_btn_${questionId}_$index"),
            color = backgroundColor
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Letter Indicator (A, B, C, D)
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      isSubmitted && isThisCorrect -> Color(0xFF059669)
                      isSubmitted && isOptionSelected && !isThisCorrect -> Color(0xFFDC2626)
                      isOptionSelected -> MaterialTheme.colorScheme.primary
                      else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (isSubmitted && isThisCorrect) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Correct",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                } else if (isSubmitted && isOptionSelected && !isThisCorrect) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Incorrect",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                } else {
                  Text(
                    text = optionLetters.getOrElse(index) { "${index + 1}" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOptionSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bottom Actions: Submit or Result Feedback
      if (!isSubmitted) {
        Button(
          onClick = {
            if (selectedIndex != -1) {
              isSubmitted = true
              onAnswerSubmitted?.invoke(selectedIndex, selectedIndex == correctAnswerIndex)
            }
          },
          enabled = selectedIndex != -1,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_submit_btn_$questionId")
        ) {
          Text("Submit Answer", fontWeight = FontWeight.Bold)
        }
      } else {
        // Feedback Card
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(
                if (isCorrect) Color(0xFF10B981).copy(alpha = 0.12f)
                else Color(0xFFEF4444).copy(alpha = 0.12f)
              )
              .padding(14.dp)
              .testTag("quiz_feedback_banner_$questionId")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Info,
                contentDescription = null,
                tint = if (isCorrect) Color(0xFF059669) else Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isCorrect) "Correct! Well done!" else "Not quite! Correct answer is: ${options.getOrElse(correctAnswerIndex) { "" }}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) Color(0xFF059669) else Color(0xFFDC2626)
              )
            }

            if (explanation.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.Lightbulb,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = explanation,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
              onClick = {
                selectedIndex = -1
                isSubmitted = false
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.align(Alignment.End).testTag("quiz_try_again_btn_$questionId")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Try Again", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }
  }
}

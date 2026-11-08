package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.QuizCategories
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch

/**
 * Dialog component for creating community polls and saving them directly to Firestore 'polls' collection.
 */
@Composable
fun PollCreatorDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onPollCreated: (pollId: String) -> Unit,
  repository: QuizPollRepository,
  modifier: Modifier = Modifier
) {
  if (!isOpen) return

  val scope = rememberCoroutineScope()

  var question by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("General Knowledge") }
  val options = remember { mutableStateListOf("", "") }
  var isMultipleChoice by remember { mutableStateOf(false) }
  var allowVoteChange by remember { mutableStateOf(true) }

  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val categories = remember { QuizCategories.ALL }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    modifier = modifier.testTag("poll_creator_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Poll,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Create Community Poll",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(
          onClick = onDismiss,
          enabled = !isSubmitting,
          modifier = Modifier.size(36.dp).testTag("poll_creator_close_btn")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp)
      ) {
        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
              .testTag("poll_creator_error_banner")
          ) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        // 1. Question Input
        Text(
          text = "Poll Question *",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = question,
          onValueChange = {
            question = it
            errorMessage = null
          },
          placeholder = { Text("e.g. Which programming language do you use most?") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("poll_creator_question_input"),
          shape = RoundedCornerShape(12.dp),
          singleLine = false,
          maxLines = 3
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Context / Description Input
        Text(
          text = "Description / Context (Optional)",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          placeholder = { Text("Provide details or encourage discussion...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("poll_creator_description_input"),
          shape = RoundedCornerShape(12.dp),
          singleLine = false,
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Category Selector
        Text(
          text = "Category *",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { selectedCategory = cat }
                .testTag("poll_creator_category_chip_$cat")
            ) {
              Text(
                text = cat,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Options List
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Poll Options (2 - 6) *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${options.size}/6",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        options.forEachIndexed { index, optionText ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = optionText,
              onValueChange = { newText ->
                options[index] = newText
                errorMessage = null
              },
              placeholder = { Text("Option ${index + 1}") },
              modifier = Modifier
                .weight(1f)
                .testTag("poll_creator_option_input_$index"),
              shape = RoundedCornerShape(10.dp),
              singleLine = true
            )

            if (options.size > 2) {
              Spacer(modifier = Modifier.width(6.dp))
              IconButton(
                onClick = { options.removeAt(index) },
                modifier = Modifier.size(36.dp).testTag("poll_creator_remove_option_$index")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Remove option",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }

        if (options.size < 6) {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = { options.add("") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("poll_creator_add_option_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Option")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Settings Toggles
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Multiple Choice",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Allow community members to select multiple options",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isMultipleChoice,
            onCheckedChange = { isMultipleChoice = it },
            modifier = Modifier.testTag("poll_creator_multiple_choice_toggle")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Allow Vote Changing",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Permit voters to update their answers later",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = allowVoteChange,
            onCheckedChange = { allowVoteChange = it },
            modifier = Modifier.testTag("poll_creator_vote_change_toggle")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanQuestion = question.trim()
          val cleanOptions = options.map { it.trim() }.filter { it.isNotBlank() }

          if (cleanQuestion.isBlank()) {
            errorMessage = "Please enter a question for the poll."
            return@Button
          }
          if (cleanOptions.size < 2) {
            errorMessage = "Please provide at least 2 non-blank options."
            return@Button
          }

          scope.launch {
            isSubmitting = true
            errorMessage = null
            try {
              val pollId = repository.createPoll(
                question = cleanQuestion,
                description = description.trim(),
                category = selectedCategory,
                options = cleanOptions,
                isMultipleChoice = isMultipleChoice,
                allowVoteChange = allowVoteChange,
                isAnonymous = false,
                isPublic = true
              )
              isSubmitting = false
              onPollCreated(pollId)
              onDismiss()
            } catch (e: Exception) {
              isSubmitting = false
              errorMessage = "Failed to save poll: ${e.message ?: "Unknown error"}"
            }
          }
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("poll_creator_submit_btn")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Saving...")
        } else {
          Text("Publish Poll")
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSubmitting,
        modifier = Modifier.testTag("poll_creator_cancel_btn")
      ) {
        Text("Cancel")
      }
    }
  )
}

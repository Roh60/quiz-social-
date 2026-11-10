package com.example.ui.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DraftDao
import com.example.data.local.QuizDraftEntity
import com.example.data.model.QuizCategories
import com.example.data.model.QuizQuestion
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Data holder for question drafting in the quiz creation screen.
 */
class EditableQuestionState(
  val id: String = UUID.randomUUID().toString(),
  initialText: String = "",
  initialType: String = "SINGLE_MCQ",
  initialOptions: List<String> = listOf("", "", "", ""),
  initialCorrectIndex: Int = 0,
  initialExplanation: String = ""
) {
  var questionText by mutableStateOf(initialText)
  var questionType by mutableStateOf(initialType)
  val options = mutableStateListOf<String>().apply { addAll(initialOptions) }
  var correctIndex by mutableIntStateOf(initialCorrectIndex)
  var explanation by mutableStateOf(initialExplanation)
}

/**
 * Screen for users to create and publish interactive quizzes with inline form fields for questions,
 * multiple-choice options, and correct answer toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
  repository: QuizPollRepository,
  draftDao: DraftDao? = null,
  onNavigateBack: () -> Unit,
  onQuizPublished: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(QuizCategories.ALL.first()) }
  var categoryExpanded by remember { mutableStateOf(false) }
  var difficulty by remember { mutableStateOf("Medium") }
  var timeLimitSeconds by remember { mutableLongStateOf(60L) }
  var isPublic by remember { mutableStateOf(true) }

  // Initialized with one empty question form block ready for input
  val questions = remember {
    mutableStateListOf(
      EditableQuestionState(
        initialText = "",
        initialOptions = listOf("", "", "", ""),
        initialCorrectIndex = 0
      )
    )
  }

  var isPublishing by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Quiz,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Interactive Quiz", fontWeight = FontWeight.Bold)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("create_quiz_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          // Save Draft button
          IconButton(
            onClick = {
              if (title.isBlank()) {
                scope.launch { snackbarHostState.showSnackbar("Enter at least a quiz title to save draft") }
                return@IconButton
              }
              scope.launch {
                draftDao?.saveQuizDraft(
                  QuizDraftEntity(
                    title = title,
                    description = description,
                    category = selectedCategory,
                    difficulty = difficulty,
                    timeLimitSeconds = timeLimitSeconds,
                    questionsJson = "${questions.size} questions",
                    savedAtEpochMs = System.currentTimeMillis()
                  )
                )
                snackbarHostState.showSnackbar("Quiz saved to local drafts")
              }
            },
            modifier = Modifier.testTag("save_quiz_draft_btn")
          ) {
            Icon(Icons.Default.Save, contentDescription = "Save Draft", tint = MaterialTheme.colorScheme.primary)
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (validationError != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_validation_error")
        ) {
          Text(
            text = validationError!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // 1. Quiz Metadata Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Quiz Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          OutlinedTextField(
            value = title,
            onValueChange = {
              if (it.length <= 100) title = it
              validationError = null
            },
            label = { Text("Quiz Title *") },
            placeholder = { Text("e.g. World History Trivia Challenge") },
            modifier = Modifier.fillMaxWidth().testTag("quiz_title_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
          )

          OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 300) description = it },
            label = { Text("Description") },
            placeholder = { Text("Brief description of what learners will test...") },
            modifier = Modifier.fillMaxWidth().testTag("quiz_description_input"),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
          )

          // Category Dropdown
          ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
          ) {
            OutlinedTextField(
              value = selectedCategory,
              onValueChange = {},
              readOnly = true,
              label = { Text("Subject Category") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("quiz_category_selector"),
              shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
              expanded = categoryExpanded,
              onDismissRequest = { categoryExpanded = false }
            ) {
              QuizCategories.ALL.forEach { cat ->
                DropdownMenuItem(
                  text = { Text(cat) },
                  onClick = {
                    selectedCategory = cat
                    categoryExpanded = false
                  }
                )
              }
            }
          }

          // Difficulty Selection Chips
          Column {
            Text("Difficulty", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("Easy", "Medium", "Hard").forEach { diff ->
                FilterChip(
                  selected = difficulty == diff,
                  onClick = { difficulty = diff },
                  label = { Text(diff) },
                  modifier = Modifier.testTag("quiz_difficulty_chip_$diff")
                )
              }
            }
          }

          // Time limit per question
          Column {
            Text("Time Limit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(0L to "None", 30L to "30s", 60L to "60s", 90L to "90s").forEach { (sec, label) ->
                FilterChip(
                  selected = timeLimitSeconds == sec,
                  onClick = { timeLimitSeconds = sec },
                  label = { Text(label) }
                )
              }
            }
          }

          // Public Visibility Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Public Visibility", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text("Visible to everyone in community social feed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = isPublic,
              onCheckedChange = { isPublic = it },
              modifier = Modifier.testTag("quiz_public_switch")
            )
          }
        }
      }

      // 2. Interactive Questions List Section Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Questions & Options (${questions.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Fill in questions, choices, and toggle the correct answer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        OutlinedButton(
          onClick = {
            questions.add(
              EditableQuestionState(
                initialText = "",
                initialOptions = listOf("", "", "", ""),
                initialCorrectIndex = 0
              )
            )
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("add_question_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Question")
        }
      }

      // 3. Question Form Cards with Option Fields & Correct Answer Toggles
      questions.forEachIndexed { qIndex, qState ->
        QuestionEditorCard(
          questionNumber = qIndex + 1,
          questionState = qState,
          canDelete = questions.size > 1,
          onDelete = { questions.removeAt(qIndex) },
          modifier = Modifier.testTag("question_editor_card_$qIndex")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 4. Publish Quiz Button
      Button(
        onClick = {
          val cleanTitle = title.trim()
          if (cleanTitle.isBlank()) {
            validationError = "Please enter a quiz title."
            return@Button
          }
          if (questions.isEmpty()) {
            validationError = "Please add at least one question."
            return@Button
          }

          val quizQuestions = mutableListOf<QuizQuestion>()
          for ((idx, q) in questions.withIndex()) {
            val qText = q.questionText.trim()
            if (qText.isBlank()) {
              validationError = "Question #${idx + 1} is empty. Please enter question text."
              return@Button
            }
            val validOptions = q.options.map { it.trim() }.filter { it.isNotBlank() }
            if (validOptions.size < 2) {
              validationError = "Question #${idx + 1} must have at least 2 non-blank options."
              return@Button
            }
            if (q.correctIndex !in validOptions.indices) {
              validationError = "Please select a valid correct answer toggle for Question #${idx + 1}."
              return@Button
            }
            quizQuestions.add(
              QuizQuestion(
                questionText = qText,
                questionType = q.questionType,
                options = validOptions,
                correctAnswers = listOf(q.correctIndex),
                explanation = q.explanation.trim(),
                orderIndex = idx
              )
            )
          }

          validationError = null
          scope.launch {
            isPublishing = true
            try {
              val quizId = repository.createQuiz(
                title = cleanTitle,
                description = description.trim(),
                category = selectedCategory,
                difficulty = difficulty,
                language = "English",
                timeLimitSeconds = timeLimitSeconds,
                isPublic = isPublic,
                questions = quizQuestions
              )
              isPublishing = false
              onQuizPublished(quizId)
            } catch (e: Exception) {
              isPublishing = false
              val msg = when {
                e.message?.contains("permission", ignoreCase = true) == true ->
                  "Unable to publish quiz: Please ensure your account is signed in and try again."
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("unavailable", ignoreCase = true) == true ->
                  "Network error: Unable to connect to server. Please check your internet connection."
                else -> e.localizedMessage ?: "Failed to publish quiz. Please try again."
              }
              validationError = msg
            }
          }
        },
        enabled = !isPublishing,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("publish_quiz_btn")
      ) {
        if (isPublishing) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Publishing to Community...")
        } else {
          Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Publish Quiz to Community",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

/**
 * Question editor card containing form fields for question text, multiple choice options,
 * and correct answer toggles for each option.
 */
@Composable
fun QuestionEditorCard(
  questionNumber: Int,
  questionState: EditableQuestionState,
  canDelete: Boolean,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val optionLetters = listOf("A", "B", "C", "D")

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Question Header with number badge, correct answer indicator, and delete action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "$questionNumber",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Question $questionNumber",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        // Active Correct Answer Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.testTag("correct_answer_indicator_${questionNumber - 1}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Correct: Option ${optionLetters.getOrElse(questionState.correctIndex) { "A" }}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        if (canDelete) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp).testTag("remove_question_btn_${questionNumber - 1}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Question",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }

      // Question Text Form Field
      OutlinedTextField(
        value = questionState.questionText,
        onValueChange = { questionState.questionText = it },
        label = { Text("Question Text *") },
        placeholder = { Text("e.g. Which planet has the highest density?") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("question_text_input_${questionNumber - 1}"),
        shape = RoundedCornerShape(12.dp),
        singleLine = false,
        maxLines = 3
      )

      // Multiple Choice Options Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Options (Select the Correct Answer):",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Tap radio button to set",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Option Form Fields with Correct Answer Toggle
      questionState.options.forEachIndexed { optIndex, optText ->
        val isCorrect = questionState.correctIndex == optIndex
        val letter = optionLetters.getOrElse(optIndex) { "${optIndex + 1}" }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isCorrect) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          border = BorderStroke(
            width = if (isCorrect) 1.5.dp else 1.dp,
            color = if (isCorrect) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("option_card_${questionNumber - 1}_$optIndex")
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Direct RadioButton for selecting the correct answer
              androidx.compose.material3.RadioButton(
                selected = isCorrect,
                onClick = { questionState.correctIndex = optIndex },
                modifier = Modifier.testTag("correct_answer_radio_${questionNumber - 1}_$optIndex")
              )

              Surface(
                shape = CircleShape,
                color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .clickable { questionState.correctIndex = optIndex }
                  .testTag("correct_answer_letter_${questionNumber - 1}_$optIndex")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = letter,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              OutlinedTextField(
                value = optText,
                onValueChange = { questionState.options[optIndex] = it },
                label = { Text("Option $letter ${if (optIndex < 2) "*" else ""}") },
                placeholder = { Text("Choice $letter") },
                modifier = Modifier
                  .weight(1f)
                  .testTag("option_text_input_${questionNumber - 1}_$optIndex"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Correct Answer Toggle Button & Status
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isCorrect) "✓ This option will be scored as correct" else "Mark option $letter as the correct answer",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 8.dp)
              )

              FilterChip(
                selected = isCorrect,
                onClick = { questionState.correctIndex = optIndex },
                leadingIcon = {
                  if (isCorrect) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                },
                label = {
                  Text(
                    text = if (isCorrect) "✓ Correct Answer" else "Set as Correct",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                  .testTag("correct_toggle_${questionNumber - 1}_$optIndex")
                  .testTag("correct_answer_toggle_${questionNumber - 1}_$optIndex")
              )
            }
          }
        }
      }

      // Explanation Form Field
      OutlinedTextField(
        value = questionState.explanation,
        onValueChange = { questionState.explanation = it },
        label = { Text("Answer Explanation (Optional)") },
        placeholder = { Text("Explain why this is correct after the user answers...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("question_explanation_input_${questionNumber - 1}"),
        shape = RoundedCornerShape(12.dp),
        maxLines = 2
      )
    }
  }
}

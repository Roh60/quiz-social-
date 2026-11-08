package com.example.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DraftDao
import com.example.data.local.PollDraftEntity
import com.example.data.model.QuizCategories
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
  repository: QuizPollRepository,
  draftDao: DraftDao? = null,
  onNavigateBack: () -> Unit,
  onPollPublished: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var question by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(QuizCategories.ALL.first()) }
  var categoryExpanded by remember { mutableStateOf(false) }

  var options by remember { mutableStateOf(listOf("", "")) }
  var isMultipleChoice by remember { mutableStateOf(false) }
  var allowVoteChange by remember { mutableStateOf(true) }
  var isAnonymous by remember { mutableStateOf(false) }
  var isPublic by remember { mutableStateOf(true) }
  var isPublishing by remember { mutableStateOf(false) }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Create Poll", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = {
              if (question.isBlank()) {
                scope.launch { snackbarHostState.showSnackbar("Enter a question to save draft") }
                return@IconButton
              }
              scope.launch {
                draftDao?.savePollDraft(
                  PollDraftEntity(
                    question = question,
                    description = description,
                    category = selectedCategory,
                    optionsCsv = options.joinToString("||"),
                    isMultipleChoice = isMultipleChoice,
                    allowVoteChange = allowVoteChange,
                    isAnonymous = isAnonymous,
                    savedAtEpochMs = System.currentTimeMillis()
                  )
                )
                snackbarHostState.showSnackbar("Poll draft saved locally")
              }
            },
            modifier = Modifier.testTag("save_poll_draft_btn")
          ) {
            Icon(Icons.Default.Save, contentDescription = "Save Draft")
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
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      OutlinedTextField(
        value = question,
        onValueChange = { if (it.length <= 150) question = it },
        label = { Text("Poll Question *") },
        placeholder = { Text("e.g. Which programming language is best for AI?") },
        modifier = Modifier.fillMaxWidth().testTag("poll_question_input"),
        maxLines = 3
      )

      OutlinedTextField(
        value = description,
        onValueChange = { if (it.length <= 300) description = it },
        label = { Text("Context / Notes (optional)") },
        placeholder = { Text("Provide background or guidelines for voters...") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
      )

      // Category
      ExposedDropdownMenuBox(
        expanded = categoryExpanded,
        onExpandedChange = { categoryExpanded = it }
      ) {
        OutlinedTextField(
          value = selectedCategory,
          onValueChange = {},
          readOnly = true,
          label = { Text("Category") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

      // Options
      Text(
        text = "Poll Choices (2 to 6 options):",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      options.forEachIndexed { index, opt ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = opt,
            onValueChange = { text ->
              val updated = options.toMutableList()
              updated[index] = text
              options = updated
            },
            label = { Text("Option ${index + 1} *") },
            modifier = Modifier.weight(1f).testTag("poll_option_input_$index"),
            singleLine = true
          )
          if (options.size > 2) {
            IconButton(
              onClick = {
                options = options.filterIndexed { i, _ -> i != index }
              }
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Option", tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      if (options.size < 6) {
        OutlinedButton(
          onClick = { options = options + "" },
          modifier = Modifier.testTag("add_poll_option_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Option")
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Option")
        }
      }

      // Toggles
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Multiple Choice Voting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text("Allow voters to pick more than one option", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isMultipleChoice, onCheckedChange = { isMultipleChoice = it })
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Allow Vote Changing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text("Members can change their vote before poll ends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = allowVoteChange, onCheckedChange = { allowVoteChange = it })
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Anonymous Poll", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text("Hide voter identities on results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          if (question.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Please enter a poll question.") }
            return@Button
          }
          val validOptions = options.map { it.trim() }.filter { it.isNotBlank() }
          if (validOptions.size < 2) {
            scope.launch { snackbarHostState.showSnackbar("Enter at least two distinct options.") }
            return@Button
          }
          isPublishing = true
          scope.launch {
            try {
              val pollId = repository.createPoll(
                question = question,
                description = description,
                category = selectedCategory,
                options = validOptions,
                isMultipleChoice = isMultipleChoice,
                allowVoteChange = allowVoteChange,
                isAnonymous = isAnonymous,
                isPublic = isPublic
              )
              isPublishing = false
              onPollPublished(pollId)
            } catch (e: Exception) {
              isPublishing = false
              snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to publish poll.")
            }
          }
        },
        enabled = !isPublishing,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("publish_poll_btn"),
        shape = RoundedCornerShape(16.dp)
      ) {
        if (isPublishing) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text("Publish Poll to Feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

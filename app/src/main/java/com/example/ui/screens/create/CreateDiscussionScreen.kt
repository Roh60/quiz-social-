package com.example.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.QuizCategories
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDiscussionScreen(
  repository: QuizPollRepository,
  onNavigateBack: () -> Unit,
  onDiscussionPublished: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(QuizCategories.ALL.first()) }
  var categoryExpanded by remember { mutableStateOf(false) }
  var isPublishing by remember { mutableStateOf(false) }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Start Discussion", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        value = title,
        onValueChange = { if (it.length <= 120) title = it },
        label = { Text("Discussion Topic / Question *") },
        placeholder = { Text("e.g. How does quantum entanglement relate to cryptography?") },
        modifier = Modifier.fillMaxWidth().testTag("discussion_title_input"),
        singleLine = true
      )

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

      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text("Discussion Content / Explanation *") },
        placeholder = { Text("Detail your question, concept, or ideas to invite insightful community discussion...") },
        modifier = Modifier.fillMaxWidth().height(200.dp).testTag("discussion_content_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          if (title.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Please enter a discussion title.") }
            return@Button
          }
          if (content.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Please provide the discussion content.") }
            return@Button
          }
          isPublishing = true
          scope.launch {
            try {
              val postId = repository.createDiscussionPost(
                title = title,
                content = content,
                category = selectedCategory
              )
              isPublishing = false
              onDiscussionPublished(postId)
            } catch (e: Exception) {
              isPublishing = false
              snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to post discussion.")
            }
          }
        },
        enabled = !isPublishing,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("publish_discussion_btn"),
        shape = RoundedCornerShape(16.dp)
      ) {
        if (isPublishing) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text("Post Discussion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

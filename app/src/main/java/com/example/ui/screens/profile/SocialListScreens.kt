package com.example.ui.screens.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DraftDao
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
  userId: String,
  repository: QuizPollRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Followers", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    EmptyStateView(
      title = "No Followers Yet",
      message = "Share quizzes and interesting polls to build your educational network!",
      icon = Icons.Default.Group,
      modifier = modifier.padding(innerPadding)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
  userId: String,
  repository: QuizPollRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Following", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    EmptyStateView(
      title = "Not Following Anyone Yet",
      message = "Explore top educators and creators to see their content in your feed.",
      icon = Icons.Default.Group,
      modifier = modifier.padding(innerPadding)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
  repository: QuizPollRepository,
  onNavigateBack: () -> Unit,
  onNavigateToPost: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val currentUserId = repository.getCurrentUserId() ?: ""
  val bookmarks by repository.observeUserBookmarks(currentUserId)
    .collectAsState(initial = emptyList())

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Saved Bookmarks (${bookmarks.size})", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    if (bookmarks.isEmpty()) {
      EmptyStateView(
        title = "No Saved Bookmarks",
        message = "Tap the bookmark icon on any quiz, poll, or discussion to revisit it here.",
        icon = Icons.Default.Bookmark,
        modifier = modifier.padding(innerPadding)
      )
    } else {
      LazyColumn(
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(bookmarks, key = { it.id }) { item ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onNavigateToPost(item.postId) }
              .testTag("saved_bookmark_${item.id}")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = item.postType.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                  onClick = {
                    scope.launch {
                      repository.toggleBookmark(
                        postId = item.postId,
                        postType = item.postType,
                        title = item.title,
                        currentlyBookmarked = true
                      )
                    }
                  },
                  modifier = Modifier.size(28.dp).testTag("delete_saved_${item.id}")
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Bookmark",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              if (item.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = item.content,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftsScreen(
  draftDao: DraftDao?,
  onNavigateBack: () -> Unit,
  onSelectQuizDraft: (Long) -> Unit,
  onSelectPollDraft: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val quizDrafts by draftDao?.observeQuizDrafts()?.collectAsState(initial = emptyList())
    ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
  val pollDrafts by draftDao?.observePollDrafts()?.collectAsState(initial = emptyList())
    ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Saved Drafts", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    if (quizDrafts.isEmpty() && pollDrafts.isEmpty()) {
      EmptyStateView(
        title = "No Drafts Saved",
        message = "When you start creating a quiz or poll, you can save unfinished drafts on this device.",
        icon = Icons.Default.Drafts,
        modifier = modifier.padding(innerPadding)
      )
    } else {
      LazyColumn(
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (quizDrafts.isNotEmpty()) {
          item {
            Text("Quiz Drafts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          items(quizDrafts) { draft ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectQuizDraft(draft.draftId) }
                .testTag("quiz_draft_card_${draft.draftId}")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = draft.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text(text = "${draft.category} • ${draft.difficulty}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                  onClick = {
                    scope.launch { draftDao?.deleteQuizDraft(draft.draftId) }
                  }
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Draft", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }

        if (pollDrafts.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text("Poll Drafts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          items(pollDrafts) { draft ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectPollDraft(draft.draftId) }
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = draft.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text(text = draft.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                  onClick = {
                    scope.launch { draftDao?.deletePollDraft(draft.draftId) }
                  }
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Draft", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }
  }
}

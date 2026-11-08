package com.example.ui.screens.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.Post
import com.example.data.model.QuizCategories
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.CategoryChip
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PostCard
import com.example.ui.components.SkeletonLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  initialCategory: String?,
  repository: QuizPollRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  onNavigateToPostDetail: (String) -> Unit,
  onNavigateToQuizPlayer: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var query by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf("All") } // "All", "Quizzes", "Polls", "Discussions"
  var selectedCategory by remember { mutableStateOf(initialCategory ?: "All") }

  var allPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    repository.observeFeed(tab = "Latest").collectLatest {
      allPosts = it
      isLoading = false
    }
  }

  // Filter posts in-memory based on search query, type, and category
  val filteredPosts = remember(query, selectedType, selectedCategory, allPosts) {
    allPosts.filter { post ->
      val matchesQuery = query.isBlank() ||
        post.title.contains(query, ignoreCase = true) ||
        post.content.contains(query, ignoreCase = true) ||
        post.authorName.contains(query, ignoreCase = true) ||
        post.authorUsername.contains(query, ignoreCase = true)

      val matchesType = when (selectedType) {
        "Quizzes" -> post.postType == "quiz"
        "Polls" -> post.postType == "poll"
        "Discussions" -> post.postType == "discussion"
        else -> true
      }

      val matchesCategory = selectedCategory == "All" || post.category.equals(selectedCategory, ignoreCase = true)

      matchesQuery && matchesType && matchesCategory
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Search Community", fontWeight = FontWeight.Bold) },
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
    ) {
      // Search Input
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search topics, questions, authors...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
          if (query.isNotBlank()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("search_text_input"),
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
      )

      // Post Type Filter Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("All", "Quizzes", "Polls", "Discussions").forEach { type ->
          FilterChip(
            selected = selectedType == type,
            onClick = { selectedType = type },
            label = { Text(type) }
          )
        }
      }

      // Category Filter Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 4.dp)
      ) {
        (listOf("All") + QuizCategories.ALL).forEach { cat ->
          CategoryChip(
            text = cat,
            isSelected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            modifier = Modifier.padding(end = 6.dp)
          )
        }
      }

      // Search Results
      if (isLoading) {
        SkeletonLoader(modifier = Modifier.fillMaxSize().padding(top = 8.dp))
      } else if (filteredPosts.isEmpty()) {
        EmptyStateView(
          title = "No Results Found",
          message = "Try searching for a different keyword, category, or author."
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(filteredPosts, key = { it.id }) { post ->
            PostCard(
              post = post,
              currentUserId = currentUserId,
              isLiked = false,
              isBookmarked = false,
              onLikeClick = {
                scope.launch { repository.toggleLike(post.id, false) }
              },
              onBookmarkClick = {
                scope.launch { repository.toggleBookmark(post.id, post.postType, post.title, false) }
              },
              onCommentClick = { onNavigateToPostDetail(post.id) },
              onShareClick = {},
              onPostClick = { onNavigateToPostDetail(post.id) },
              onStartQuizClick = { quizId -> onNavigateToQuizPlayer(quizId) },
              onReportClick = {},
              onBlockUserClick = {}
            )
          }
        }
      }
    }
  }
}

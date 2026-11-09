package com.example.ui.screens.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateMapOf
import com.example.data.model.Comment
import com.example.data.model.Quiz
import com.example.data.model.QuizCategories
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Screen that fetches and displays a list of available community quizzes from Firestore,
 * showing titles, categories, author names, questions count, difficulty, and playtime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizFeedScreen(
  repository: QuizPollRepository,
  onNavigateBack: (() -> Unit)? = null,
  onTakeQuiz: (quizId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var quizzes by remember { mutableStateOf<List<Quiz>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var isRefreshing by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf("All") }
  var searchQuery by remember { mutableStateOf("") }

  val categories = remember { listOf("All") + QuizCategories.ALL }

  // Map of quizId -> Comments list for real-time discussions
  val commentsMap = remember { mutableStateMapOf<String, List<Comment>>() }

  // Observe community quizzes from Firestore
  LaunchedEffect(selectedCategory) {
    isLoading = true
    val catFilter = if (selectedCategory == "All") null else selectedCategory
    repository.observeQuizzes(catFilter).collectLatest { list ->
      quizzes = list
      isLoading = false

      // Attach realtime comment stream for each quiz item
      list.forEach { q ->
        scope.launch {
          repository.observeTargetComments("quiz", q.id).collectLatest { comments ->
            commentsMap[q.id] = comments
          }
        }
      }
    }
  }

  // Filter quizzes based on category and title/author search query
  val filteredQuizzes = remember(quizzes, searchQuery) {
    if (searchQuery.isBlank()) {
      quizzes
    } else {
      val query = searchQuery.trim().lowercase()
      quizzes.filter { quiz ->
        quiz.title.lowercase().contains(query) ||
        quiz.description.lowercase().contains(query) ||
        quiz.category.lowercase().contains(query) ||
        quiz.authorName.lowercase().contains(query) ||
        quiz.authorUsername.lowercase().contains(query)
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Community Quizzes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("quiz_feed_back_btn")
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by quiz title, author, topic...") },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("quiz_feed_search_input")
      )

      // Horizontal Category Filter Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("quiz_feed_category_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        categories.forEach { cat ->
          val isSelected = selectedCategory == cat
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedCategory = cat }
              .testTag("quiz_feed_cat_$cat")
          ) {
            Text(
              text = cat,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }

      // Quiz List with Pull-To-Refresh
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
          scope.launch {
            isRefreshing = true
            val catFilter = if (selectedCategory == "All") null else selectedCategory
            repository.observeQuizzes(catFilter).collectLatest { list ->
              quizzes = list
              isRefreshing = false
            }
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("quiz_feed_pull_refresh")
      ) {
        if (isLoading && filteredQuizzes.isEmpty()) {
          QuizFeedSkeletonLoader(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp))
        } else if (filteredQuizzes.isEmpty()) {
          EmptyStateView(
            title = if (searchQuery.isNotBlank()) "No Quizzes Found" else "No Quizzes in $selectedCategory",
            message = if (searchQuery.isNotBlank()) {
              "No quizzes matched \"$searchQuery\". Try adjusting your search keywords."
            } else {
              "Be the first scholar in the community to create a quiz challenge for $selectedCategory!"
            },
            actionLabel = if (searchQuery.isNotBlank()) "Clear Filter" else "Reset Category",
            onActionClick = {
              searchQuery = ""
              selectedCategory = "All"
            }
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .testTag("quiz_feed_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${filteredQuizzes.size} Available Quizzes",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = selectedCategory,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            items(filteredQuizzes, key = { it.id }) { quiz ->
              val quizComments = commentsMap[quiz.id] ?: emptyList()
              CommunityQuizCard(
                quiz = quiz,
                comments = quizComments,
                onAddComment = { text ->
                  scope.launch {
                    try {
                      repository.addTargetComment("quiz", quiz.id, text)
                    } catch (_: Exception) {}
                  }
                },
                onPlayClick = { onTakeQuiz(quiz.id) }
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Card component displaying a community quiz challenge with title, category, author info,
 * difficulty badge, and question counts.
 */
@Composable
fun CommunityQuizCard(
  quiz: Quiz,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
  comments: List<Comment> = emptyList(),
  onAddComment: ((String) -> Unit)? = null
) {
  val authorDisplayName = quiz.authorName.ifBlank { quiz.authorUsername.ifBlank { "Quiz Scholar" } }
  var showComments by remember { mutableStateOf(false) }
  var newCommentText by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("community_quiz_card_${quiz.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Category & Difficulty Row
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.HelpOutline,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = quiz.category.ifBlank { "General Knowledge" },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when (quiz.difficulty.lowercase()) {
            "easy" -> MaterialTheme.colorScheme.secondaryContainer
            "hard" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.tertiaryContainer
          }
        ) {
          Text(
            text = quiz.difficulty.ifBlank { "Medium" }.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when (quiz.difficulty.lowercase()) {
              "easy" -> MaterialTheme.colorScheme.onSecondaryContainer
              "hard" -> MaterialTheme.colorScheme.onErrorContainer
              else -> MaterialTheme.colorScheme.onTertiaryContainer
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Quiz Title
      Text(
        text = quiz.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("quiz_card_title_${quiz.id}")
      )

      if (quiz.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = quiz.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Author Details & Play Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          UserAvatar(name = authorDisplayName, size = 32)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = authorDisplayName,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("quiz_card_author_${quiz.id}")
            )
            if (quiz.authorUsername.isNotBlank()) {
              Text(
                text = "@${quiz.authorUsername}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (quiz.timeLimitSeconds > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "${quiz.timeLimitSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Discussion toggle button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (showComments) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { showComments = !showComments }
              .testTag("quiz_card_comment_btn_${quiz.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = "Discuss Quiz",
                tint = if (showComments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${comments.size}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (showComments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Button(
            onClick = onPlayClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.testTag("play_quiz_btn_${quiz.id}")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Inline Comments & Community Discussion Section
      if (showComments) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth().testTag("quiz_comments_section_${quiz.id}")
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Discussion & Strategy (${comments.size})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (comments.isEmpty()) {
              Text(
                text = "No comments yet. Share your thoughts or quiz strategy below!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comments.takeLast(4).forEach { c ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                  ) {
                    UserAvatar(name = c.authorName.ifBlank { "Scholar" }, size = 26)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = c.authorName.ifBlank { "Quiz Scholar" },
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        if (c.authorUsername.isNotBlank()) {
                          Text(
                            text = "@${c.authorUsername}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = c.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post New Comment Input Row
            if (onAddComment != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = newCommentText,
                  onValueChange = { newCommentText = it },
                  placeholder = { Text("Write a comment or tip...", style = MaterialTheme.typography.bodySmall) },
                  shape = RoundedCornerShape(20.dp),
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("quiz_comment_input_${quiz.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    if (newCommentText.isNotBlank()) {
                      onAddComment(newCommentText.trim())
                      newCommentText = ""
                    }
                  },
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("quiz_comment_send_${quiz.id}")
                ) {
                  Icon(
                    Icons.Default.Send,
                    contentDescription = "Post comment",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Creates an animated linear shimmer brush for skeleton layouts.
 */
@Composable
fun rememberQuizFeedShimmerBrush(): Brush {
  val transition = rememberInfiniteTransition(label = "QuizFeedShimmer")
  val translateAnim = transition.animateFloat(
    initialValue = 0f,
    targetValue = 1200f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "QuizFeedShimmerTranslate"
  )

  val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
  val baseColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
  val highlightColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFF2F2F7)

  return Brush.linearGradient(
    colors = listOf(baseColor, highlightColor, baseColor),
    start = Offset(translateAnim.value - 300f, translateAnim.value - 300f),
    end = Offset(translateAnim.value, translateAnim.value)
  )
}

/**
 * Professional Skeleton Loader with shimmer animations matching the CommunityQuizCard layout.
 */
@Composable
fun QuizFeedSkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberQuizFeedShimmerBrush()
  Column(
    modifier = modifier.testTag("quiz_feed_skeleton_loader"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    repeat(4) { idx ->
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quiz_skeleton_card_$idx")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          // Category and Difficulty tags row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(width = 110.dp, height = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
            )
            Box(
              modifier = Modifier
                .size(width = 65.dp, height = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Title shimmer lines
          Box(
            modifier = Modifier
              .fillMaxWidth(0.85f)
              .height(20.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(shimmerBrush)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(0.55f)
              .height(16.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Description line
          Box(
            modifier = Modifier
              .fillMaxWidth(0.95f)
              .height(12.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Author and Play button row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(shimmerBrush)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Box(
                  modifier = Modifier
                    .size(width = 90.dp, height = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                  modifier = Modifier
                    .size(width = 60.dp, height = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
                )
              }
            }

            Box(
              modifier = Modifier
                .size(width = 76.dp, height = 32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
            )
          }
        }
      }
    }
  }
}


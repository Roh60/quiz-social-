package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Comment
import com.example.data.model.Poll
import com.example.data.model.Quiz
import com.example.data.model.QuizCategories
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Vertical feed displaying interactive quizzes fetched directly from Firestore,
 * equipped with like/reaction buttons, real-time poll results visualizer (pie/bar),
 * swipe-to-refresh mechanism, and interactive comment sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizFeed(
  repository: QuizPollRepository,
  onTakeQuiz: (quizId: String) -> Unit,
  modifier: Modifier = Modifier,
  initialCategory: String = "All",
  onShareQuiz: ((Quiz) -> Unit)? = null
) {
  val scope = rememberCoroutineScope()
  var selectedCategory by remember { mutableStateOf(initialCategory) }
  var quizzes by remember { mutableStateOf<List<Quiz>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var isRefreshing by remember { mutableStateOf(false) }

  // Map of quizId -> Comments list for real-time discussions
  val commentsMap = remember { mutableStateMapOf<String, List<Comment>>() }
  // Map of quizId -> Like status
  val likedQuizzesMap = remember { mutableStateMapOf<String, Boolean>() }
  // Map of quizId -> Like Count
  val likesCountMap = remember { mutableStateMapOf<String, Int>() }
  // Map of quizId -> Topic Poll for real-time visual charts
  val topicPollsMap = remember { mutableStateMapOf<String, Poll>() }

  val categories = remember { listOf("All") + QuizCategories.ALL }

  // Observe quizzes for current category
  LaunchedEffect(selectedCategory) {
    isLoading = true
    repository.observeQuizzes(if (selectedCategory == "All") null else selectedCategory)
      .collectLatest { fetched ->
        quizzes = fetched
        isLoading = false

        // Attach listeners for comments, likes, and topic polls
        fetched.forEach { quiz ->
          likesCountMap[quiz.id] = quiz.likeCount.toInt().coerceAtLeast(0)

          scope.launch {
            repository.observeCommentsForQuiz(quiz.id).collectLatest { commentList ->
              commentsMap[quiz.id] = commentList
            }
          }

          scope.launch {
            repository.observeQuizIsLiked(quiz.id).collectLatest { isLiked ->
              likedQuizzesMap[quiz.id] = isLiked
            }
          }

          // Fetch topic poll for live pie/bar visualization
          scope.launch {
            repository.observeFeed(categoryFilter = quiz.category).collectLatest { feedPosts ->
              val pollPost = feedPosts.firstOrNull { it.postType == "poll" && it.pollId.isNotBlank() }
              if (pollPost != null) {
                repository.observePoll(pollPost.pollId).collectLatest { livePoll ->
                  if (livePoll != null) {
                    topicPollsMap[quiz.id] = livePoll
                  }
                }
              } else {
                // Topic default poll if none in feed
                topicPollsMap[quiz.id] = Poll(
                  id = "poll_${quiz.id}",
                  question = "What is your mastery level on ${quiz.title}?",
                  category = quiz.category,
                  options = listOf("Complete Beginner", "Intermediate Scholar", "Advanced Expert", "Master"),
                  optionVotes = listOf(14L, 28L, 19L, 9L),
                  totalVotes = 70L
                )
              }
            }
          }
        }
      }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("quiz_feed_container")
  ) {
    // 1. Topic / Category Filter Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      categories.forEach { category ->
        val isSelected = category.equals(selectedCategory, ignoreCase = true)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
          border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { selectedCategory = category }
            .testTag("quiz_feed_category_chip_$category")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = category,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 2. Feed Counter Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (selectedCategory == "All") "Interactive Quizzes" else "$selectedCategory Quizzes",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "${quizzes.size} Available",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 3. Quiz List with Swipe-to-Refresh Mechanism
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        scope.launch {
          isRefreshing = true
          try {
            repository.seedStarterContentIfEmpty()
          } catch (_: Exception) {}
          delay(650)
          isRefreshing = false
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .testTag("quiz_feed_pull_refresh")
    ) {
      if (isLoading && quizzes.isEmpty()) {
        SkeletonLoader(modifier = Modifier.testTag("quiz_feed_loading"))
      } else if (quizzes.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("quiz_feed_empty_state"),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              modifier = Modifier.size(72.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Quiz,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(36.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "No Quizzes Found in $selectedCategory",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Try picking another topic or pull down to refresh!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("quiz_feed_list")
        ) {
          items(quizzes, key = { it.id }) { quiz ->
            val quizComments = commentsMap[quiz.id] ?: emptyList()
            val isLiked = likedQuizzesMap[quiz.id] ?: false
            val currentLikes = likesCountMap[quiz.id] ?: quiz.likeCount.toInt()
            val topicPoll = topicPollsMap[quiz.id]

            QuizFeedCard(
              quiz = quiz,
              comments = quizComments,
              isLiked = isLiked,
              likesCount = currentLikes,
              topicPoll = topicPoll,
              repository = repository,
              onTakeQuiz = { onTakeQuiz(quiz.id) },
              onShare = { onShareQuiz?.invoke(quiz) },
              onLikeToggle = {
                val newLiked = !isLiked
                likedQuizzesMap[quiz.id] = newLiked
                likesCountMap[quiz.id] = if (newLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
                scope.launch {
                  try {
                    repository.toggleQuizLike(quiz, isLiked)
                  } catch (_: Exception) {}
                }
              },
              onAddComment = { text ->
                scope.launch {
                  try {
                    repository.addCommentForQuiz(quiz, text)
                  } catch (_: Exception) {}
                }
              }
            )
          }
          item {
            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }
    }
  }
}

/**
 * Card displaying an individual interactive quiz with author, difficulty, question count,
 * 'Take Quiz' action, Like/Reaction button, Poll Result Chart toggle, and expandable comments.
 */
@Composable
fun QuizFeedCard(
  quiz: Quiz,
  comments: List<Comment>,
  isLiked: Boolean,
  likesCount: Int,
  topicPoll: Poll?,
  onTakeQuiz: () -> Unit,
  modifier: Modifier = Modifier,
  repository: QuizPollRepository? = null,
  onShare: (() -> Unit)? = null,
  onLikeToggle: () -> Unit = {},
  onAddComment: (String) -> Unit = {}
) {
  var isCommentsExpanded by remember { mutableStateOf(false) }
  var isPollChartExpanded by remember { mutableStateOf(false) }
  var isLeaderboardExpanded by remember { mutableStateOf(false) }
  var selectedReaction by remember { mutableStateOf<String?>(null) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("quiz_feed_card_${quiz.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      // 1. Author & Topic Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          UserAvatar(name = quiz.authorName.ifBlank { quiz.authorUsername }, size = 38)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = quiz.authorName.ifBlank { "Community Educator" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("quiz_card_author_${quiz.id}")
            )
            Text(
              text = "@${quiz.authorUsername.ifBlank { "user" }}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = quiz.category.ifBlank { "General" },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Title & Description
      Text(
        text = quiz.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("quiz_card_title_${quiz.id}")
      )

      if (quiz.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = quiz.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Metadata Chips (Difficulty, Question count, Timer)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${quiz.questionCount.coerceAtLeast(1)} Questions",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when (quiz.difficulty.lowercase()) {
            "easy" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            "hard" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
          }
        ) {
          Text(
            text = quiz.difficulty.ifBlank { "Medium" },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = when (quiz.difficulty.lowercase()) {
              "easy" -> MaterialTheme.colorScheme.onSecondaryContainer
              "hard" -> MaterialTheme.colorScheme.onErrorContainer
              else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
          )
        }

        if (quiz.timeLimitSeconds > 0) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${quiz.timeLimitSeconds}s Limit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 4. Action Row with 'Like' Reaction Button, Feedback, Chart Toggle, and 'Take Quiz'
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Like / Reaction Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isLiked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              1.dp,
              if (isLiked) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { onLikeToggle() }
              .testTag("quiz_like_btn_${quiz.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like Quiz",
                tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "$likesCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                color = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("quiz_likes_count_${quiz.id}")
              )
            }
          }

          // Toggle Discussion / Comments Button with badge
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCommentsExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (isCommentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { isCommentsExpanded = !isCommentsExpanded }
              .testTag("quiz_card_comments_btn_${quiz.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Discuss Quiz",
                tint = if (isCommentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${comments.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isCommentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Poll Results Visualizer Toggle Button (Pie Chart / Bar Graph)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isPollChartExpanded) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (isPollChartExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { isPollChartExpanded = !isPollChartExpanded }
              .testTag("quiz_poll_chart_btn_${quiz.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "Poll Results Chart",
                tint = if (isPollChartExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Chart",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPollChartExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Toggle Individual Quiz Competition Leaderboard Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isLeaderboardExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (isLeaderboardExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { isLeaderboardExpanded = !isLeaderboardExpanded }
              .testTag("quiz_card_leaderboard_btn_${quiz.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Quiz Leaderboard",
                tint = if (isLeaderboardExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Ranks",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isLeaderboardExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onShare != null) {
            IconButton(
              onClick = onShare,
              modifier = Modifier
                .size(40.dp)
                .testTag("quiz_card_share_btn_${quiz.id}")
            ) {
              Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(2.dp))
          }

          Button(
            onClick = onTakeQuiz,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("take_quiz_btn_${quiz.id}")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Take Quiz", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      // 5. Expandable Real-time Quiz Competition Leaderboard
      AnimatedVisibility(
        visible = isLeaderboardExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("quiz_leaderboard_section_${quiz.id}")
        ) {
          if (repository != null) {
            IndividualQuizLeaderboardView(
              quizId = quiz.id,
              repository = repository,
              currentUserId = repository.getCurrentUserId() ?: ""
            )
          }
        }
      }

      // 6. Expandable Real-time Poll Visual Chart (Pie Chart / Bar Graph)
      AnimatedVisibility(
        visible = isPollChartExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("quiz_poll_chart_section_${quiz.id}")
        ) {
          if (topicPoll != null) {
            PollResultChart(poll = topicPoll)
          } else {
            Text(
              text = "Loading community poll insights...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // 6. Expandable Comment & Feedback Section
      AnimatedVisibility(
        visible = isCommentsExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("quiz_comments_section_${quiz.id}")
        ) {
          QuizCommentsFeedbackView(
            quizId = quiz.id,
            comments = comments,
            onAddComment = onAddComment
          )
        }
      }
    }
  }
}

/**
 * Dedicated comment section inside QuizFeedCard allowing users to discuss the quiz,
 * share insights, and submit feedback.
 */
@Composable
fun QuizCommentsFeedbackView(
  quizId: String,
  comments: List<Comment>,
  onAddComment: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var commentInput by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Quiz Discussion & Feedback",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
          Text(
            text = "${comments.size} comments",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Comments List or Empty State
      if (comments.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_comments_empty_$quizId")
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "No feedback or discussion yet.",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Be the first to share your thoughts, tips, or question insights!",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_comments_list_$quizId"),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          comments.forEach { comment ->
            QuizCommentItem(comment = comment)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Comment input field with send button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = commentInput,
          onValueChange = { commentInput = it },
          placeholder = {
            Text(
              text = "Share feedback or discuss questions...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("quiz_comment_input_$quizId"),
          shape = RoundedCornerShape(16.dp),
          singleLine = true,
          enabled = !isSubmitting,
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
          )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            val text = commentInput.trim()
            if (text.isNotBlank() && !isSubmitting) {
              isSubmitting = true
              onAddComment(text)
              commentInput = ""
              isSubmitting = false
            }
          },
          enabled = commentInput.isNotBlank() && !isSubmitting,
          modifier = Modifier
            .size(44.dp)
            .background(
              color = if (commentInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = CircleShape
            )
            .testTag("quiz_comment_send_btn_$quizId")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
          } else {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = "Post Comment",
              tint = if (commentInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Individual comment item inside QuizCommentsFeedbackView.
 */
@Composable
fun QuizCommentItem(
  comment: Comment,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("quiz_comment_item_${comment.id}")
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.Top
    ) {
      UserAvatar(name = comment.authorName.ifBlank { comment.authorUsername }, size = 30)
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = comment.authorName.ifBlank { "Community Member" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "@${comment.authorUsername.ifBlank { "user" }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = comment.content,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("quiz_comment_text_${comment.id}")
        )
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardSortOption
import com.example.data.model.LeaderboardTimeframe
import com.example.data.model.LeaderboardUser
import com.example.data.model.Post
import com.example.data.model.QuizLeaderboardEntry
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.flow.collectLatest

/**
 * Enhanced Leaderboard Component for Quiz Competitions.
 * Supports:
 * - General quiz performance (Weekly top scores vs All-Time)
 * - Individual quiz leaderboards with selectable quiz tabs
 * - Sortable by Score, Accuracy, and Time Taken
 * - Top 3 Podium highlight and ranked list
 */
@Composable
fun LeaderboardView(
  repository: QuizPollRepository,
  modifier: Modifier = Modifier,
  currentUserId: String = "",
  initialQuizId: String? = null,
  onNavigateToQuizPlayer: ((String) -> Unit)? = null
) {
  var selectedScopeTab by remember { mutableIntStateOf(if (initialQuizId != null) 1 else 0) } // 0: General, 1: Individual Quizzes
  var selectedTimeframe by remember { mutableStateOf(LeaderboardTimeframe.THIS_WEEK) }
  var selectedSortOption by remember { mutableStateOf(LeaderboardSortOption.SCORE) }

  // General Leaderboard State
  var generalUsers by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }
  var isGeneralLoading by remember { mutableStateOf(true) }

  // Individual Quizzes State
  var availableQuizzes by remember { mutableStateOf<List<Post>>(emptyList()) }
  var activeQuizId by remember { mutableStateOf(initialQuizId ?: "") }
  var activeQuizTitle by remember { mutableStateOf("Community Quiz") }
  var quizEntries by remember { mutableStateOf<List<QuizLeaderboardEntry>>(emptyList()) }
  var isQuizLoading by remember { mutableStateOf(false) }

  // 1. Observe general competition leaderboard
  LaunchedEffect(selectedTimeframe, selectedSortOption) {
    isGeneralLoading = true
    repository.observeCompetitionLeaderboard(
      timeframe = selectedTimeframe,
      sortBy = selectedSortOption,
      limit = 20
    ).collectLatest { users ->
      generalUsers = users
      isGeneralLoading = false
    }
  }

  // 2. Fetch quizzes for individual quiz switcher
  LaunchedEffect(Unit) {
    repository.observeFeed("quizzes").collectLatest { posts ->
      val quizzes = posts.filter { it.postType == "quiz" && it.quizId.isNotBlank() }
      availableQuizzes = quizzes
      if (activeQuizId.isBlank() && quizzes.isNotEmpty()) {
        activeQuizId = quizzes.first().quizId
        activeQuizTitle = quizzes.first().title
      }
    }
  }

  // 3. Observe active individual quiz leaderboard
  LaunchedEffect(activeQuizId, selectedSortOption) {
    if (activeQuizId.isNotBlank()) {
      isQuizLoading = true
      repository.observeQuizLeaderboard(
        quizId = activeQuizId,
        sortBy = selectedSortOption
      ).collectLatest { entries ->
        quizEntries = entries
        isQuizLoading = false
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("leaderboard_container")
  ) {
    // 1. Header Banner
    LeaderboardHeroBanner()

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Main Scope Tabs: General vs Individual Quizzes
    TabRow(
      selectedTabIndex = selectedScopeTab,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("leaderboard_scope_tabs"),
      containerColor = Color.Transparent
    ) {
      Tab(
        selected = selectedScopeTab == 0,
        onClick = { selectedScopeTab = 0 },
        modifier = Modifier
          .height(48.dp)
          .testTag("tab_general_leaderboard"),
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("General Competitions", fontWeight = if (selectedScopeTab == 0) FontWeight.Bold else FontWeight.Medium)
          }
        }
      )
      Tab(
        selected = selectedScopeTab == 1,
        onClick = { selectedScopeTab = 1 },
        modifier = Modifier
          .height(48.dp)
          .testTag("tab_individual_quizzes_leaderboard"),
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Individual Quizzes", fontWeight = if (selectedScopeTab == 1) FontWeight.Bold else FontWeight.Medium)
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Controls Row: Timeframe (for General) and Sort Options (Score, Accuracy, Time Taken)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Timeframe selector (only for General leaderboard)
      if (selectedScopeTab == 0) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Timeframe:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeaderboardTimeframe.values().forEach { timeframe ->
              val isSelected = selectedTimeframe == timeframe
              FilterChip(
                selected = isSelected,
                onClick = { selectedTimeframe = timeframe },
                label = {
                  Text(
                    text = if (timeframe == LeaderboardTimeframe.THIS_WEEK) "This Week" else "All-Time",
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                leadingIcon = {
                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                  } else {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                  .height(36.dp)
                  .testTag(if (timeframe == LeaderboardTimeframe.THIS_WEEK) "leaderboard_timeframe_this_week" else "leaderboard_timeframe_all_time")
              )
            }
          }
        }
      } else {
        // Individual Quiz Selector Chips
        if (availableQuizzes.isNotEmpty()) {
          Text(
            text = "Select Competition Quiz:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.testTag("quiz_leaderboard_selector_row")
          ) {
            items(availableQuizzes, key = { it.quizId }) { post ->
              val isSelected = activeQuizId == post.quizId
              FilterChip(
                selected = isSelected,
                onClick = {
                  activeQuizId = post.quizId
                  activeQuizTitle = post.title
                },
                label = {
                  Text(
                    text = post.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                  .height(36.dp)
                  .testTag("quiz_chip_${post.quizId}")
              )
            }
          }
        }
      }

      // Sort criteria selector (Score, Accuracy, Time Taken)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Sort By:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          LeaderboardSortOption.values().forEach { sortOption ->
            val isSelected = selectedSortOption == sortOption
            val icon = when (sortOption) {
              LeaderboardSortOption.SCORE -> Icons.Default.EmojiEvents
              LeaderboardSortOption.ACCURACY -> Icons.Default.Percent
              LeaderboardSortOption.TIME_TAKEN -> Icons.Default.Timer
            }
            FilterChip(
              selected = isSelected,
              onClick = { selectedSortOption = sortOption },
              label = {
                Text(
                  text = sortOption.label,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
              ),
              modifier = Modifier
                .height(36.dp)
                .testTag(
                  when (sortOption) {
                    LeaderboardSortOption.SCORE -> "leaderboard_sort_score"
                    LeaderboardSortOption.ACCURACY -> "leaderboard_sort_accuracy"
                    LeaderboardSortOption.TIME_TAKEN -> "leaderboard_sort_time"
                  }
                )
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Content Area
    if (selectedScopeTab == 0) {
      // General Competition Leaderboard List
      if (isGeneralLoading && generalUsers.isEmpty()) {
        LeaderboardSkeletonLoader(modifier = Modifier.testTag("leaderboard_loading"))
      } else if (generalUsers.isEmpty()) {
        EmptyStateView(
          title = "No Competition Scores Yet",
          message = "Take quizzes and participate in competitions to rank on the weekly leaderboard!",
          modifier = Modifier.testTag("leaderboard_empty")
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
            .testTag("leaderboard_list"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Top 3 Podium
          if (generalUsers.size >= 3) {
            item(key = "general_podium") {
              GeneralPodiumSection(
                topUsers = generalUsers.take(3),
                timeframe = selectedTimeframe,
                sortBy = selectedSortOption,
                currentUserId = currentUserId
              )
              Spacer(modifier = Modifier.height(12.dp))
            }
          }

          // Remaining ranked list
          val remainingUsers = if (generalUsers.size >= 3) generalUsers.drop(3) else generalUsers
          items(remainingUsers, key = { it.userId }) { user ->
            GeneralLeaderboardUserCard(
              user = user,
              timeframe = selectedTimeframe,
              sortBy = selectedSortOption,
              isCurrentUser = user.userId == currentUserId
            )
          }

          item {
            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }
    } else {
      // Individual Quiz Leaderboard List
      if (isQuizLoading && quizEntries.isEmpty()) {
        LeaderboardSkeletonLoader(modifier = Modifier.testTag("quiz_leaderboard_loading"))
      } else if (quizEntries.isEmpty()) {
        EmptyStateView(
          title = "No Quiz Competitors Yet",
          message = "Be the first scholar to complete '$activeQuizTitle' and claim #1!",
          modifier = Modifier.testTag("quiz_leaderboard_empty")
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
            .testTag("individual_quiz_leaderboard_list"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Header with Quiz Title & Take Quiz button
          item(key = "quiz_header") {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = activeQuizTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${quizEntries.size} competitors ranked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (onNavigateToQuizPlayer != null && activeQuizId.isNotBlank()) {
                  Button(
                    onClick = { onNavigateToQuizPlayer(activeQuizId) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("take_competition_quiz_btn")
                  ) {
                    Text("Take Quiz")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }

          // Top 3 Podium for Quiz
          if (quizEntries.size >= 3) {
            item(key = "quiz_podium") {
              QuizPodiumSection(
                topEntries = quizEntries.take(3),
                sortBy = selectedSortOption,
                currentUserId = currentUserId
              )
              Spacer(modifier = Modifier.height(10.dp))
            }
          }

          val remainingQuizEntries = if (quizEntries.size >= 3) quizEntries.drop(3) else quizEntries
          items(remainingQuizEntries, key = { it.userId }) { entry ->
            IndividualQuizLeaderboardCard(
              entry = entry,
              sortBy = selectedSortOption,
              isCurrentUser = entry.userId == currentUserId
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
 * Top 3 Podium for General Competitions.
 */
@Composable
private fun GeneralPodiumSection(
  topUsers: List<LeaderboardUser>,
  timeframe: LeaderboardTimeframe,
  sortBy: LeaderboardSortOption,
  currentUserId: String
) {
  val first = topUsers.getOrNull(0)
  val second = topUsers.getOrNull(1)
  val third = topUsers.getOrNull(2)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("leaderboard_podium"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
      ) {
        // 2nd Place (Silver - Left)
        if (second != null) {
          PodiumColumn(
            user = second,
            rank = 2,
            pedestalHeight = 90.dp,
            badgeColor = Color(0xFF64748B),
            medalBorder = Color(0xFF94A3B8),
            sortBy = sortBy,
            timeframe = timeframe,
            isCurrentUser = second.userId == currentUserId
          )
        }

        // 1st Place (Gold - Center)
        if (first != null) {
          PodiumColumn(
            user = first,
            rank = 1,
            pedestalHeight = 118.dp,
            badgeColor = Color(0xFFD97706),
            medalBorder = Color(0xFFF59E0B),
            sortBy = sortBy,
            timeframe = timeframe,
            isCurrentUser = first.userId == currentUserId
          )
        }

        // 3rd Place (Bronze - Right)
        if (third != null) {
          PodiumColumn(
            user = third,
            rank = 3,
            pedestalHeight = 75.dp,
            badgeColor = Color(0xFFB45309),
            medalBorder = Color(0xFFD97706),
            sortBy = sortBy,
            timeframe = timeframe,
            isCurrentUser = third.userId == currentUserId
          )
        }
      }
    }
  }
}

/**
 * Single Column on Podium for General user.
 */
@Composable
private fun PodiumColumn(
  user: LeaderboardUser,
  rank: Int,
  pedestalHeight: androidx.compose.ui.unit.Dp,
  badgeColor: Color,
  medalBorder: Color,
  sortBy: LeaderboardSortOption,
  timeframe: LeaderboardTimeframe,
  isCurrentUser: Boolean
) {
  val displayedScore = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyScore else user.totalScore
  val displayedAccuracy = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyAccuracy else user.accuracy
  val displayedTime = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyTimeTakenSeconds else user.timeTakenSeconds

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(100.dp)
      .testTag("podium_rank_$rank")
  ) {
    // Medal Icon / Crown for 1st
    if (rank == 1) {
      Icon(
        Icons.Default.EmojiEvents,
        contentDescription = "Gold Crown",
        tint = Color(0xFFD97706),
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
    }

    // Avatar with medal border
    Surface(
      shape = CircleShape,
      border = BorderStroke(2.dp, medalBorder),
      modifier = Modifier.size(if (rank == 1) 54.dp else 46.dp)
    ) {
      UserAvatar(name = user.displayName, size = if (rank == 1) 54 else 46)
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = user.displayName,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )

    // Key metric highlight
    Text(
      text = when (sortBy) {
        LeaderboardSortOption.SCORE -> "$displayedScore pts"
        LeaderboardSortOption.ACCURACY -> "${displayedAccuracy.toInt()}%"
        LeaderboardSortOption.TIME_TAKEN -> "${displayedTime}s"
      },
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Pedestal Block
    Surface(
      shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
      color = when (rank) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFF1F5F9)
        else -> Color(0xFFFFEDD5)
      },
      border = BorderStroke(1.dp, medalBorder.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .height(pedestalHeight)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "#$rank",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black,
          color = badgeColor
        )
        if (isCurrentUser) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Text(
              text = "YOU",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Top 3 Podium for an Individual Quiz.
 */
@Composable
private fun QuizPodiumSection(
  topEntries: List<QuizLeaderboardEntry>,
  sortBy: LeaderboardSortOption,
  currentUserId: String
) {
  val first = topEntries.getOrNull(0)
  val second = topEntries.getOrNull(1)
  val third = topEntries.getOrNull(2)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("quiz_podium"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.Bottom
    ) {
      if (second != null) {
        QuizPodiumColumn(second, 2, 85.dp, Color(0xFF64748B), Color(0xFF94A3B8), sortBy, second.userId == currentUserId)
      }
      if (first != null) {
        QuizPodiumColumn(first, 1, 110.dp, Color(0xFFD97706), Color(0xFFF59E0B), sortBy, first.userId == currentUserId)
      }
      if (third != null) {
        QuizPodiumColumn(third, 3, 70.dp, Color(0xFFB45309), Color(0xFFD97706), sortBy, third.userId == currentUserId)
      }
    }
  }
}

@Composable
private fun QuizPodiumColumn(
  entry: QuizLeaderboardEntry,
  rank: Int,
  pedestalHeight: androidx.compose.ui.unit.Dp,
  badgeColor: Color,
  medalBorder: Color,
  sortBy: LeaderboardSortOption,
  isCurrentUser: Boolean
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(96.dp)
  ) {
    if (rank == 1) {
      Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
    }
    Surface(
      shape = CircleShape,
      border = BorderStroke(2.dp, medalBorder),
      modifier = Modifier.size(if (rank == 1) 50.dp else 42.dp)
    ) {
      UserAvatar(name = entry.displayName, size = if (rank == 1) 50 else 42)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = entry.displayName,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = when (sortBy) {
        LeaderboardSortOption.SCORE -> "${entry.score} pts"
        LeaderboardSortOption.ACCURACY -> "${entry.accuracy.toInt()}%"
        LeaderboardSortOption.TIME_TAKEN -> "${entry.timeTakenSeconds}s"
      },
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Surface(
      shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
      color = when (rank) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFF1F5F9)
        else -> Color(0xFFFFEDD5)
      },
      modifier = Modifier.fillMaxWidth().height(pedestalHeight)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(text = "#$rank", fontWeight = FontWeight.Black, color = badgeColor)
      }
    }
  }
}

/**
 * Individual ranked user card with dynamic rank, badge title, and sort-specific highlights.
 */
@Composable
fun GeneralLeaderboardUserCard(
  user: LeaderboardUser,
  timeframe: LeaderboardTimeframe,
  sortBy: LeaderboardSortOption,
  isCurrentUser: Boolean,
  modifier: Modifier = Modifier
) {
  val (rankColor, rankBg, rankBorder) = when (user.rank) {
    1 -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFFF59E0B)) // Gold
    2 -> Triple(Color(0xFF64748B), Color(0xFFF1F5F9), Color(0xFF94A3B8)) // Silver
    3 -> Triple(Color(0xFFB45309), Color(0xFFFFEDD5), Color(0xFFD97706)) // Bronze
    else -> Triple(
      MaterialTheme.colorScheme.onSurfaceVariant,
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      Color.Transparent
    )
  }

  val displayedScore = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyScore else user.totalScore
  val displayedAccuracy = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyAccuracy else user.accuracy
  val displayedTime = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyTimeTakenSeconds else user.timeTakenSeconds
  val displayedQuizzes = if (timeframe == LeaderboardTimeframe.THIS_WEEK) user.weeklyQuizzesCompleted else user.quizzesCompleted

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("leaderboard_user_card_${user.rank}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                       else MaterialTheme.colorScheme.surface
    ),
    border = if (isCurrentUser) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
             else if (user.rank <= 3) BorderStroke(1.dp, rankBorder)
             else null,
    elevation = CardDefaults.cardElevation(defaultElevation = if (user.rank <= 3) 2.dp else 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Rank Badge
      Surface(
        shape = CircleShape,
        color = rankBg,
        border = BorderStroke(1.dp, rankBorder),
        modifier = Modifier
          .size(38.dp)
          .testTag("leaderboard_rank_${user.rank}")
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (user.rank == 1) {
            Icon(Icons.Default.EmojiEvents, contentDescription = "#1", tint = rankColor, modifier = Modifier.size(20.dp))
          } else {
            Text(
              text = "#${user.rank}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = rankColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // 2. Avatar
      UserAvatar(name = user.displayName, size = 44)

      Spacer(modifier = Modifier.width(12.dp))

      // 3. User Details
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("leaderboard_user_name_${user.rank}")
          )
          if (isCurrentUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.primary
            ) {
              Text(
                text = "YOU",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
          ) {
            Text(
              text = user.badgeTitle,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Metrics breakdown
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "${displayedQuizzes} quizzes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
          Text(
            text = "${displayedAccuracy.toInt()}% acc",
            style = MaterialTheme.typography.labelSmall,
            color = if (sortBy == LeaderboardSortOption.ACCURACY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (sortBy == LeaderboardSortOption.ACCURACY) FontWeight.Bold else FontWeight.Normal
          )
          Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
          Text(
            text = "${displayedTime}s avg",
            style = MaterialTheme.typography.labelSmall,
            color = if (sortBy == LeaderboardSortOption.TIME_TAKEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (sortBy == LeaderboardSortOption.TIME_TAKEN) FontWeight.Bold else FontWeight.Normal
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // 4. Primary Score / Sort Metric
      Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = when (sortBy) {
              LeaderboardSortOption.SCORE -> Icons.Default.LocalFireDepartment
              LeaderboardSortOption.ACCURACY -> Icons.Default.Percent
              LeaderboardSortOption.TIME_TAKEN -> Icons.Default.Timer
            },
            contentDescription = null,
            tint = when (sortBy) {
              LeaderboardSortOption.SCORE -> Color(0xFFF97316)
              LeaderboardSortOption.ACCURACY -> Color(0xFF10B981)
              LeaderboardSortOption.TIME_TAKEN -> Color(0xFF3B82F6)
            },
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = when (sortBy) {
              LeaderboardSortOption.SCORE -> "$displayedScore"
              LeaderboardSortOption.ACCURACY -> "${displayedAccuracy.toInt()}%"
              LeaderboardSortOption.TIME_TAKEN -> "${displayedTime}s"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("leaderboard_points_${user.rank}")
          )
        }
        Text(
          text = when (sortBy) {
            LeaderboardSortOption.SCORE -> "score"
            LeaderboardSortOption.ACCURACY -> "accuracy"
            LeaderboardSortOption.TIME_TAKEN -> "time"
          },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

/**
 * Individual Quiz Leaderboard Card.
 */
@Composable
fun IndividualQuizLeaderboardCard(
  entry: QuizLeaderboardEntry,
  sortBy: LeaderboardSortOption,
  isCurrentUser: Boolean,
  modifier: Modifier = Modifier
) {
  val (rankColor, rankBg, rankBorder) = when (entry.rank) {
    1 -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFFF59E0B))
    2 -> Triple(Color(0xFF64748B), Color(0xFFF1F5F9), Color(0xFF94A3B8))
    3 -> Triple(Color(0xFFB45309), Color(0xFFFFEDD5), Color(0xFFD97706))
    else -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), Color.Transparent)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("quiz_leaderboard_entry_${entry.rank}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
    ),
    border = if (isCurrentUser) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
             else if (entry.rank <= 3) BorderStroke(1.dp, rankBorder) else null
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Rank Badge
      Surface(
        shape = CircleShape,
        color = rankBg,
        border = BorderStroke(1.dp, rankBorder),
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(text = "#${entry.rank}", fontWeight = FontWeight.Bold, color = rankColor)
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      UserAvatar(name = entry.displayName, size = 42)

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = entry.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (isCurrentUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
              Text("YOU", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
          }
        }
        Text(text = "@${entry.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "${entry.score}/${entry.totalQuestions} correct",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (sortBy == LeaderboardSortOption.SCORE) FontWeight.Bold else FontWeight.Normal
          )
          Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
          Text(
            text = "${entry.accuracy.toInt()}% acc",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (sortBy == LeaderboardSortOption.ACCURACY) FontWeight.Bold else FontWeight.Normal
          )
          Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
          Text(
            text = "${entry.timeTakenSeconds}s time",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (sortBy == LeaderboardSortOption.TIME_TAKEN) FontWeight.Bold else FontWeight.Normal
          )
        }
      }

      // Main Sort Highlight Badge
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = when (sortBy) {
            LeaderboardSortOption.SCORE -> "${entry.score} pts"
            LeaderboardSortOption.ACCURACY -> "${entry.accuracy.toInt()}%"
            LeaderboardSortOption.TIME_TAKEN -> "${entry.timeTakenSeconds}s"
          },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(text = sortBy.label.lowercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

/**
 * Dedicated Composable to show an Individual Quiz Leaderboard.
 * Designed to be embedded directly into QuizResultScreen, quiz details, or bottom sheets.
 */
@Composable
fun IndividualQuizLeaderboardView(
  quizId: String,
  repository: QuizPollRepository,
  modifier: Modifier = Modifier,
  currentUserId: String = ""
) {
  var selectedSortOption by remember { mutableStateOf(LeaderboardSortOption.SCORE) }
  var entries by remember { mutableStateOf<List<QuizLeaderboardEntry>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(quizId, selectedSortOption) {
    isLoading = true
    repository.observeQuizLeaderboard(quizId, selectedSortOption).collectLatest { list ->
      entries = list
      isLoading = false
    }
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("individual_quiz_leaderboard_view")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Quiz Leaderboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Sort Chips
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          LeaderboardSortOption.values().forEach { sort ->
            val isSelected = selectedSortOption == sort
            FilterChip(
              selected = isSelected,
              onClick = { selectedSortOption = sort },
              label = { Text(sort.label, fontSize = 11.sp) },
              modifier = Modifier.height(32.dp).testTag("result_sort_${sort.name.lowercase()}")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (isLoading && entries.isEmpty()) {
        SkeletonLoader(modifier = Modifier.height(120.dp))
      } else if (entries.isEmpty()) {
        Text(
          text = "No recorded scores yet. You're among the first to complete this challenge!",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp)
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          entries.take(5).forEach { entry ->
            IndividualQuizLeaderboardCard(
              entry = entry,
              sortBy = selectedSortOption,
              isCurrentUser = entry.userId == currentUserId
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LeaderboardHeroBanner() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    )
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Quiz Competitions",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Live rankings by score, accuracy & time taken across the community",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

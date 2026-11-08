package com.example.ui.screens.profile

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityPointsCalculator
import com.example.data.model.BookmarkItem
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.ActivityPointsAndBadgesCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PollOptionsPreview
import com.example.ui.components.PollResultChart
import com.example.ui.components.ReferralCard
import com.example.ui.components.PostCard
import com.example.ui.components.ProfileSkeletonLoader
import com.example.ui.components.UserAvatar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  repository: QuizPollRepository,
  currentUserId: String,
  onNavigateToSettings: () -> Unit,
  onNavigateToFollowers: (String) -> Unit,
  onNavigateToFollowing: (String) -> Unit,
  onNavigateToPostDetail: (String) -> Unit,
  onNavigateToQuizPlayer: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var profile by remember { mutableStateOf<UserProfile?>(null) }
  var userQuizzes by remember { mutableStateOf<List<Quiz>>(emptyList()) }
  var userPolls by remember { mutableStateOf<List<Poll>>(emptyList()) }
  var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var userAttempts by remember { mutableStateOf<List<QuizAttempt>>(emptyList()) }
  var userBookmarks by remember { mutableStateOf<List<BookmarkItem>>(emptyList()) }
  var userCompletedPolls by remember { mutableStateOf<List<Poll>>(emptyList()) }
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabTitles = listOf(
    "Quiz History (${userAttempts.size})",
    "Badges & Level",
    "Created Quizzes (${userQuizzes.size})",
    "Completed Polls (${userCompletedPolls.size})",
    "Created Polls (${userPolls.size})",
    "Saved (${userBookmarks.size})",
    "Discussions"
  )

  var showEditDialog by remember { mutableStateOf(false) }

  // Observe User Profile from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeUserProfile(currentUserId).collectLatest {
      if (it != null) {
        profile = it
      } else {
        profile = try {
          repository.getOrCreateUserProfile()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
          throw e
        } catch (e: Exception) {
          android.util.Log.e("ProfileScreen", "Could not load profile", e)
          profile ?: UserProfile(userId = currentUserId)
        }
      }
    }
  }

  // Observe User's Created Quizzes from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeUserQuizzes(currentUserId).collectLatest {
      userQuizzes = it
    }
  }

  // Observe User's Created Polls from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeUserPolls(currentUserId).collectLatest {
      userPolls = it
    }
  }

  // Observe User's Completed / Voted Polls from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeUserVotedPolls(currentUserId).collectLatest {
      userCompletedPolls = it
    }
  }

  // Observe User's Saved Bookmarks from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeUserBookmarks(currentUserId).collectLatest {
      userBookmarks = it
    }
  }

  // Observe User's Feed Posts from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeFeed(tab = "Latest").collectLatest { all ->
      userPosts = all.filter { it.userId == currentUserId }
    }
  }

  // Observe User's Quiz Attempt History from Firestore
  LaunchedEffect(currentUserId) {
    repository.observeUserAttempts(currentUserId).collectLatest {
      userAttempts = it
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("My Profile", fontWeight = FontWeight.Bold) },
        actions = {
          IconButton(onClick = { selectedTab = 2 }, modifier = Modifier.testTag("profile_saved_bookmarks_btn")) {
            Icon(Icons.Default.Bookmark, contentDescription = "Saved Bookmarks", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("profile_settings_btn")) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        }
      )
    }
  ) { innerPadding ->
    if (profile == null) {
      ProfileSkeletonLoader(modifier = Modifier.padding(innerPadding))
    } else {
      val p = profile!!
      val quizCount = maxOf(userQuizzes.size, p.quizCount.toInt())
      val pollCount = maxOf(userPolls.size, p.pollCount.toInt())
      val commentCount = maxOf(p.commentCount.toInt(), userPosts.sumOf { it.commentCount.toInt() })
      val stats = remember(quizCount, pollCount, commentCount) {
        ActivityPointsCalculator.computeStats(quizCount, pollCount, commentCount)
      }

      LazyColumn(
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // Profile Header
        item {
          Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              UserAvatar(
                name = p.displayName,
                avatarUrl = p.profilePictureUrl,
                size = 72,
                modifier = Modifier.testTag("profile_avatar_image")
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = p.displayName.ifBlank { "Quiz Member" },
                  style = MaterialTheme.typography.headlineLarge,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "@${p.username}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.testTag("edit_profile_btn")
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
              }
            }

            if (p.bio.isNotBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = p.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            } else {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Quiz enthusiast & knowledge contributor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row (Quizzes, Polls, Followers, Following)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              StatColumn(
                title = "Quizzes",
                value = maxOf(userQuizzes.size.toLong(), p.quizCount).toString(),
                modifier = Modifier.clickable { selectedTab = 2 }
              )
              StatColumn(
                title = "Completed",
                value = userCompletedPolls.size.toString(),
                modifier = Modifier.clickable { selectedTab = 3 }
              )
              StatColumn(
                title = "Polls",
                value = maxOf(userPolls.size.toLong(), p.pollCount).toString(),
                modifier = Modifier.clickable { selectedTab = 4 }
              )
              StatColumn(
                title = "Followers",
                value = p.followersCount.toString(),
                modifier = Modifier.clickable { onNavigateToFollowers(currentUserId) }
              )
              StatColumn(
                title = "Following",
                value = p.followingCount.toString(),
                modifier = Modifier.clickable { onNavigateToFollowing(currentUserId) }
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Edit Profile & Manage Account Settings
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("edit_profile_btn"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Profile", style = MaterialTheme.typography.labelLarge)
              }
              Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                  .weight(1f)
                  .testTag("manage_account_settings_btn")
                  .testTag("profile_manage_settings_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Account Settings", style = MaterialTheme.typography.labelLarge)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
          }
        }

        // Tab Row
        item {
          ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
          ) {
            tabTitles.forEachIndexed { index, title ->
              val tabTag = when (index) {
                0 -> "profile_history_tab"
                1 -> "profile_badges_tab"
                2 -> "profile_quizzes_tab"
                3 -> "profile_completed_polls_tab"
                4 -> "profile_polls_tab"
                5 -> "profile_saved_tab"
                else -> "profile_posts_tab"
              }
              Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = {
                  Text(
                    title,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                  )
                },
                modifier = Modifier.testTag(tabTag)
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Tab Content
        when (selectedTab) {
          // Tab 0: Quiz History
          0 -> {
            if (userAttempts.isEmpty()) {
              item {
                EmptyStateView(
                  title = "No Quiz History",
                  message = "Play quizzes from the home feed to track your scores, speed, and accuracy here!",
                  actionLabel = "Explore Quizzes",
                  onActionClick = { selectedTab = 2 }
                )
              }
            } else {
              items(userAttempts, key = { it.id }) { att ->
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("quiz_history_card_${att.id}")
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                      ) {
                        Surface(
                          shape = CircleShape,
                          color = MaterialTheme.colorScheme.primaryContainer,
                          modifier = Modifier.size(38.dp)
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              Icons.Default.History,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(20.dp)
                            )
                          }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text(
                            text = att.quizTitle.ifBlank { "Quiz Challenge" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("quiz_history_title_${att.id}")
                          )
                          Text(
                            text = "Score: ${att.score}/${att.totalQuestions} • ${att.timeTakenSeconds}s elapsed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }

                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (att.accuracy >= 70.0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                      ) {
                        Text(
                          text = "${att.accuracy.toInt()}%",
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          color = if (att.accuracy >= 70.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Completed attempt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )

                      Button(
                        onClick = { onNavigateToQuizPlayer(att.quizId) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("quiz_history_play_again_${att.id}")
                      ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Again", style = MaterialTheme.typography.labelMedium)
                      }
                    }
                  }
                }
              }
            }
          }

          // Tab 1: Badges & Level & Referral
          1 -> {
            item {
              ActivityPointsAndBadgesCard(stats = stats)
            }
            item {
              Spacer(modifier = Modifier.height(12.dp))
              profile?.let { p ->
                ReferralCard(
                  profile = p,
                  repository = repository,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                  onProfileUpdated = {
                    scope.launch {
                      try {
                        profile = repository.getOrCreateUserProfile()
                      } catch (e: Exception) {
                        android.util.Log.e("ProfileScreen", "Profile refresh failed", e)
                      }
                    }
                  }
                )
              }
            }
          }

          // Tab 2: Created Quizzes
          2 -> {
            if (userQuizzes.isEmpty()) {
              item {
                EmptyStateView(
                  title = "No Quizzes Created",
                  message = "You haven't created any community quiz challenges yet. Tap below to build one!",
                  actionLabel = "Create a Quiz",
                  onActionClick = { /* Can be routed from parent or bottom bar */ }
                )
              }
            } else {
              items(userQuizzes, key = { it.id }) { quiz ->
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("user_quiz_card_${quiz.id}"),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (quiz.difficulty.lowercase()) {
                          "easy" -> MaterialTheme.colorScheme.secondaryContainer
                          "hard" -> MaterialTheme.colorScheme.errorContainer
                          else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                      ) {
                        Text(
                          text = quiz.difficulty.ifBlank { "Medium" },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                      text = quiz.title,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (quiz.description.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = quiz.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = "${quiz.questionCount} Questions • ${quiz.attemptCount} plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                          onClick = {
                            scope.launch {
                              repository.deleteQuiz(quiz.id)
                            }
                          },
                          modifier = Modifier.size(36.dp).testTag("delete_user_quiz_${quiz.id}")
                        ) {
                          Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Quiz",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                          )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                          onClick = { onNavigateToQuizPlayer(quiz.id) },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                          modifier = Modifier.testTag("play_user_quiz_${quiz.id}")
                        ) {
                          Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Play Quiz")
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // Tab 3: Completed Polls (User has voted and can view live results)
          3 -> {
            if (userCompletedPolls.isEmpty()) {
              item {
                EmptyStateView(
                  title = "No Completed Polls",
                  message = "You haven't participated in any community polls yet. Cast your vote from the feed to view real-time charts and results here!",
                  icon = Icons.Default.Poll
                )
              }
            } else {
              items(userCompletedPolls, key = { it.id }) { poll ->
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("completed_poll_card_${poll.id}"),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                      ) {
                        Text(
                          text = poll.category.ifBlank { "Community" },
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSecondaryContainer,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = "Voted",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                          )
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                      text = poll.question,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (poll.description.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = poll.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-time Poll Results Chart (Pie Chart & Bar Graph)
                    PollResultChart(
                      poll = poll,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completed_poll_chart_${poll.id}")
                    )
                  }
                }
              }
            }
          }

          // Tab 4: Created Polls
          4 -> {
            if (userPolls.isEmpty()) {
              item {
                EmptyStateView(
                  title = "No Polls Created",
                  message = "You haven't created any community polls yet. Share questions to gather opinions!",
                  actionLabel = "Create a Poll",
                  onActionClick = { /* Can be routed from parent */ }
                )
              }
            } else {
              items(userPolls, key = { it.id }) { poll ->
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("user_poll_card_${poll.id}"),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                      ) {
                        Text(
                          text = poll.category.ifBlank { "Community" },
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSecondaryContainer,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                      IconButton(
                        onClick = {
                          scope.launch {
                            repository.deletePoll(poll.id)
                          }
                        },
                        modifier = Modifier.size(32.dp).testTag("delete_user_poll_${poll.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete Poll",
                          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                      text = poll.question,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (poll.description.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = poll.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-time Poll Results Visualization
                    PollResultChart(
                      poll = poll,
                      modifier = Modifier.fillMaxWidth().testTag("created_poll_chart_${poll.id}")
                    )
                  }
                }
              }
            }
          }

          // Tab 5: Saved (Personal Bookmarks)
          5 -> {
            if (userBookmarks.isEmpty()) {
              item {
                EmptyStateView(
                  title = "No Saved Items Yet",
                  message = "Bookmark your favorite quizzes and community polls to quickly revisit, play, or vote on them here.",
                  icon = Icons.Default.Bookmark
                )
              }
            } else {
              items(userBookmarks, key = { it.id }) { bookmark ->
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("saved_bookmark_item_${bookmark.id}"),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = if (bookmark.postType == "quiz") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                          Text(
                            text = if (bookmark.postType == "quiz") "Quiz" else if (bookmark.postType == "poll") "Poll" else "Discussion",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (bookmark.postType == "quiz") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = bookmark.category,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      IconButton(
                        onClick = {
                          scope.launch {
                            repository.toggleBookmark(
                              postId = bookmark.postId,
                              postType = bookmark.postType,
                              title = bookmark.title,
                              currentlyBookmarked = true
                            )
                          }
                        },
                        modifier = Modifier.size(32.dp).testTag("remove_bookmark_${bookmark.id}")
                      ) {
                        Icon(
                          Icons.Default.Delete,
                          contentDescription = "Remove Bookmark",
                          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                      text = bookmark.title,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (bookmark.content.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = bookmark.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                      )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = if (bookmark.authorName.isNotBlank()) "by ${bookmark.authorName}" else "Community Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )

                      if (bookmark.postType == "quiz" && bookmark.quizId.isNotBlank()) {
                        Button(
                          onClick = { onNavigateToQuizPlayer(bookmark.quizId) },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                          modifier = Modifier.testTag("play_bookmark_${bookmark.id}")
                        ) {
                          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Play Quiz")
                        }
                      } else {
                        Button(
                          onClick = { onNavigateToPostDetail(bookmark.postId) },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                          modifier = Modifier.testTag("view_bookmark_${bookmark.id}")
                        ) {
                          Text("View Item")
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // Tab 6: Discussions
          6 -> {
            val discussions = userPosts.filter { it.postType == "discussion" }
            if (discussions.isEmpty()) {
              item { EmptyStateView(title = "No Discussions", message = "You haven't posted any discussion topics yet.") }
            } else {
              items(discussions, key = { it.id }) { post ->
                PostCard(
                  post = post,
                  currentUserId = currentUserId,
                  isLiked = false,
                  isBookmarked = false,
                  onLikeClick = { scope.launch { repository.toggleLike(post.id, false) } },
                  onBookmarkClick = { scope.launch { repository.toggleBookmark(post.id, post.postType, post.title, false) } },
                  onCommentClick = { onNavigateToPostDetail(post.id) },
                  onShareClick = {},
                  onPostClick = { onNavigateToPostDetail(post.id) },
                  onReportClick = {},
                  onBlockUserClick = {}
                )
              }
            }
          }
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditDialog && profile != null) {
    EditProfileDialog(
      profile = profile!!,
      onDismiss = { showEditDialog = false },
      onSave = { newName, newBio, newLang, newAvatarUrl ->
        scope.launch {
          repository.updateUserProfile(newName, newBio, newLang, newAvatarUrl)
          showEditDialog = false
        }
      }
    )
  }
}

@Composable
private fun StatColumn(title: String, value: String, modifier: Modifier = Modifier) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.padding(horizontal = 8.dp)
  ) {
    Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun EditProfileDialog(
  profile: UserProfile,
  onDismiss: () -> Unit,
  onSave: (displayName: String, bio: String, language: String, profilePictureUrl: String) -> Unit
) {
  var displayName by remember { mutableStateOf(profile.displayName) }
  var bio by remember { mutableStateOf(profile.bio) }
  var language by remember { mutableStateOf(profile.language) }
  var profilePictureUrl by remember { mutableStateOf(profile.profilePictureUrl) }

  val avatarPresets = listOf("🎓", "🔬", "🚀", "💡", "🎨", "🏆", "📚", "💻", "⭐", "🔥")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
      ) {
        // Current avatar preview
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          UserAvatar(
            name = displayName.ifBlank { "User" },
            avatarUrl = profilePictureUrl,
            size = 68,
            modifier = Modifier.testTag("edit_avatar_preview")
          )
        }

        // Avatar preset picker
        Text(
          text = "Choose Avatar Preset",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          avatarPresets.forEach { emoji ->
            val isSelected = profilePictureUrl == "emoji:$emoji"
            Surface(
              shape = CircleShape,
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { profilePictureUrl = "emoji:$emoji" }
                .testTag("edit_avatar_chip_$emoji")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = emoji, fontSize = 20.sp)
              }
            }
          }
        }

        OutlinedTextField(
          value = if (profilePictureUrl.startsWith("emoji:")) "" else profilePictureUrl,
          onValueChange = { profilePictureUrl = it },
          label = { Text("Custom Profile Picture URL") },
          placeholder = { Text("https://example.com/photo.jpg") },
          modifier = Modifier.fillMaxWidth().testTag("edit_profile_picture_url_input"),
          singleLine = true
        )

        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("Display Name") },
          modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input"),
          singleLine = true
        )

        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it },
          label = { Text("Bio") },
          modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
          maxLines = 3
        )

        OutlinedTextField(
          value = language,
          onValueChange = { language = it },
          label = { Text("Language") },
          modifier = Modifier.fillMaxWidth().testTag("edit_language_input"),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(displayName, bio, language, profilePictureUrl) },
        modifier = Modifier.testTag("save_profile_dialog_btn")
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_profile_dialog_btn")) {
        Text("Cancel")
      }
    }
  )
}

package com.example.ui.screens.explore

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.components.LeaderboardView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.QuizCategories
import com.example.data.model.UserProfile
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.PollWidget
import com.example.ui.components.QuizCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberShimmerBrush
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
  repository: QuizPollRepository,
  currentUserId: String,
  onNavigateToSearch: (initialCategory: String?) -> Unit,
  onNavigateToPostDetail: (String) -> Unit,
  onNavigateToQuizPlayer: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var trendingPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var isLoadingTrending by remember { mutableStateOf(true) }
  var followingUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

  var spotlightPollVotes by remember { mutableStateOf<List<Int>>(emptyList()) }
  var spotlightPoll by remember { mutableStateOf<Poll?>(null) }

  LaunchedEffect(Unit) {
    repository.observeFeed(tab = "Trending").collectLatest {
      trendingPosts = it
      isLoadingTrending = false
    }
  }

  LaunchedEffect(currentUserId) {
    repository.observeSuggestedUsers(currentUserId).collectLatest { suggestedUsers = it }
  }

  LaunchedEffect(currentUserId) {
    repository.observeFollowingIds(currentUserId).collectLatest { followingUserIds = it }
  }

  val trendingQuizzes = trendingPosts.filter { it.postType == "quiz" && it.quizId.isNotBlank() }
  val trendingPolls = trendingPosts.filter { it.postType == "poll" && it.pollId.isNotBlank() }
  val trendingDiscussions = trendingPosts.filter { it.postType == "discussion" }
  val spotlightQuizPost = trendingQuizzes.firstOrNull()
  val spotlightPollPost = trendingPolls.firstOrNull()

  LaunchedEffect(spotlightPollPost?.pollId) {
    val pollId = spotlightPollPost?.pollId
    if (pollId.isNullOrBlank()) {
      spotlightPoll = null
      spotlightPollVotes = emptyList()
    } else {
      repository.observePoll(pollId).collectLatest { spotlightPoll = it }
    }
  }

  LaunchedEffect(spotlightPollPost?.pollId) {
    val pollId = spotlightPollPost?.pollId
    if (!pollId.isNullOrBlank()) {
      repository.observeUserVote(pollId).collectLatest { vote ->
        spotlightPollVotes = vote?.selectedOptions ?: emptyList()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    TopAppBar(
      title = {
        Text("Explore & Discover", fontWeight = FontWeight.Bold)
      }
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
      // Search entry that navigates to SearchScreen
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { onNavigateToSearch(null) }
          .testTag("explore_search_bar"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Search quizzes, polls, topics & users...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Real community quiz spotlight; no fabricated daily question or score.
      SectionHeader(title = "Community Quiz Spotlight", icon = Icons.Default.HelpOutline)
      if (spotlightQuizPost == null) {
        Text(
          "No trending community quiz yet. Create the first quiz and it can appear here.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp)
        )
      } else {
        Card(
          modifier = Modifier.fillMaxWidth().clickable { onNavigateToQuizPlayer(spotlightQuizPost.quizId) },
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(spotlightQuizPost.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("By @${spotlightQuizPost.authorUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = { onNavigateToQuizPlayer(spotlightQuizPost.quizId) }) { Text("Play Quiz") }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Real Firestore poll only; never display fabricated vote totals.
      SectionHeader(title = "Trending Community Poll", icon = Icons.Default.BarChart)
      if (spotlightPoll == null) {
        Text(
          "No trending community poll yet. Published polls will appear here.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp)
        )
      } else {
        val activePoll = spotlightPoll
        if (activePoll != null) {
          PollWidget(
            poll = activePoll,
            userVotedIndices = spotlightPollVotes,
            onVoteCast = { selected ->
              scope.launch {
                try { repository.submitPollVote(activePoll.id, selected) } catch (_: Exception) { }
              }
            },
            modifier = Modifier.padding(horizontal = 0.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Popular Subjects Grid
      SectionHeader(title = "Academic & Knowledge Categories", icon = Icons.Default.TrendingUp)

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuizCategories.ALL.chunked(2).forEach { rowCategories ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            rowCategories.forEach { category ->
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onNavigateToSearch(category) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.HelpOutline, contentDescription = category, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
            if (rowCategories.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Trending Quizzes
      if (isLoadingTrending) {
        val shimmerBrush = rememberShimmerBrush()
        SectionHeader(title = "Featured Community Quizzes", icon = Icons.Default.HelpOutline)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(3) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
              modifier = Modifier.width(260.dp).height(125.dp)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Box(modifier = Modifier.size(width = 80.dp, height = 14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.size(width = 110.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      } else if (trendingQuizzes.isNotEmpty()) {
        SectionHeader(title = "Featured Community Quizzes", icon = Icons.Default.HelpOutline)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(trendingQuizzes) { post ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
              modifier = Modifier
                .width(260.dp)
                .clickable {
                  if (post.quizId.isNotBlank()) onNavigateToQuizPlayer(post.quizId)
                  else onNavigateToPostDetail(post.id)
                }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(text = post.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = "By @${post.authorUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Button(
                    onClick = { if (post.quizId.isNotBlank()) onNavigateToQuizPlayer(post.quizId) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Text("Play", style = MaterialTheme.typography.labelMedium)
                  }
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }

      // Trending Polls
      if (isLoadingTrending) {
        val shimmerBrush = rememberShimmerBrush()
        SectionHeader(title = "Active Community Polls", icon = Icons.Default.BarChart)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(3) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
              modifier = Modifier.width(260.dp).height(125.dp)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Box(modifier = Modifier.size(width = 80.dp, height = 14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.size(width = 110.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      } else if (trendingPolls.isNotEmpty()) {
        SectionHeader(title = "Active Community Polls", icon = Icons.Default.BarChart)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(trendingPolls) { post ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
              modifier = Modifier
                .width(260.dp)
                .clickable { onNavigateToPostDetail(post.id) }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(text = post.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = "By @${post.authorUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  OutlinedButton(
                    onClick = { onNavigateToPostDetail(post.id) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Text("Vote", style = MaterialTheme.typography.labelMedium)
                  }
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }

      // Recommended contributors are real Firebase users, never fabricated demo profiles.
      SectionHeader(title = "Community Educators", icon = Icons.Default.PersonAdd)
      if (suggestedUsers.isEmpty()) {
        Text(
          "Community creators will appear here as learners join and publish quizzes.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp)
        )
      } else {
        suggestedUsers.forEach { profile ->
          val userId = profile.userId
          val isFollowing = followingUserIds.contains(userId)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              UserAvatar(name = profile.displayName, size = 40)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "@${profile.username} • ${profile.quizCount} quizzes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Button(
                onClick = {
                  scope.launch {
                    try {
                      if (isFollowing) repository.unfollowUser(userId)
                      else repository.followUser(userId, profile.displayName, profile.username)
                      followingUserIds = if (isFollowing) followingUserIds - userId else followingUserIds + userId
                    } catch (_: Exception) { }
                  }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                  contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
              ) {
                Text(if (isFollowing) "Following" else "Follow")
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Community Competition Leaderboards Section
      SectionHeader(title = "Quiz Competitions & Leaderboards", icon = Icons.Default.EmojiEvents)
      LeaderboardView(
        repository = repository,
        currentUserId = currentUserId,
        onNavigateToQuizPlayer = onNavigateToQuizPlayer,
        modifier = Modifier.heightIn(max = 780.dp)
      )

      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 8.dp)
  ) {
    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
  }
}

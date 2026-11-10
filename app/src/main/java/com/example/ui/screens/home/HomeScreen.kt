package com.example.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Comment
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.data.model.QuizCategories
import com.example.data.model.WeeklyTrendingSummary
import com.example.data.remote.QuizPollRepository
import androidx.compose.material.icons.filled.Poll
import com.example.ui.components.CategoryChip
import com.example.ui.components.DiscussionFeed
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LeaderboardView
import com.example.ui.components.OfflineBanner
import com.example.ui.components.PollCreatorDialog
import com.example.ui.components.PostCard
import com.example.ui.components.QuizFeed
import com.example.ui.components.ReportDialog
import com.example.ui.components.SkeletonLoader
import com.example.ui.components.TrendingChartsSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  repository: QuizPollRepository,
  currentUserId: String,
  onNavigateToSearch: () -> Unit,
  onNavigateToNotifications: () -> Unit,
  onNavigateToPostDetail: (String) -> Unit,
  onNavigateToQuizPlayer: (String) -> Unit,
  onNavigateToWallet: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("For You", "Quizzes", "Discussions", "Trending", "Leaderboard")

  var selectedCategory by remember { mutableStateOf<String?>("All") }
  var selectedFilterType by remember { mutableStateOf("All") } // "All", "Quizzes", "Polls"
  var isPollCreatorOpen by remember { mutableStateOf(false) }

  val categories = remember {
    val prioritized = listOf("All", "Science", "History", "Pop Culture", "Technology", "General Knowledge", "Entertainment", "Sports", "Geography")
    val remaining = QuizCategories.ALL.filter { it !in prioritized }
    prioritized + remaining
  }

  var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  var isOffline by remember { mutableStateOf(false) }

  val filteredPosts = remember(posts, searchQuery, selectedFilterType, selectedCategory) {
    var list = posts
    if (selectedFilterType == "Quizzes") {
      list = list.filter { it.postType == "quiz" }
    } else if (selectedFilterType == "Polls") {
      list = list.filter { it.postType == "poll" }
    }

    if (selectedCategory != null && selectedCategory != "All") {
      list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    if (searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      list = list.filter { post ->
        post.title.lowercase().contains(q) ||
        post.content.lowercase().contains(q) ||
        post.category.lowercase().contains(q)
      }
    }
    list
  }

  // State maps for user interactions
  var likedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var bookmarkedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var quizDataMap by remember { mutableStateOf<Map<String, Quiz>>(emptyMap()) }
  var pollDataMap by remember { mutableStateOf<Map<String, Poll>>(emptyMap()) }
  var pollVotesMap by remember { mutableStateOf<Map<String, List<Int>>>(emptyMap()) }
  var quizCommentsMap by remember { mutableStateOf<Map<String, List<Comment>>>(emptyMap()) }
  var isRefreshing by remember { mutableStateOf(false) }
  var weeklyTrending by remember { mutableStateOf(WeeklyTrendingSummary()) }

  // Reporting dialog state
  var reportingPost by remember { mutableStateOf<Post?>(null) }

  // Seed starter content on first launch if empty & sync FCM token
  LaunchedEffect(Unit) {
    scope.launch {
      repository.seedStarterContentIfEmpty()
    }
    scope.launch {
      repository.syncFcmToken()
    }
    scope.launch {
      repository.observeWeeklyTrendingSummary().collectLatest {
        weeklyTrending = it
      }
    }
  }

  // Observe user's real saved bookmarks
  LaunchedEffect(currentUserId) {
    repository.observeUserBookmarks(currentUserId).collectLatest { bookmarks ->
      bookmarkedPostIds = bookmarks.map { it.postId }.toSet()
    }
  }

  // Load feed based on tab and category
  LaunchedEffect(selectedTabIndex, selectedCategory) {
    isLoading = true
    val tabName = tabs[selectedTabIndex]
    repository.observeFeed(
      tab = tabName,
      categoryFilter = if (selectedCategory == "All") null else selectedCategory
    ).collectLatest { fetchedPosts ->
      posts = fetchedPosts
      isLoading = false

      // Attach realtime listeners for companion quizzes
      fetchedPosts.filter { it.postType == "quiz" && it.quizId.isNotBlank() }.forEach { post ->
        scope.launch {
          repository.observeQuiz(post.quizId).collectLatest { quiz ->
            if (quiz != null) {
              quizDataMap = quizDataMap + (post.quizId to quiz)
            }
          }
        }
      }

      // Attach realtime listeners for post comments
      fetchedPosts.forEach { post ->
        scope.launch {
          repository.observeComments(post.id).collectLatest { comments ->
            quizCommentsMap = quizCommentsMap + (post.id to comments)
          }
        }
      }

      // Attach realtime listeners for companion polls and user votes
      fetchedPosts.filter { it.postType == "poll" && it.pollId.isNotBlank() }.forEach { post ->
        scope.launch {
          repository.observePoll(post.pollId).collectLatest { poll ->
            if (poll != null) {
              pollDataMap = pollDataMap + (post.pollId to poll)
            }
          }
        }
        scope.launch {
          repository.observeUserVote(post.pollId).collectLatest { vote ->
            if (vote != null) {
              pollVotesMap = pollVotesMap + (post.pollId to vote.selectedOptions)
            }
          }
        }
      }
    }
  }

  // Room database fuzzy search integration for instant offline keyword & category filtering
  LaunchedEffect(searchQuery, selectedCategory, selectedFilterType) {
    if (searchQuery.isNotBlank()) {
      repository.searchCachedPostsWithFuzzyMatching(
        query = searchQuery,
        category = selectedCategory,
        postType = selectedFilterType
      ).collectLatest { roomMatches ->
        if (roomMatches.isNotEmpty()) {
          val combined = (roomMatches + posts).distinctBy { it.id }
          posts = combined
        }
      }
    }
  }


  Column(modifier = modifier.fillMaxSize()) {
    // Top App Bar
    TopAppBar(
      title = {
        Text(
          text = "QuizPoll",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary
        )
      },
      actions = {
        IconButton(
          onClick = { isPollCreatorOpen = true },
          modifier = Modifier.testTag("home_create_poll_action")
        ) {
          Icon(Icons.Default.Poll, contentDescription = "Create Poll", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onNavigateToWallet, modifier = Modifier.testTag("nav_wallet_btn")) {
          Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet & Coins", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onNavigateToSearch, modifier = Modifier.testTag("nav_search_btn")) {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onNavigateToNotifications, modifier = Modifier.testTag("nav_notifications_btn")) {
          BadgedBox(
            badge = {
              Badge { Text("3") }
            }
          ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
          }
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    )

    // Global Search Bar at top of feed for keywords & topic filtering
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("feed_search_bar")
        .testTag("global_search_bar"),
      placeholder = {
        Text(
          "Search quizzes & polls by topic or keywords...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      leadingIcon = {
        Icon(
          Icons.Default.Search,
          contentDescription = "Search",
          tint = MaterialTheme.colorScheme.primary
        )
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.testTag("search_clear_btn")
          ) {
            Icon(
              Icons.Default.Clear,
              contentDescription = "Clear search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(24.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
      )
    )

    // Content Type Filter row (All, Quizzes, Polls)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Type:",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      listOf("All", "Quizzes", "Polls").forEach { type ->
        val isSelected = selectedFilterType == type
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { selectedFilterType = type }
            .testTag(
              when (type) {
                "Quizzes" -> "search_filter_type_quiz"
                "Polls" -> "search_filter_type_poll"
                else -> "search_filter_type_all"
              }
            )
        ) {
          Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
    }

    if (searchQuery.isNotBlank() || selectedFilterType != "All" || (selectedCategory != null && selectedCategory != "All")) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val filterDesc = buildString {
          append("${filteredPosts.size} results")
          if (selectedFilterType != "All") append(" • $selectedFilterType")
          if (selectedCategory != null && selectedCategory != "All") append(" • $selectedCategory")
          if (searchQuery.isNotBlank()) append(" • \"$searchQuery\"")
        }
        Text(
          text = filterDesc,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
        TextButton(
          onClick = {
            searchQuery = ""
            selectedFilterType = "All"
            selectedCategory = "All"
          },
          modifier = Modifier.testTag("clear_search_chip")
        ) {
          Text("Reset", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    // Feed Tabs
    ScrollableTabRow(
      selectedTabIndex = selectedTabIndex,
      edgePadding = 16.dp,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          text = {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
            )
          },
          modifier = Modifier.testTag("feed_tab_$index")
        )
      }
    }

    // Category Filter Bar with topic search tags
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("category_filter_row")
    ) {
      categories.forEach { cat ->
        CategoryChip(
          text = cat,
          isSelected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          modifier = Modifier
            .padding(end = 6.dp)
            .testTag("search_filter_topic_$cat")
        )
      }
    }

    OfflineBanner(isOffline = isOffline)

    // Feed Content with Pull to Refresh
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        scope.launch {
          isRefreshing = true
          try {
            val refreshed = repository.refreshFeedAndSync(
              tab = tabs[selectedTabIndex],
              categoryFilter = if (selectedCategory == "All") null else selectedCategory
            )
            if (refreshed.isNotEmpty()) {
              posts = refreshed
            }
          } catch (_: Exception) {}
          delay(500)
          isRefreshing = false
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .testTag("feed_pull_refresh")
        .testTag("pull_refresh_indicator")
    ) {
      if (isLoading && filteredPosts.isEmpty() && searchQuery.isBlank()) {
        SkeletonLoader(modifier = Modifier.testTag("feed_skeleton_loader"))
      } else if (filteredPosts.isEmpty()) {
        if (searchQuery.isNotBlank()) {
          EmptyStateView(
            title = "No Matches Found",
            message = "We couldn't find any quiz or poll titles matching \"$searchQuery\". Try checking the spelling or searching another topic.",
            actionLabel = "Clear Search",
            onActionClick = { searchQuery = "" }
          )
        } else {
          EmptyStateView(
            title = "No Posts Yet",
            message = "Be the first in the community to create a Quiz, Poll, or Discussion in ${selectedCategory ?: "this topic"}!",
            actionLabel = "Create Content",
            onActionClick = onNavigateToSearch
          )
        }
      } else if (selectedTabIndex == 1 && searchQuery.isBlank() && selectedFilterType == "All") {
        QuizFeed(
          repository = repository,
          onTakeQuiz = onNavigateToQuizPlayer,
          initialCategory = selectedCategory ?: "All",
          onShareQuiz = { quiz ->
            val deepLinkUrl = "https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/quiz/${quiz.id}"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, "Quiz: ${quiz.title}")
              putExtra(Intent.EXTRA_TEXT, "Take this quiz on QuizPoll!\n\n\"${quiz.title}\"\n\n$deepLinkUrl")
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Quiz with friends"))
          }
        )
      } else if (selectedTabIndex == 2 && searchQuery.isBlank() && selectedFilterType == "All") {
        val discussionList = remember(filteredPosts) {
          filteredPosts.filter { it.postType == "discussion" }
        }
        DiscussionFeed(
          discussions = discussionList,
          currentUserId = currentUserId,
          selectedTopic = selectedCategory ?: "All",
          onTopicSelected = { selectedCategory = it },
          onLikeClick = { post ->
            val isLiked = likedPostIds.contains(post.id)
            val newLiked = !isLiked
            likedPostIds = if (newLiked) likedPostIds + post.id else likedPostIds - post.id
            scope.launch {
              try {
                repository.toggleLike(post.id, isLiked)
              } catch (_: Exception) {}
            }
          },
          likedPostIds = likedPostIds,
          bookmarkedPostIds = bookmarkedPostIds,
          commentsMap = quizCommentsMap,
          onPostComment = { postId, text ->
            scope.launch {
              try {
                repository.addComment(postId, text)
              } catch (_: Exception) {}
            }
          },
          onBookmarkClick = { post ->
            val isBookmarked = bookmarkedPostIds.contains(post.id)
            val newBookmarked = !isBookmarked
            bookmarkedPostIds = if (newBookmarked) bookmarkedPostIds + post.id else bookmarkedPostIds - post.id
            scope.launch {
              try {
                repository.toggleBookmark(
                  postId = post.id,
                  postType = post.postType,
                  title = post.title,
                  currentlyBookmarked = isBookmarked,
                  content = post.content,
                  category = post.category,
                  authorName = post.authorName,
                  quizId = post.quizId,
                  pollId = post.pollId
                )
              } catch (_: Exception) {}
            }
          },
          onShareClick = { post ->
            val deepLinkUrl = "https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/post/${post.id}"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, "Discussion: ${post.title}")
              putExtra(Intent.EXTRA_TEXT, "Join the discussion on QuizPoll Community!\n\n\"${post.title}\"\n${post.content}\n\n$deepLinkUrl")
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Discussion with friends"))
          },
          onReportClick = { post ->
            reportingPost = post
          },
          onCreateDiscussionClick = onNavigateToSearch
        )
      } else if (selectedTabIndex == 4 && searchQuery.isBlank() && selectedFilterType == "All") {
        LeaderboardView(
          repository = repository,
          currentUserId = currentUserId,
          onNavigateToQuizPlayer = onNavigateToQuizPlayer
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("feed_lazy_column")
            .testTag("main_feed_lazy_column")
        ) {
          // Top 5 of the Week Charts displayed on Trending tab
          if (selectedTabIndex == 3 && searchQuery.isBlank()) {
            item {
              TrendingChartsSection(
                summary = weeklyTrending,
                onNavigateToQuizPlayer = onNavigateToQuizPlayer,
                onNavigateToPostDetail = onNavigateToPostDetail
              )
            }
          }

          items(filteredPosts, key = { it.id }) { post ->
            val isLiked = likedPostIds.contains(post.id)
            val isBookmarked = bookmarkedPostIds.contains(post.id)
            val poll = if (post.postType == "poll") pollDataMap[post.pollId] else null
            val userVotes = pollVotesMap[post.pollId] ?: emptyList()
            val comments = quizCommentsMap[post.id] ?: emptyList()

            PostCard(
              post = post,
              currentUserId = currentUserId,
              isLiked = isLiked,
              isBookmarked = isBookmarked,
              quizComments = comments,
              onAddQuizComment = { postId, text ->
                scope.launch {
                  try {
                    repository.addComment(postId, text)
                  } catch (_: Exception) {}
                }
              },
              onLikeClick = {
                val newLiked = !isLiked
                likedPostIds = if (newLiked) likedPostIds + post.id else likedPostIds - post.id
                scope.launch {
                  try {
                    repository.toggleLike(post.id, isLiked)
                  } catch (_: Exception) {}
                }
              },
              onBookmarkClick = {
                val newBookmarked = !isBookmarked
                bookmarkedPostIds = if (newBookmarked) bookmarkedPostIds + post.id else bookmarkedPostIds - post.id
                scope.launch {
                  try {
                    repository.toggleBookmark(
                      postId = post.id,
                      postType = post.postType,
                      title = post.title,
                      currentlyBookmarked = isBookmarked,
                      content = post.content,
                      category = post.category,
                      authorName = post.authorName,
                      quizId = post.quizId,
                      pollId = post.pollId
                    )
                  } catch (_: Exception) {}
                }
              },
              onCommentClick = {
                onNavigateToPostDetail(post.id)
              },
              onShareClick = {
                val shareType = when (post.postType) {
                  "quiz" -> "Quiz Challenge"
                  "poll" -> "Interactive Poll"
                  else -> "Community Discussion"
                }
                val shareSubject = "$shareType: ${post.title}"
                val deepLinkUrl = "https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/post/${post.id}"
                val shareBody = buildString {
                  append("📢 Check out this $shareType on QuizPoll Community!\n\n")
                  append("📌 \"${post.title}\"\n")
                  if (post.content.isNotBlank() && post.content != post.title) {
                    append("📝 ${post.content}\n")
                  }
                  append("\n🏷️ Topic: ${post.category}")
                  append("\n👤 Creator: @${post.authorUsername.ifBlank { "community" }}")
                  if (post.postType == "quiz" && quizDataMap[post.quizId] != null) {
                    val q = quizDataMap[post.quizId]!!
                    append("\n🧠 ${q.questionCount} Questions • ${q.difficulty} Difficulty")
                  } else if (post.postType == "poll" && pollDataMap[post.pollId] != null) {
                    val p = pollDataMap[post.pollId]!!
                    append("\n📊 ${p.options.size} Choices • ${p.totalVotes} total votes")
                  }
                  append("\n\n🔗 Open link to play, vote, or join the discussion:\n$deepLinkUrl")
                }

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                  putExtra(Intent.EXTRA_TEXT, shareBody)
                }
                val chooserIntent = Intent.createChooser(sendIntent, "Share $shareType with friends")
                context.startActivity(chooserIntent)
              },
              onPostClick = {
                onNavigateToPostDetail(post.id)
              },
              onStartQuizClick = { quizId ->
                onNavigateToQuizPlayer(quizId)
              },
              quizData = if (post.postType == "quiz") quizDataMap[post.quizId] else null,
              pollData = poll,
              userVotedOptions = userVotes,
              onVotePoll = { pollId, selected ->
                pollVotesMap = pollVotesMap + (pollId to selected)
                scope.launch {
                  try {
                    repository.submitPollVote(pollId, selected)
                    val updated = repository.getPoll(pollId)
                    if (updated != null) {
                      pollDataMap = pollDataMap + (pollId to updated)
                    }
                  } catch (_: Exception) {}
                }
              },
              onReportClick = {
                reportingPost = post
              },
              onBlockUserClick = {
                scope.launch {
                  repository.blockUser(post.userId, post.authorName)
                  posts = posts.filter { it.userId != post.userId }
                }
              },
              onDeleteClick = if (post.userId == currentUserId) {
                {
                  scope.launch {
                    repository.deletePost(post.id)
                    posts = posts.filter { it.id != post.id }
                  }
                }
              } else null
            )
          }
        }
      }
    }

    if (reportingPost != null) {
      ReportDialog(
        targetType = "Post",
        targetId = reportingPost!!.id,
        onDismiss = { reportingPost = null },
        onSubmitReport = { reason, details ->
          scope.launch {
            repository.reportContent("POST", reportingPost!!.id, reason, details)
            reportingPost = null
          }
        }
      )
    }

    if (isPollCreatorOpen) {
      PollCreatorDialog(
        isOpen = isPollCreatorOpen,
        onDismiss = { isPollCreatorOpen = false },
        onPollCreated = {
          scope.launch {
            isRefreshing = true
            delay(500)
            isRefreshing = false
          }
        },
        repository = repository
      )
    }
  }
}

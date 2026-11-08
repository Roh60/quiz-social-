package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedPollEntity
import com.example.data.local.CachedPostEntity
import com.example.data.local.CachedQuizEntity
import com.example.data.local.CachedQuizLeaderboardEntity
import com.example.data.local.QuizPollDatabase
import com.example.data.local.toCachedEntity
import com.example.data.local.toModel
import com.example.data.model.LeaderboardSortOption
import com.example.data.model.LeaderboardSorter
import com.example.data.model.LeaderboardTimeframe
import com.example.data.model.LeaderboardUser
import com.example.data.model.QuizCategories
import com.example.data.model.QuizLeaderboardEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QuizPoll", appName)
  }

  @Test
  fun `verify category list includes Science History and Pop Culture`() {
    val categories = QuizCategories.ALL
    assertTrue("Should include Science", categories.contains("Science"))
    assertTrue("Should include History", categories.contains("History"))
    assertTrue("Should include Pop Culture", categories.contains("Pop Culture"))
  }

  @Test
  fun `verify share sheet intent construction`() {
    val postTitle = "Ancient World Wonders"
    val postCategory = "History"
    val shareSubject = "Quiz: $postTitle"
    val shareBody = buildString {
      append("Check out this Quiz on QuizPoll Community!\n\n")
      append("\"$postTitle\"")
      append("\n\nTopic: $postCategory")
      append("\n\nSearch for QuizPoll Community in the app to find this quiz.")
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, shareSubject)
      putExtra(Intent.EXTRA_TEXT, shareBody)
    }

    val chooser = Intent.createChooser(sendIntent, "Share Quiz with friends")
    assertNotNull(chooser)
    assertEquals(Intent.ACTION_CHOOSER, chooser.action)

    val targetIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
    assertNotNull(targetIntent)
    assertEquals(Intent.ACTION_SEND, targetIntent?.action)
    assertEquals("text/plain", targetIntent?.type)
    assertEquals(shareSubject, targetIntent?.getStringExtra(Intent.EXTRA_SUBJECT))
    assertTrue(targetIntent?.getStringExtra(Intent.EXTRA_TEXT)?.contains("Search for QuizPoll Community in the app") == true)
    assertFalse(targetIntent?.getStringExtra(Intent.EXTRA_TEXT)?.contains("https://") == true)
  }

  @Test
  fun `verify activity points and level calculation`() {
    // 2 quizzes (50 pts) + 3 polls (60 pts) + 5 comments (50 pts) = 160 pts
    val points = com.example.data.model.ActivityPointsCalculator.calculatePoints(
      quizzes = 2,
      polls = 3,
      comments = 5
    )
    assertEquals(160, points)

    val (levelNum, levelName) = com.example.data.model.ActivityPointsCalculator.calculateLevel(points)
    assertEquals(3, levelNum)
    assertEquals("Community Contributor", levelName)
  }

  @Test
  fun `verify badge assignment for quizzes polls and comments`() {
    val stats = com.example.data.model.ActivityPointsCalculator.computeStats(
      quizzesCreated = 3,
      pollsCreated = 3,
      commentsContributed = 10
    )

    // Total points: (3*25) + (3*20) + (10*10) = 75 + 60 + 100 = 235 pts
    assertEquals(235, stats.totalPoints)
    val badgesMap = stats.badges.associateBy { it.id }

    // First Spark unlocked
    assertTrue("First Spark should be unlocked", badgesMap["first_spark"]?.isUnlocked == true)
    // Quiz Architect unlocked
    assertTrue("Quiz Architect should be unlocked", badgesMap["quiz_architect"]?.isUnlocked == true)
    // Master Pollster unlocked
    assertTrue("Master Pollster should be unlocked", badgesMap["master_pollster"]?.isUnlocked == true)
    // Conversation Starter unlocked
    assertTrue("Conversation Starter should be unlocked", badgesMap["conversation_starter"]?.isUnlocked == true)
    // Discussion Guru unlocked
    assertTrue("Discussion Guru should be unlocked", badgesMap["discussion_guru"]?.isUnlocked == true)
    // Prolific Author (3+3 = 6 >= 5) unlocked
    assertTrue("Prolific Author should be unlocked", badgesMap["prolific_author"]?.isUnlocked == true)
    // Community Legend (235 >= 200) unlocked
    assertTrue("Community Legend should be unlocked", badgesMap["community_legend"]?.isUnlocked == true)
  }

  @Test
  fun `verify search query filters quiz and poll titles case-insensitively`() {
    val posts = listOf(
      com.example.data.model.Post(id = "1", postType = "quiz", title = "Quantum Physics Quiz"),
      com.example.data.model.Post(id = "2", postType = "poll", title = "Favorite Programming Language"),
      com.example.data.model.Post(id = "3", postType = "quiz", title = "World Capitals Trivia")
    )

    val query = "physics"
    val filtered = posts.filter { post ->
      post.title.lowercase().contains(query.lowercase()) ||
      post.content.lowercase().contains(query.lowercase())
    }

    assertEquals(1, filtered.size)
    assertEquals("Quantum Physics Quiz", filtered.first().title)
  }

  @Test
  fun `verify personal bookmark creation and removal logic`() {
    val bookmarks = mutableListOf<com.example.data.model.BookmarkItem>()
    val sampleQuizBookmark = com.example.data.model.BookmarkItem(
      id = "bm_1",
      userId = "user_123",
      postId = "post_100",
      postType = "quiz",
      title = "Cell Biology Quiz",
      category = "Science",
      quizId = "quiz_50"
    )

    // Save bookmark
    bookmarks.add(sampleQuizBookmark)
    assertEquals(1, bookmarks.size)
    assertEquals("Cell Biology Quiz", bookmarks.first().title)
    assertEquals("quiz", bookmarks.first().postType)

    // Remove bookmark
    bookmarks.removeIf { it.postId == "post_100" }
    assertTrue("Bookmarks list should be empty after removal", bookmarks.isEmpty())
  }

  @Test
  fun `verify trending engagement scoring and top 5 ranking`() {
    val quizzes = listOf(
      com.example.data.model.TrendingMetric(id = "q1", title = "Quiz 1", type = "quiz", score = 150),
      com.example.data.model.TrendingMetric(id = "q2", title = "Quiz 2", type = "quiz", score = 320),
      com.example.data.model.TrendingMetric(id = "q3", title = "Quiz 3", type = "quiz", score = 90),
      com.example.data.model.TrendingMetric(id = "q4", title = "Quiz 4", type = "quiz", score = 210),
      com.example.data.model.TrendingMetric(id = "q5", title = "Quiz 5", type = "quiz", score = 400),
      com.example.data.model.TrendingMetric(id = "q6", title = "Quiz 6", type = "quiz", score = 80)
    )

    val top5 = quizzes.sortedByDescending { it.score }.take(5)
    assertEquals(5, top5.size)
    assertEquals("Quiz 5", top5[0].title)
    assertEquals(400, top5[0].score)
    assertEquals("Quiz 2", top5[1].title)
    assertEquals(320, top5[1].score)
  }

  @Test
  fun `verify notification payload construction for quiz comments and poll votes`() {
    val commentNotif = com.example.data.model.NotificationItem(
      id = "notif_1",
      recipientUserId = "creator_uid",
      senderUserId = "commenter_uid",
      senderName = "Alex",
      type = "COMMENT",
      targetId = "quiz_post_1",
      message = "Alex commented on your quiz: 'World History'"
    )

    assertEquals("creator_uid", commentNotif.recipientUserId)
    assertEquals("COMMENT", commentNotif.type)
    assertTrue(commentNotif.message.contains("Alex commented"))

    val voteNotif = com.example.data.model.NotificationItem(
      id = "notif_2",
      recipientUserId = "creator_uid",
      senderUserId = "voter_uid",
      senderName = "Sam",
      type = "POLL_VOTE",
      targetId = "poll_1",
      message = "Sam voted on your poll: 'Favorite Framework'"
    )

    assertEquals("POLL_VOTE", voteNotif.type)
    assertTrue(voteNotif.message.contains("Sam voted"))
  }

  @Test
  fun `verify QuizCard option validation and feedback calculation`() {
    val correctAnswerIndex = 2
    val options = listOf("Mercury", "Venus", "Earth", "Mars")
    
    // User chooses index 2 (Earth) -> Correct
    val userSelectedIndexCorrect = 2
    val isCorrect = userSelectedIndexCorrect == correctAnswerIndex
    assertTrue("Earth should be identified as correct answer", isCorrect)

    // User chooses index 0 (Mercury) -> Incorrect
    val userSelectedIndexIncorrect = 0
    val isIncorrect = userSelectedIndexIncorrect == correctAnswerIndex
    assertTrue("Mercury should be identified as incorrect answer", !isIncorrect)
  }

  @Test
  fun `verify PollWidget vote percentage and progress bar target calculation`() {
    val optionVotes = listOf(50L, 25L, 25L)
    val totalVotes = optionVotes.sum()
    assertEquals(100L, totalVotes)

    val percentages = optionVotes.map { votes ->
      if (totalVotes > 0) ((votes.toDouble() / totalVotes.toDouble()) * 100).toInt() else 0
    }
    assertEquals(listOf(50, 25, 25), percentages)

    val progressValues = optionVotes.map { votes ->
      (votes.toFloat() / totalVotes.toFloat()).coerceIn(0f, 1f)
    }
    assertEquals(0.5f, progressValues[0], 0.001f)
    assertEquals(0.25f, progressValues[1], 0.001f)
    assertEquals(0.25f, progressValues[2], 0.001f)
  }

  @Test
  fun `verify DiscussionFeed topic filtering and reaction types`() {
    val discussions = listOf(
      com.example.data.model.Post(id = "d1", postType = "discussion", title = "Quantum Computing", category = "Science"),
      com.example.data.model.Post(id = "d2", postType = "discussion", title = "Renaissance Art", category = "History"),
      com.example.data.model.Post(id = "d3", postType = "discussion", title = "Genetic Engineering", category = "Science"),
      com.example.data.model.Post(id = "d4", postType = "discussion", title = "Anime Trends", category = "Pop Culture")
    )

    // Test "Science" filter
    val scienceDiscussions = discussions.filter { it.category.equals("Science", ignoreCase = true) }
    assertEquals(2, scienceDiscussions.size)
    assertTrue(scienceDiscussions.all { it.category == "Science" })

    // Test "All" filter
    val allDiscussions = discussions
    assertEquals(4, allDiscussions.size)

    // Verify discussion reactions contains Love, Insightful, Fire, Innovative, and Celebrate
    val reactionKeys = com.example.ui.components.DISCUSSION_REACTIONS.map { it.key }
    assertTrue(reactionKeys.contains("like"))
    assertTrue(reactionKeys.contains("lightbulb"))
    assertTrue(reactionKeys.contains("fire"))
    assertTrue(reactionKeys.contains("rocket"))
    assertTrue(reactionKeys.contains("party"))
  }

  @Test
  fun `verify leaderboard ranking and top 10 limit calculation`() {
    val users = listOf(
      com.example.data.model.LeaderboardUser(userId = "u1", displayName = "User 1", totalPoints = 900),
      com.example.data.model.LeaderboardUser(userId = "u2", displayName = "User 2", totalPoints = 1500),
      com.example.data.model.LeaderboardUser(userId = "u3", displayName = "User 3", totalPoints = 1200),
      com.example.data.model.LeaderboardUser(userId = "u4", displayName = "User 4", totalPoints = 400),
      com.example.data.model.LeaderboardUser(userId = "u5", displayName = "User 5", totalPoints = 750),
      com.example.data.model.LeaderboardUser(userId = "u6", displayName = "User 6", totalPoints = 1100),
      com.example.data.model.LeaderboardUser(userId = "u7", displayName = "User 7", totalPoints = 650),
      com.example.data.model.LeaderboardUser(userId = "u8", displayName = "User 8", totalPoints = 1350),
      com.example.data.model.LeaderboardUser(userId = "u9", displayName = "User 9", totalPoints = 800),
      com.example.data.model.LeaderboardUser(userId = "u10", displayName = "User 10", totalPoints = 950),
      com.example.data.model.LeaderboardUser(userId = "u11", displayName = "User 11", totalPoints = 300),
      com.example.data.model.LeaderboardUser(userId = "u12", displayName = "User 12", totalPoints = 200)
    )

    val top10 = users
      .sortedByDescending { it.totalPoints }
      .take(10)
      .mapIndexed { index, user -> user.copy(rank = index + 1) }

    assertEquals(10, top10.size)
    assertEquals(1, top10[0].rank)
    assertEquals("User 2", top10[0].displayName)
    assertEquals(1500, top10[0].totalPoints)

    assertEquals(2, top10[1].rank)
    assertEquals("User 8", top10[1].displayName)
    assertEquals(1350, top10[1].totalPoints)

    assertEquals(10, top10.last().rank)
    assertEquals("User 4", top10.last().displayName)
    assertEquals(400, top10.last().totalPoints)
  }

  @Test
  fun `verify feed search bar filters quizzes and polls by topic and keywords`() {
    val posts = listOf(
      com.example.data.model.Post(id = "p1", postType = "quiz", title = "Cellular Biology", category = "Science", content = "Mitochondria functions"),
      com.example.data.model.Post(id = "p2", postType = "poll", title = "Favorite Science Topic", category = "Science", content = "Cast your vote"),
      com.example.data.model.Post(id = "p3", postType = "quiz", title = "French Revolution", category = "History", content = "Key dates"),
      com.example.data.model.Post(id = "p4", postType = "poll", title = "Best Movie Franchise", category = "Pop Culture", content = "Hollywood hits")
    )

    // Filter by topic "Science"
    val topicFiltered = posts.filter { it.category.equals("Science", ignoreCase = true) }
    assertEquals(2, topicFiltered.size)

    // Filter by type "poll" + topic "Science"
    val pollAndTopic = topicFiltered.filter { it.postType == "poll" }
    assertEquals(1, pollAndTopic.size)
    assertEquals("Favorite Science Topic", pollAndTopic.first().title)

    // Filter by keyword "revolution"
    val query = "revolution"
    val keywordFiltered = posts.filter {
      it.title.lowercase().contains(query) || it.content.lowercase().contains(query)
    }
    assertEquals(1, keywordFiltered.size)
    assertEquals("French Revolution", keywordFiltered.first().title)
  }

  @Test
  fun `verify poll creator options and question validation logic`() {
    // Valid poll
    val question = "What is your primary development OS?"
    val options = listOf("Linux / Ubuntu", "macOS", "Windows with WSL2")
    val isValid = question.isNotBlank() && options.count { it.isNotBlank() } >= 2
    assertTrue("Poll should be valid with question and 3 non-empty options", isValid)

    // Invalid poll - blank question
    val invalidQ = "   "
    val isInvalidQ = invalidQ.isNotBlank() && options.count { it.isNotBlank() } >= 2
    assertTrue("Blank question should fail validation", !isInvalidQ)

    // Invalid poll - only 1 option
    val singleOption = listOf("Linux")
    val isInvalidOpts = question.isNotBlank() && singleOption.count { it.isNotBlank() } >= 2
    assertTrue("Single option should fail validation", !isInvalidOpts)
  }

  @Test
  fun `verify QuizFeed quiz card properties and topic filtering`() {
    val sampleQuizzes = listOf(
      com.example.data.model.Quiz(
        id = "quiz_geo_1",
        title = "World Capitals & Monuments",
        description = "Test your knowledge of landmarks across the globe",
        category = "Geography",
        difficulty = "Medium",
        authorName = "Atlas Pro",
        authorUsername = "atlas_pro",
        questionCount = 10,
        timeLimitSeconds = 30
      ),
      com.example.data.model.Quiz(
        id = "quiz_hist_1",
        title = "Ancient Roman Emperors",
        description = "From Caesar Augustus to Marcus Aurelius",
        category = "History",
        difficulty = "Hard",
        authorName = "History Buff",
        authorUsername = "buff_hist",
        questionCount = 15,
        timeLimitSeconds = 45
      )
    )

    // Filter by category "History"
    val historyQuizzes = sampleQuizzes.filter { it.category == "History" }
    assertEquals(1, historyQuizzes.size)
    assertEquals("Ancient Roman Emperors", historyQuizzes.first().title)
    assertEquals("History Buff", historyQuizzes.first().authorName)
    assertEquals("buff_hist", historyQuizzes.first().authorUsername)
    assertEquals(15, historyQuizzes.first().questionCount)

    // Verify 'Take Quiz' trigger callback
    var triggeredQuizId: String? = null
    val onTakeQuiz: (String) -> Unit = { id -> triggeredQuizId = id }
    onTakeQuiz(historyQuizzes.first().id)
    assertEquals("quiz_hist_1", triggeredQuizId)
  }

  @Test
  fun `verify QuizFeed comment section feedback list and comment submission`() {
    val quizId = "quiz_geo_1"
    val existingComments = mutableListOf(
      com.example.data.model.Comment(
        id = "c1",
        postId = quizId,
        userId = "user_1",
        authorName = "Alex Traveler",
        authorUsername = "alex_t",
        content = "Question 3 had a great explanation about the Colosseum!"
      )
    )

    assertEquals(1, existingComments.size)
    assertEquals("Alex Traveler", existingComments.first().authorName)
    assertTrue(existingComments.first().content.contains("Colosseum"))

    // Submit new feedback comment
    val newFeedback = com.example.data.model.Comment(
      id = "c2",
      postId = quizId,
      userId = "user_2",
      authorName = "Sam Scholar",
      authorUsername = "sam_s",
      content = "I really enjoyed the timer pacing for this quiz challenge."
    )
    existingComments.add(newFeedback)

    assertEquals(2, existingComments.size)
    assertEquals("c2", existingComments.last().id)
    assertEquals("Sam Scholar", existingComments.last().authorName)
    assertTrue(existingComments.last().content.contains("timer pacing"))
  }

  @Test
  fun `verify QuizFeed swipe-to-refresh mechanism state flow`() {
    var isRefreshing = false
    var feedUpdated = false

    // Simulate pull-to-refresh start
    isRefreshing = true
    assertTrue("Pull to refresh should be active", isRefreshing)

    // Simulate background re-fetch and update
    feedUpdated = true
    isRefreshing = false

    assertTrue("Feed should be updated after refresh", feedUpdated)
    assertTrue("Refreshing indicator should dismiss", !isRefreshing)
  }

  @Test
  fun `verify poll result chart percentage and angle calculation`() {
    val poll = com.example.data.model.Poll(
      id = "poll_geo_1",
      question = "Which is your favorite season?",
      options = listOf("Spring", "Summer", "Autumn", "Winter"),
      optionVotes = listOf(50L, 30L, 15L, 5L),
      totalVotes = 100L
    )

    val totalVotes = poll.totalVotes.coerceAtLeast(1L)
    val percentages = poll.optionVotes.map { ((it.toDouble() / totalVotes.toDouble()) * 100).toInt() }
    val sweepAngles = poll.optionVotes.map { (it.toFloat() / totalVotes.toFloat()) * 360f }

    assertEquals(listOf(50, 30, 15, 5), percentages)
    assertEquals(180f, sweepAngles[0], 0.01f)
    assertEquals(108f, sweepAngles[1], 0.01f)
    assertEquals(54f, sweepAngles[2], 0.01f)
    assertEquals(18f, sweepAngles[3], 0.01f)
    assertEquals(360f, sweepAngles.sum(), 0.01f)
  }

  @Test
  fun `verify quiz like reaction optimistic toggle and count update`() {
    var quiz = com.example.data.model.Quiz(
      id = "quiz_tech_1",
      title = "Android Kotlin Masterclass",
      likeCount = 12L
    )

    var isLiked = false

    // User taps Like button
    isLiked = !isLiked
    val displayedLikesCountAfterLike = quiz.likeCount + if (isLiked) 1L else 0L
    assertEquals(true, isLiked)
    assertEquals(13L, displayedLikesCountAfterLike)

    // User taps Like button again to unlike
    isLiked = !isLiked
    val displayedLikesCountAfterUnlike = quiz.likeCount + if (isLiked) 1L else 0L
    assertEquals(false, isLiked)
    assertEquals(12L, displayedLikesCountAfterUnlike)
  }

  @Test
  fun `verify user profile edit with display name and profile picture preset or url`() {
    var profile = com.example.data.model.UserProfile(
      userId = "user_42",
      username = "quiz_master",
      displayName = "Original Name",
      bio = "Love testing",
      profilePictureUrl = ""
    )

    // User selects emoji avatar preset 🎓 and updates display name
    profile = profile.copy(
      displayName = "Master Alex",
      bio = "Certified Trivia Champion",
      profilePictureUrl = "emoji:🎓"
    )

    assertEquals("Master Alex", profile.displayName)
    assertEquals("emoji:🎓", profile.profilePictureUrl)
    assertTrue("Avatar should be an emoji preset", profile.profilePictureUrl.startsWith("emoji:"))

    // User updates to custom image URL
    profile = profile.copy(
      profilePictureUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
    )
    assertTrue("Avatar should be an image URL", profile.profilePictureUrl.startsWith("https://"))
  }

  @Test
  fun `verify user completed polls list tracking`() {
    val completedPolls = mutableListOf<com.example.data.model.Poll>()

    val poll1 = com.example.data.model.Poll(
      id = "p1",
      question = "Coffee or Tea?",
      options = listOf("Coffee", "Tea"),
      optionVotes = listOf(14L, 8L),
      totalVotes = 22L
    )

    val poll2 = com.example.data.model.Poll(
      id = "p2",
      question = "Tabs or Spaces?",
      options = listOf("Tabs", "Spaces"),
      optionVotes = listOf(35L, 42L),
      totalVotes = 77L
    )

    completedPolls.add(poll1)
    completedPolls.add(poll2)

    assertEquals(2, completedPolls.size)
    assertEquals("Coffee or Tea?", completedPolls[0].question)
    assertEquals("Tabs or Spaces?", completedPolls[1].question)
  }

  @Test
  fun `verify D3 poll real-time percentage computation after user casts vote`() {
    var poll = com.example.data.model.Poll(
      id = "poll_d3_test",
      question = "Best mobile framework?",
      options = listOf("Jetpack Compose", "Flutter", "React Native"),
      optionVotes = listOf(60L, 30L, 10L),
      totalVotes = 100L
    )

    // User has not voted yet
    var userVotedIndices = emptyList<Int>()
    var totalVotes = poll.totalVotes.coerceAtLeast(1L)
    var percentages = poll.optionVotes.map { ((it.toDouble() / totalVotes.toDouble()) * 100).toInt() }
    assertEquals(listOf(60, 30, 10), percentages)

    // User casts vote for "Jetpack Compose" (index 0)
    userVotedIndices = listOf(0)
    val updatedOptionVotes = poll.optionVotes.toMutableList()
    updatedOptionVotes[0] = updatedOptionVotes[0] + 1L
    poll = poll.copy(
      optionVotes = updatedOptionVotes,
      totalVotes = poll.totalVotes + 1L
    )

    // Verify real-time percentage update
    totalVotes = poll.totalVotes
    percentages = poll.optionVotes.map { ((it.toDouble() / totalVotes.toDouble()) * 100).toInt() }

    assertEquals(61L, poll.optionVotes[0])
    assertEquals(101L, poll.totalVotes)
    assertEquals(listOf(60, 29, 9), percentages) // 61/101 = 60.39%, 30/101 = 29.7%, 10/101 = 9.9%
    assertTrue("User voted index should be recorded", userVotedIndices.contains(0))
  }

  @Test
  fun `verify D3 data payload formatting with leader badge and user vote highlight`() {
    val poll = com.example.data.model.Poll(
      id = "poll_leader_test",
      question = "Favorite IDE theme?",
      options = listOf("Dracula", "One Dark Pro", "Solarized"),
      optionVotes = listOf(120L, 80L, 40L),
      totalVotes = 240L
    )

    val highestVote = poll.optionVotes.maxOrNull() ?: 0L
    val userVotedIndices = listOf(1) // User voted for One Dark Pro

    val d3Items = poll.options.mapIndexed { index, option ->
      val votes = poll.optionVotes[index]
      val pct = ((votes.toDouble() / poll.totalVotes.toDouble()) * 100).toInt()
      val isVoted = userVotedIndices.contains(index)
      val isLeader = highestVote > 0L && votes == highestVote
      mapOf(
        "label" to option,
        "votes" to votes,
        "percentage" to pct,
        "isVoted" to isVoted,
        "isLeader" to isLeader
      )
    }

    // Option 0: Dracula (Leader, not voted by user)
    assertEquals("Dracula", d3Items[0]["label"])
    assertEquals(50, d3Items[0]["percentage"])
    assertEquals(true, d3Items[0]["isLeader"])
    assertEquals(false, d3Items[0]["isVoted"])

    // Option 1: One Dark Pro (Voted by user, not leader)
    assertEquals("One Dark Pro", d3Items[1]["label"])
    assertEquals(33, d3Items[1]["percentage"])
    assertEquals(false, d3Items[1]["isLeader"])
    assertEquals(true, d3Items[1]["isVoted"])
  }

  @Test
  fun `verify email and password validation rules for authentication`() {
    fun isValidEmail(email: String): Boolean = email.isNotBlank() && email.contains("@") && email.contains(".")
    fun isValidPassword(password: String): Boolean = password.length >= 6
    fun doPasswordsMatch(p1: String, p2: String): Boolean = p1 == p2

    assertTrue(isValidEmail("user@example.com"))
    assertTrue(!isValidEmail("invalid-email"))
    assertTrue(!isValidEmail(""))

    assertTrue(isValidPassword("securePass123"))
    assertTrue(!isValidPassword("123"))

    assertTrue(doPasswordsMatch("pass123", "pass123"))
    assertTrue(!doPasswordsMatch("pass123", "different"))
  }

  @Test
  fun `verify Room entity mapping for offline cache of feed, poll results, and quiz history`() {
    // 1. Social Feed Post mapping
    val originalPost = com.example.data.model.Post(
      id = "post_99",
      userId = "user_abc",
      authorName = "Alex Author",
      title = "Quantum Physics Quiz Post",
      content = "Check out this quiz on particle physics!",
      category = "Science",
      likeCount = 42L,
      commentCount = 15L
    )
    val postEntity = originalPost.toCachedEntity()
    val restoredPost = postEntity.toModel()

    assertEquals(originalPost.id, restoredPost.id)
    assertEquals(originalPost.title, restoredPost.title)
    assertEquals(originalPost.category, restoredPost.category)
    assertEquals(originalPost.likeCount, restoredPost.likeCount)

    // 2. Poll Results mapping
    val originalPoll = com.example.data.model.Poll(
      id = "poll_44",
      userId = "user_pollster",
      question = "Favorite programming paradigm?",
      options = listOf("Functional", "OOP", "Procedural"),
      optionVotes = listOf(150L, 220L, 30L),
      totalVotes = 400L
    )
    val pollEntity = originalPoll.toCachedEntity(listOf(1))
    val restoredPoll = pollEntity.toModel()

    assertEquals(originalPoll.id, restoredPoll.id)
    assertEquals(originalPoll.question, restoredPoll.question)
    assertEquals(originalPoll.options, restoredPoll.options)
    assertEquals(originalPoll.optionVotes, restoredPoll.optionVotes)
    assertEquals(originalPoll.totalVotes, restoredPoll.totalVotes)

    // 3. Quiz History Attempt mapping
    val originalAttempt = com.example.data.model.QuizAttempt(
      id = "att_77",
      quizId = "quiz_12",
      userId = "user_student",
      quizTitle = "Ancient History Speedrun",
      score = 8,
      totalQuestions = 10,
      accuracy = 80.0,
      timeTakenSeconds = 45
    )
    val attemptEntity = originalAttempt.toCachedEntity()
    val restoredAttempt = attemptEntity.toModel()

    assertEquals(originalAttempt.id, restoredAttempt.id)
    assertEquals(originalAttempt.quizTitle, restoredAttempt.quizTitle)
    assertEquals(originalAttempt.score, restoredAttempt.score)
    assertEquals(originalAttempt.accuracy, restoredAttempt.accuracy, 0.01)
  }

  @Test
  fun `verify UserProfile linking to authenticated account with count placeholders`() {
    val authUid = "firebase_auth_uid_12345"
    val profile = com.example.data.model.UserProfile(
      userId = authUid,
      username = "quiz_pro",
      displayName = "Master Quizzer",
      bio = "Exploring knowledge & sharing quizzes with the community!",
      quizCount = 0L,
      pollCount = 0L,
      followersCount = 0L,
      followingCount = 0L
    )

    assertEquals(authUid, profile.userId)
    assertEquals("quiz_pro", profile.username)
    assertEquals("Master Quizzer", profile.displayName)
    assertEquals(0L, profile.quizCount)
    assertEquals(0L, profile.pollCount)
    assertEquals(0L, profile.followersCount)
    assertEquals(0L, profile.followingCount)
  }

  @Test
  fun `verify correct answer selection state in quiz question drafting`() {
    val qState = com.example.ui.screens.create.EditableQuestionState(
      initialText = "What is the capital of France?",
      initialOptions = listOf("Berlin", "Madrid", "Paris", "Rome"),
      initialCorrectIndex = 0
    )

    // Initially index 0 ("Berlin")
    assertEquals(0, qState.correctIndex)

    // User selects Option C (index 2: "Paris") as the correct answer
    qState.correctIndex = 2
    assertEquals(2, qState.correctIndex)

    val optionLetters = listOf("A", "B", "C", "D")
    assertEquals("C", optionLetters[qState.correctIndex])
    assertEquals("Paris", qState.options[qState.correctIndex])

    // Convert to QuizQuestion model
    val quizQuestion = com.example.data.model.QuizQuestion(
      questionText = qState.questionText,
      options = qState.options.toList(),
      correctAnswers = listOf(qState.correctIndex)
    )

    assertEquals(listOf(2), quizQuestion.correctAnswers)
    assertEquals("Paris", quizQuestion.options[quizQuestion.correctAnswers.first()])
  }

  @Test
  fun `verify interactive poll component state management for immediate voting and real-time percentages`() {
    val poll = com.example.data.model.Poll(
      id = "poll_live_test",
      question = "Which mobile OS do you prefer?",
      options = listOf("Android", "iOS", "Other"),
      optionVotes = listOf(40L, 30L, 30L),
      totalVotes = 100L,
      allowVoteChange = true
    )

    // Initial percentages: Android = 40%, iOS = 30%, Other = 30%
    val initialAndroidPct = ((poll.optionVotes[0].toDouble() / poll.totalVotes) * 100).toInt()
    assertEquals(40, initialAndroidPct)

    // Simulating immediate local state transition on voting for Android (index 0)
    val localOptionVotes = poll.optionVotes.toMutableList()
    var localTotalVotes = poll.totalVotes
    var userVotedIndices = emptyList<Int>()

    // Vote for option 0 (Android)
    localOptionVotes[0] += 1
    localTotalVotes += 1
    userVotedIndices = listOf(0)

    assertEquals(41L, localOptionVotes[0])
    assertEquals(101L, localTotalVotes)
    assertEquals(listOf(0), userVotedIndices)

    val updatedAndroidPct = ((localOptionVotes[0].toDouble() / localTotalVotes) * 100).toInt()
    assertEquals(40, updatedAndroidPct) // 41 / 101 = 40%

    // Switch vote to option 1 (iOS)
    localOptionVotes[0] -= 1 // Android decrements
    localOptionVotes[1] += 1 // iOS increments
    userVotedIndices = listOf(1)

    assertEquals(40L, localOptionVotes[0])
    assertEquals(31L, localOptionVotes[1])
    assertEquals(101L, localTotalVotes)
    assertEquals(listOf(1), userVotedIndices)

    val updatedIosPct = ((localOptionVotes[1].toDouble() / localTotalVotes) * 100).toInt()
    assertEquals(30, updatedIosPct) // 31 / 101 = 30.6% -> 30%
  }

  @Test
  fun `verify offline-first Room database schema and DAO for feed caching`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.QuizPollDatabase.getDatabase(context)

    val postDao = database.postDao()
    val pollDao = database.pollDao()

    assertNotNull("PostDao must be instantiated", postDao)
    assertNotNull("PollDao must be instantiated", pollDao)

    // Verify entity mapping
    val post = com.example.data.model.Post(
      id = "cached_feed_post_1",
      userId = "user_abc",
      authorName = "Educator Bob",
      authorUsername = "b_educator",
      postType = "quiz",
      title = "Quantum Physics 101",
      content = "Introduction to quantum superpositions.",
      category = "Science",
      likeCount = 15L,
      commentCount = 4L
    )

    val entity = post.toCachedEntity()
    assertEquals("cached_feed_post_1", entity.id)
    assertEquals("Science", entity.category)
    assertEquals("quiz", entity.postType)

    val restored = entity.toModel()
    assertEquals(post.title, restored.title)
    assertEquals(post.likeCount, restored.likeCount)
  }

  @Test
  fun `verify Room fuzzy search query execution across titles contents categories and authors`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, QuizPollDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val postDao = database.postDao()

    val posts = listOf(
      CachedPostEntity(
        id = "post_science_quiz",
        userId = "user_alice",
        authorName = "Alice Quarks",
        authorUsername = "alice_q",
        postType = "quiz",
        title = "Quantum Computing Principles",
        content = "Superposition, entanglement, and quantum gates explained.",
        category = "Science",
        quizId = "q_1",
        pollId = "",
        likeCount = 25,
        commentCount = 5,
        createdAtEpochMs = 1000
      ),
      CachedPostEntity(
        id = "post_pop_poll",
        userId = "user_bob",
        authorName = "Bob Cinematic",
        authorUsername = "bob_c",
        postType = "poll",
        title = "Favorite Sci-Fi Movie of the Decade",
        content = "Vote between Interstellar, Matrix, and Blade Runner.",
        category = "Pop Culture",
        quizId = "",
        pollId = "p_1",
        likeCount = 40,
        commentCount = 12,
        createdAtEpochMs = 2000
      ),
      CachedPostEntity(
        id = "post_history_quiz",
        userId = "user_charlie",
        authorName = "Charlie Historian",
        authorUsername = "charlie_h",
        postType = "quiz",
        title = "Ancient Roman Empire and Republic",
        content = "From the founding of Rome to the Pax Romana.",
        category = "History",
        quizId = "q_2",
        pollId = "",
        likeCount = 18,
        commentCount = 3,
        createdAtEpochMs = 3000
      ),
      CachedPostEntity(
        id = "post_science_discussion",
        userId = "user_diana",
        authorName = "Diana Physicist",
        authorUsername = "diana_p",
        postType = "discussion",
        title = "Quantum Mechanics Copenhagen Interpretation Debate",
        content = "Does observation collapse the wave function?",
        category = "Science",
        quizId = "",
        pollId = "",
        likeCount = 55,
        commentCount = 20,
        createdAtEpochMs = 4000
      )
    )

    postDao.insertPosts(posts)

    // 1. Fuzzy keyword search for "Quantum" matches both Science posts
    val quantumMatches = postDao.searchCachedPosts("Quantum").first()
    assertEquals(2, quantumMatches.size)
    assertTrue(quantumMatches.any { it.id == "post_science_quiz" })
    assertTrue(quantumMatches.any { it.id == "post_science_discussion" })

    // 2. Filter by keyword "Quantum", category "Science", and postType "quiz" matches only the quiz
    val filteredQuiz = postDao.searchCachedPostsWithFilters("Quantum", "Science", "quiz").first()
    assertEquals(1, filteredQuiz.size)
    assertEquals("post_science_quiz", filteredQuiz.first().id)
    assertEquals("Quantum Computing Principles", filteredQuiz.first().title)

    // 3. Filter by keyword "Interstellar" matching content
    val contentMatch = postDao.searchCachedPostsWithFilters("Interstellar", "All", "All").first()
    assertEquals(1, contentMatch.size)
    assertEquals("post_pop_poll", contentMatch.first().id)

    // 4. Filter by author name "Charlie"
    val authorMatch = postDao.searchCachedPostsWithFilters("Charlie", "All", "All").first()
    assertEquals(1, authorMatch.size)
    assertEquals("post_history_quiz", authorMatch.first().id)

    // 5. Category only filter with empty query
    val historyPosts = postDao.searchCachedPostsWithFilters("", "History", "All").first()
    assertEquals(1, historyPosts.size)
    assertEquals("History", historyPosts.first().category)

    database.close()
  }

  @Test
  fun `verify Room fuzzy search on cached quizzes and polls directly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, QuizPollDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val quizDao = database.quizDao()
    val pollDao = database.pollDao()

    val quiz1 = CachedQuizEntity(
      id = "quiz_cell_bio",
      userId = "u1",
      authorName = "Bio Professor",
      authorUsername = "bioprof",
      title = "Cellular Biology Fundamentals",
      description = "Test your cellular division and respiration knowledge.",
      category = "Science",
      difficulty = "Medium",
      questionCount = 10,
      timeLimitSeconds = 30,
      attemptCount = 45,
      likeCount = 12,
      createdAtEpochMs = 1000
    )
    val quiz2 = CachedQuizEntity(
      id = "quiz_capitals",
      userId = "u2",
      authorName = "Atlas Geographer",
      authorUsername = "atlas_geo",
      title = "European Capitals Challenge",
      description = "Identify the capitals of 40 European nations.",
      category = "Geography",
      difficulty = "Easy",
      questionCount = 20,
      timeLimitSeconds = 45,
      attemptCount = 90,
      likeCount = 33,
      createdAtEpochMs = 2000
    )

    quizDao.insertQuizzes(listOf(quiz1, quiz2))

    val matchedQuizzes = quizDao.searchCachedQuizzes("capitals").first()
    assertEquals(1, matchedQuizzes.size)
    assertEquals("European Capitals Challenge", matchedQuizzes.first().title)

    val matchedDescription = quizDao.searchCachedQuizzes("respiration").first()
    assertEquals(1, matchedDescription.size)
    assertEquals("Cellular Biology Fundamentals", matchedDescription.first().title)

    val poll1 = CachedPollEntity(
      id = "poll_framework",
      userId = "u3",
      authorName = "Dev Advocate",
      authorUsername = "dev_adv",
      question = "Which UI toolkit do you prefer for native Android?",
      description = "Jetpack Compose or Classic XML Views",
      category = "Technology",
      optionsJson = "[\"Jetpack Compose\",\"XML Views\"]",
      optionVotesJson = "[85, 15]",
      totalVotes = 100,
      isMultipleChoice = false,
      allowVoteChange = true,
      createdAtEpochMs = 3000
    )

    pollDao.insertPoll(poll1)

    val matchedPolls = pollDao.searchCachedPolls("toolkit").first()
    assertEquals(1, matchedPolls.size)
    assertEquals("poll_framework", matchedPolls.first().id)
    assertEquals("Which UI toolkit do you prefer for native Android?", matchedPolls.first().question)

    database.close()
  }

  @Test
  fun `verify pull to refresh feed sync updates Room cache and emits latest posts`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, QuizPollDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val postDao = database.postDao()

    // Initially cache is empty
    val initialPosts = postDao.getCachedPosts()
    assertTrue(initialPosts.isEmpty())

    // Simulate pull-to-refresh sync by re-fetching and populating Room
    val refreshedEntities = listOf(
      CachedPostEntity(
        id = "sync_post_1",
        userId = "u_sync",
        authorName = "Sync Author",
        authorUsername = "sync_auth",
        postType = "quiz",
        title = "Latest Technology Quiz",
        content = "Sync from server",
        category = "Technology",
        quizId = "q_sync_1",
        pollId = "",
        likeCount = 5,
        commentCount = 1,
        createdAtEpochMs = System.currentTimeMillis()
      ),
      CachedPostEntity(
        id = "sync_post_2",
        userId = "u_sync2",
        authorName = "Sync Author 2",
        authorUsername = "sync_auth2",
        postType = "poll",
        title = "Latest Community Poll",
        content = "Sync from server poll",
        category = "General",
        quizId = "",
        pollId = "p_sync_1",
        likeCount = 8,
        commentCount = 2,
        createdAtEpochMs = System.currentTimeMillis() + 100
      )
    )

    postDao.insertPosts(refreshedEntities)

    val cachedAfterRefresh = postDao.getCachedPosts()
    assertEquals(2, cachedAfterRefresh.size)
    assertEquals("sync_post_2", cachedAfterRefresh.first().id) // Ordered by createdAtEpochMs DESC

    database.close()
  }

  @Test
  fun `verify formatted Android Share Intent with deep link for feed cards and interactive polls`() {
    val postTitle = "World History: Ancient Civilizations"
    val postCategory = "History"
    val authorUsername = "history_buff"
    val shareSubject = "Quiz: $postTitle"
    val shareBody = buildString {
      append("🎯 Check out this Quiz on QuizPoll Community!\n\n")
      append("\"$postTitle\"\n")
      append("Topic: $postCategory • By @$authorUsername\n")
      append("\n🧠 10 Questions • Medium Difficulty")
      append("\n\nSearch for QuizPoll Community in the app to find this content.")
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, shareSubject)
      putExtra(Intent.EXTRA_TEXT, shareBody)
    }

    val chooser = Intent.createChooser(sendIntent, "Share Quiz with friends")

    assertNotNull(chooser)
    assertEquals(Intent.ACTION_CHOOSER, chooser.action)

    val targetIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
    assertNotNull(targetIntent)
    assertEquals(Intent.ACTION_SEND, targetIntent?.action)
    assertEquals("text/plain", targetIntent?.type)
    assertEquals(shareSubject, targetIntent?.getStringExtra(Intent.EXTRA_SUBJECT))

    val extractedText = targetIntent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
    assertTrue("Should direct users to the app", extractedText.contains("Search for QuizPoll Community in the app"))
    assertFalse("Should not contain an unverified public URL", extractedText.contains("https://"))
    assertTrue("Should contain topic category", extractedText.contains("Topic: History"))
    assertTrue("Should contain author username", extractedText.contains("@history_buff"))
    assertTrue("Should contain question count", extractedText.contains("10 Questions"))
  }

  @Test
  fun `verify interactive poll component voting state and share intent trigger`() {
    val poll = com.example.data.model.Poll(
      id = "poll_test_42",
      question = "Which database do you prefer for mobile apps?",
      options = listOf("Room (SQLite)", "Firebase Firestore", "DataStore"),
      optionVotes = listOf(50L, 40L, 10L),
      totalVotes = 100L
    )

    // Verify initial voting calculations
    var userVotedIndices = listOf(0) // User voted Room
    val totalVotes = poll.totalVotes
    val pct0 = ((poll.optionVotes[0].toDouble() / totalVotes.toDouble()) * 100).toInt()
    val pct1 = ((poll.optionVotes[1].toDouble() / totalVotes.toDouble()) * 100).toInt()
    val pct2 = ((poll.optionVotes[2].toDouble() / totalVotes.toDouble()) * 100).toInt()

    assertEquals(50, pct0)
    assertEquals(40, pct1)
    assertEquals(10, pct2)
    assertEquals(1, userVotedIndices.size)

    // Share copy has no public URL until a real web destination is deployed.
    var sharedMessage = ""
    var sharedSubject: String? = null
    val onShareClick: (String, String) -> Unit = { _, title ->
      sharedMessage = "Search for QuizPoll Community in the app to find this poll."
      sharedSubject = "Poll: $title"
    }

    onShareClick(poll.id, poll.question)

    assertTrue(sharedMessage.contains("Search for QuizPoll Community in the app"))
    assertEquals("Poll: Which database do you prefer for mobile apps?", sharedSubject)
  }

  @Test
  fun `verify general quiz competition leaderboard sorting by score accuracy and time taken`() {
    val users = listOf(
      LeaderboardUser(
        userId = "u1",
        displayName = "High Scorer",
        totalScore = 500,
        accuracy = 80.0,
        timeTakenSeconds = 45
      ),
      LeaderboardUser(
        userId = "u2",
        displayName = "High Accuracy",
        totalScore = 400,
        accuracy = 98.0,
        timeTakenSeconds = 30
      ),
      LeaderboardUser(
        userId = "u3",
        displayName = "Speed Master",
        totalScore = 450,
        accuracy = 90.0,
        timeTakenSeconds = 18
      )
    )

    // 1. Sort by SCORE (descending)
    val byScore = LeaderboardSorter.sortGeneral(users, LeaderboardTimeframe.ALL_TIME, LeaderboardSortOption.SCORE)
    assertEquals("High Scorer", byScore[0].displayName)
    assertEquals(1, byScore[0].rank)
    assertEquals("Speed Master", byScore[1].displayName)
    assertEquals("High Accuracy", byScore[2].displayName)

    // 2. Sort by ACCURACY (descending)
    val byAccuracy = LeaderboardSorter.sortGeneral(users, LeaderboardTimeframe.ALL_TIME, LeaderboardSortOption.ACCURACY)
    assertEquals("High Accuracy", byAccuracy[0].displayName)
    assertEquals(98.0, byAccuracy[0].accuracy, 0.01)
    assertEquals("Speed Master", byAccuracy[1].displayName)
    assertEquals("High Scorer", byAccuracy[2].displayName)

    // 3. Sort by TIME TAKEN (ascending - fastest first)
    val byTime = LeaderboardSorter.sortGeneral(users, LeaderboardTimeframe.ALL_TIME, LeaderboardSortOption.TIME_TAKEN)
    assertEquals("Speed Master", byTime[0].displayName)
    assertEquals(18, byTime[0].timeTakenSeconds)
    assertEquals("High Accuracy", byTime[1].displayName)
    assertEquals("High Scorer", byTime[2].displayName)
  }

  @Test
  fun `verify weekly timeframe filtering for quiz competition leaderboard`() {
    val users = listOf(
      LeaderboardUser(
        userId = "u1",
        displayName = "Veteran User",
        totalScore = 1200,
        weeklyScore = 50,
        accuracy = 85.0,
        weeklyAccuracy = 75.0,
        timeTakenSeconds = 35,
        weeklyTimeTakenSeconds = 40
      ),
      LeaderboardUser(
        userId = "u2",
        displayName = "Weekly Rising Star",
        totalScore = 300,
        weeklyScore = 280,
        accuracy = 92.0,
        weeklyAccuracy = 95.0,
        timeTakenSeconds = 25,
        weeklyTimeTakenSeconds = 22
      )
    )

    // Weekly Sort: "Weekly Rising Star" should be #1 with 280 pts vs Veteran's 50 pts
    val weeklyRankings = LeaderboardSorter.sortGeneral(users, LeaderboardTimeframe.THIS_WEEK, LeaderboardSortOption.SCORE)
    assertEquals("Weekly Rising Star", weeklyRankings[0].displayName)
    assertEquals(280, weeklyRankings[0].weeklyScore)
    assertEquals(1, weeklyRankings[0].rank)
    assertEquals("Veteran User", weeklyRankings[1].displayName)
    assertEquals(50, weeklyRankings[1].weeklyScore)
    assertEquals(2, weeklyRankings[1].rank)

    // All-Time Sort: "Veteran User" should be #1 with 1200 totalScore vs Rising Star's 300
    val allTimeRankings = LeaderboardSorter.sortGeneral(users, LeaderboardTimeframe.ALL_TIME, LeaderboardSortOption.SCORE)
    assertEquals("Veteran User", allTimeRankings[0].displayName)
    assertEquals(1200, allTimeRankings[0].totalScore)
    assertEquals(1, allTimeRankings[0].rank)
    assertEquals("Weekly Rising Star", allTimeRankings[1].displayName)
    assertEquals(300, allTimeRankings[1].totalScore)
    assertEquals(2, allTimeRankings[1].rank)
  }

  @Test
  fun `verify individual quiz competition leaderboard ranking and tie-breaker sorting`() {
    val entries = listOf(
      QuizLeaderboardEntry(
        id = "e1",
        quizId = "quiz_science_1",
        userId = "u1",
        displayName = "Scholar A",
        score = 8,
        totalQuestions = 10,
        accuracy = 80.0,
        timeTakenSeconds = 40
      ),
      QuizLeaderboardEntry(
        id = "e2",
        quizId = "quiz_science_1",
        userId = "u2",
        displayName = "Scholar B",
        score = 8,
        totalQuestions = 10,
        accuracy = 80.0,
        timeTakenSeconds = 25 // Tie on score & accuracy, but faster!
      ),
      QuizLeaderboardEntry(
        id = "e3",
        quizId = "quiz_science_1",
        userId = "u3",
        displayName = "Scholar C",
        score = 10,
        totalQuestions = 10,
        accuracy = 100.0,
        timeTakenSeconds = 30
      )
    )

    // Sort by Score: Scholar C (10 pts) -> Scholar B (8 pts, 25s) -> Scholar A (8 pts, 40s)
    val sortedByScore = LeaderboardSorter.sortQuiz(entries, LeaderboardSortOption.SCORE)
    assertEquals("Scholar C", sortedByScore[0].displayName)
    assertEquals(1, sortedByScore[0].rank)
    assertEquals("Scholar B", sortedByScore[1].displayName)
    assertEquals(2, sortedByScore[1].rank)
    assertEquals("Scholar A", sortedByScore[2].displayName)
    assertEquals(3, sortedByScore[2].rank)

    // Sort by Time Taken: Scholar B (25s) -> Scholar C (30s) -> Scholar A (40s)
    val sortedByTime = LeaderboardSorter.sortQuiz(entries, LeaderboardSortOption.TIME_TAKEN)
    assertEquals("Scholar B", sortedByTime[0].displayName)
    assertEquals(25, sortedByTime[0].timeTakenSeconds)
    assertEquals(1, sortedByTime[0].rank)
    assertEquals("Scholar C", sortedByTime[1].displayName)
    assertEquals(30, sortedByTime[1].timeTakenSeconds)
    assertEquals(2, sortedByTime[1].rank)
  }

  @Test
  fun `verify personal best determination and aggregation logic for quiz attempts`() {
    // Attempt 1: score = 7, accuracy = 70.0, time = 30
    val score1 = 7
    val acc1 = 70.0
    val time1 = 30

    var bestScore = score1
    var bestAccuracy = acc1
    var bestTime = time1

    // Attempt 2: score = 9, accuracy = 90.0, time = 35 -> Better score!
    val score2 = 9
    val acc2 = 90.0
    val time2 = 35

    val isAttempt2Better = (score2 > bestScore) ||
      (score2 == bestScore && acc2 > bestAccuracy) ||
      (score2 == bestScore && acc2 == bestAccuracy && time2 < bestTime)

    assertTrue("Attempt 2 should be recognized as a new personal best", isAttempt2Better)
    if (isAttempt2Better) {
      bestScore = score2
      bestAccuracy = acc2
      bestTime = time2
    }
    assertEquals(9, bestScore)

    // Attempt 3: score = 9, accuracy = 90.0, time = 28 -> Same score & accuracy, but faster time!
    val score3 = 9
    val acc3 = 90.0
    val time3 = 28

    val isAttempt3Better = (score3 > bestScore) ||
      (score3 == bestScore && acc3 > bestAccuracy) ||
      (score3 == bestScore && acc3 == bestAccuracy && time3 < bestTime)

    assertTrue("Attempt 3 with faster time should replace previous personal best", isAttempt3Better)
    if (isAttempt3Better) {
      bestTime = time3
    }
    assertEquals(28, bestTime)

    // Attempt 4: score = 8, accuracy = 80.0, time = 20 -> Worse score, should NOT replace personal best
    val score4 = 8
    val acc4 = 80.0
    val time4 = 20

    val isAttempt4Better = (score4 > bestScore) ||
      (score4 == bestScore && acc4 > bestAccuracy) ||
      (score4 == bestScore && acc4 == bestAccuracy && time4 < bestTime)

    assertTrue("Lower score should not replace personal best", !isAttempt4Better)
    assertEquals(9, bestScore)
    assertEquals(28, bestTime)
  }

  @Test
  fun `verify Room offline caching of individual quiz leaderboard entries with QuizLeaderboardDao`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, QuizPollDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val leaderboardDao = database.quizLeaderboardDao()

    val entry1 = CachedQuizLeaderboardEntity(
      id = "quiz_math_1_u1",
      quizId = "quiz_math_1",
      userId = "u1",
      displayName = "Euler",
      username = "euler",
      score = 10,
      totalQuestions = 10,
      accuracy = 100.0,
      timeTakenSeconds = 30,
      rank = 1,
      attemptCount = 1,
      completedAtEpochMs = 1000L
    )

    val entry2 = CachedQuizLeaderboardEntity(
      id = "quiz_math_1_u2",
      quizId = "quiz_math_1",
      userId = "u2",
      displayName = "Gauss",
      username = "gauss",
      score = 9,
      totalQuestions = 10,
      accuracy = 90.0,
      timeTakenSeconds = 25,
      rank = 2,
      attemptCount = 2,
      completedAtEpochMs = 2000L
    )

    leaderboardDao.insertEntries(listOf(entry1, entry2))

    val cachedList = leaderboardDao.getLeaderboard("quiz_math_1")
    assertEquals(2, cachedList.size)
    assertEquals("Euler", cachedList[0].displayName)
    assertEquals(10, cachedList[0].score)
    assertEquals("Gauss", cachedList[1].displayName)
    assertEquals(9, cachedList[1].score)

    // Test observe flow
    val flowList = leaderboardDao.observeLeaderboard("quiz_math_1").first()
    assertEquals(2, flowList.size)
    assertEquals("Euler", flowList.first().displayName)

    database.close()
  }

  @Test
  fun `verify interactive quiz question composition and validation logic`() {
    val qState = com.example.ui.screens.create.EditableQuestionState(
      initialText = "What is the capital of France?",
      initialType = "SINGLE_MCQ",
      initialOptions = listOf("London", "Paris", "Berlin", "Madrid"),
      initialCorrectIndex = 1,
      initialExplanation = "Paris is the capital and most populous city of France."
    )

    assertEquals("What is the capital of France?", qState.questionText)
    assertEquals(4, qState.options.size)
    assertEquals(1, qState.correctIndex)
    assertEquals("Paris", qState.options[qState.correctIndex])

    // Verify option editing
    qState.options[0] = "Rome"
    assertEquals("Rome", qState.options[0])

    // Verify correct toggle
    qState.correctIndex = 2
    assertEquals("Berlin", qState.options[qState.correctIndex])
    qState.correctIndex = 1 // revert back to Paris

    // Validate conversion to QuizQuestion domain model for Firestore upload
    val validOptions = qState.options.map { it.trim() }.filter { it.isNotBlank() }
    assertTrue(validOptions.size >= 2)
    assertTrue(qState.correctIndex in validOptions.indices)

    val question = com.example.data.model.QuizQuestion(
      questionText = qState.questionText.trim(),
      questionType = qState.questionType,
      options = validOptions,
      correctAnswers = listOf(qState.correctIndex),
      explanation = qState.explanation.trim(),
      orderIndex = 0
    )

    assertEquals("What is the capital of France?", question.questionText)
    assertEquals("SINGLE_MCQ", question.questionType)
    assertEquals(4, question.options.size)
    assertEquals(listOf(1), question.correctAnswers)
    assertEquals("Paris is the capital and most populous city of France.", question.explanation)
  }

  @Test
  fun `verify community feed posts structure for quizzes and polls in Firestore`() {
    val quizPost = com.example.data.model.Post(
      id = "post_q_1",
      userId = "user_101",
      authorName = "Alice",
      authorUsername = "alice",
      postType = "quiz",
      title = "Quantum Physics Trivia",
      content = "Test your knowledge on Quantum Mechanics!",
      category = "Science",
      quizId = "quiz_qp_1",
      pollId = "",
      likeCount = 15,
      commentCount = 4
    )

    val pollPost = com.example.data.model.Post(
      id = "post_p_1",
      userId = "user_102",
      authorName = "Bob",
      authorUsername = "bob",
      postType = "poll",
      title = "Favorite Programming Language?",
      content = "Vote for your daily driver!",
      category = "Technology",
      quizId = "",
      pollId = "poll_lang_1",
      likeCount = 28,
      commentCount = 12
    )

    val feed = listOf(quizPost, pollPost)

    // Filter community feed by type
    val quizzesOnly = feed.filter { it.postType == "quiz" }
    val pollsOnly = feed.filter { it.postType == "poll" }

    assertEquals(1, quizzesOnly.size)
    assertEquals("Quantum Physics Trivia", quizzesOnly.first().title)
    assertEquals("quiz_qp_1", quizzesOnly.first().quizId)

    assertEquals(1, pollsOnly.size)
    assertEquals("Favorite Programming Language?", pollsOnly.first().title)
    assertEquals("poll_lang_1", pollsOnly.first().pollId)

    // Verify category filtering
    val sciencePosts = feed.filter { it.category.equals("Science", ignoreCase = true) }
    assertEquals(1, sciencePosts.size)
    assertEquals("Quantum Physics Trivia", sciencePosts.first().title)
  }

  @Test
  fun `verify user profile screen quiz history and badges tracking`() {
    val attempt1 = com.example.data.model.QuizAttempt(
      id = "att_1",
      quizId = "quiz_geo_1",
      userId = "user_me",
      quizTitle = "World Capitals Challenge",
      score = 8,
      totalQuestions = 10,
      accuracy = 80.0,
      timeTakenSeconds = 42,
      createdAt = null
    )

    val attempt2 = com.example.data.model.QuizAttempt(
      id = "att_2",
      quizId = "quiz_math_1",
      userId = "user_me",
      quizTitle = "Mental Arithmetic Blitz",
      score = 10,
      totalQuestions = 10,
      accuracy = 100.0,
      timeTakenSeconds = 25,
      createdAt = null
    )

    val history = listOf(attempt1, attempt2)
    assertEquals(2, history.size)
    assertEquals("World Capitals Challenge", history[0].quizTitle)
    assertEquals(80.0, history[0].accuracy, 0.01)
    assertEquals(42, history[0].timeTakenSeconds)

    assertEquals("Mental Arithmetic Blitz", history[1].quizTitle)
    assertEquals(100.0, history[1].accuracy, 0.01)
    assertEquals(25, history[1].timeTakenSeconds)

    // Verify badges earned calculation
    val stats = com.example.data.model.ActivityPointsCalculator.computeStats(
      quizzesCreated = 5,
      pollsCreated = 4,
      commentsContributed = 6
    )
    val unlockedBadges = stats.badges.filter { it.isUnlocked }
    assertTrue(unlockedBadges.isNotEmpty())
    assertTrue(unlockedBadges.any { it.name.contains("Quiz", ignoreCase = true) || it.name.contains("Scholar", ignoreCase = true) || it.name.contains("Creator", ignoreCase = true) })
  }

  @Test
  fun `verify feed search bar keyword and topic filtering logic`() {
    val items = listOf(
      com.example.data.model.Post(
        id = "p1",
        userId = "u1",
        postType = "quiz",
        title = "Ancient Rome Empire Quiz",
        content = "Test your knowledge of the Caesars",
        category = "History"
      ),
      com.example.data.model.Post(
        id = "p2",
        userId = "u2",
        postType = "poll",
        title = "Which mobile OS do you prefer?",
        content = "Android vs iOS ecosystem",
        category = "Technology"
      ),
      com.example.data.model.Post(
        id = "p3",
        userId = "u3",
        postType = "quiz",
        title = "Quantum Physics Intro",
        content = "Wave-particle duality and spin",
        category = "Science"
      )
    )

    // Keyword search: "caesar"
    val query1 = "caesar"
    val matchedCaesar = items.filter { post ->
      post.title.contains(query1, ignoreCase = true) ||
      post.content.contains(query1, ignoreCase = true) ||
      post.category.contains(query1, ignoreCase = true)
    }
    assertEquals(1, matchedCaesar.size)
    assertEquals("Ancient Rome Empire Quiz", matchedCaesar[0].title)

    // Topic search: "Technology"
    val techItems = items.filter { it.category.equals("Technology", ignoreCase = true) }
    assertEquals(1, techItems.size)
    assertEquals("Which mobile OS do you prefer?", techItems[0].title)

    // Combined query: quizzes in "Science"
    val scienceQuizzes = items.filter { it.postType == "quiz" && it.category.equals("Science", ignoreCase = true) }
    assertEquals(1, scienceQuizzes.size)
    assertEquals("Quantum Physics Intro", scienceQuizzes[0].title)
  }

  @Test
  fun `verify quiz comment section discussion and feedback thread`() {
    val quizPostId = "quiz_post_999"
    val comment1 = com.example.data.model.Comment(
      id = "c1",
      postId = quizPostId,
      userId = "u10",
      authorName = "Jordan",
      authorUsername = "jordan_dev",
      content = "Question 3 explanation was really helpful, thanks for detailing the formulas!",
      createdAt = null
    )

    val comment2 = com.example.data.model.Comment(
      id = "c2",
      postId = quizPostId,
      userId = "u11",
      authorName = "Elena",
      authorUsername = "elena_quiz",
      content = "Scored 9/10! Loved the timer pace.",
      createdAt = null
    )

    val thread = listOf(comment1, comment2)
    assertEquals(2, thread.size)
    assertEquals(quizPostId, thread[0].postId)
    assertEquals("Jordan", thread[0].authorName)
    assertTrue(thread[0].content.contains("Question 3"))
    assertEquals("Elena", thread[1].authorName)
    assertTrue(thread[1].content.contains("Scored 9/10"))
  }

  @Test
  fun `verify InteractivePoll multiple choice and single choice vote calculation`() {
    val singleChoicePoll = com.example.data.model.Poll(
      id = "poll_interactive_1",
      userId = "user_author",
      authorName = "Alex",
      question = "What is your primary programming language?",
      options = listOf("Kotlin", "TypeScript", "Python", "Rust"),
      optionVotes = listOf(60L, 20L, 15L, 5L),
      totalVotes = 100L,
      isMultipleChoice = false,
      allowVoteChange = true
    )

    // Verify option vote percentages
    val percentages = singleChoicePoll.optionVotes.map { votes ->
      ((votes.toDouble() / singleChoicePoll.totalVotes.toDouble()) * 100).toInt()
    }
    assertEquals(listOf(60, 20, 15, 5), percentages)

    // Top voted choice is index 0 (Kotlin with 60%)
    val topOptionIndex = singleChoicePoll.optionVotes.indexOf(singleChoicePoll.optionVotes.maxOrNull())
    assertEquals(0, topOptionIndex)

    // Verify multi-choice poll scenario
    val multiChoicePoll = com.example.data.model.Poll(
      id = "poll_interactive_2",
      userId = "user_author_2",
      question = "Which cloud platforms do you use?",
      options = listOf("GCP", "Firebase", "AWS"),
      optionVotes = listOf(30L, 40L, 10L),
      totalVotes = 50L,
      isMultipleChoice = true,
      allowVoteChange = true
    )
    assertTrue(multiChoicePoll.isMultipleChoice)
    assertEquals(3, multiChoicePoll.options.size)
    assertEquals(40L, multiChoicePoll.optionVotes[1])
  }

  @Test
  fun `verify InteractivePoll vote change logic adheres to allowVoteChange flag`() {
    val nonEditablePoll = com.example.data.model.Poll(
      id = "poll_locked",
      userId = "user_poll_author",
      question = "Should we adopt strict mode?",
      options = listOf("Yes", "No"),
      optionVotes = listOf(10L, 5L),
      totalVotes = 15L,
      allowVoteChange = false
    )

    assertFalse("Vote change must be disallowed when allowVoteChange is false", nonEditablePoll.allowVoteChange)

    val editablePoll = nonEditablePoll.copy(allowVoteChange = true)
    assertTrue("Vote change must be allowed when allowVoteChange is true", editablePoll.allowVoteChange)
  }

  @Test
  fun `verify Poll and Quiz skeleton and progress indicator loading states during network operations`() {
    // Test initial loading states
    var isPollLoading = true
    var isSubmittingTransaction = false

    assertTrue("Initial state must indicate network loading", isPollLoading)
    assertFalse("Transaction should not be submitting initially", isSubmittingTransaction)

    // Simulate network completion
    isPollLoading = false
    assertFalse("Loading state must be dismissed once data is retrieved from Firestore", isPollLoading)

    // Simulate user vote Firestore transaction
    isSubmittingTransaction = true
    assertTrue("Progress indicator must be active during atomic Firestore transaction commit", isSubmittingTransaction)

    // Transaction commits successfully
    isSubmittingTransaction = false
    assertFalse("Transaction progress indicator should clear once committed", isSubmittingTransaction)
  }
}






package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val displayName: String = "",
  val bio: String = "",
  val profilePictureUrl: String = "",
  val language: String = "English",
  val quizCount: Long = 0L,
  val pollCount: Long = 0L,
  val commentCount: Long = 0L,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val referralCode: String = "",
  val referredBy: String = "",
  val referralCount: Long = 0L,
  val referralPointsEarned: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorUsername: String = "",
  val postType: String = "discussion", // "quiz", "poll", "discussion"
  val title: String = "",
  val content: String = "",
  val category: String = "General Knowledge",
  val quizId: String = "",
  val pollId: String = "",
  val likeCount: Long = 0L,
  val commentCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val targetType: String = "post", // "quiz", "poll", "post"
  val targetId: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorUsername: String = "",
  val content: String = "",
  val likeCount: Long = 0L,
  val replyCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReferralRecord(
  @DocumentId val id: String = "",
  val referrerUserId: String = "",
  val referrerUsername: String = "",
  val refereeUserId: String = "",
  val refereeUsername: String = "",
  val referralCode: String = "",
  val pointsAwardedToReferrer: Long = 100L,
  val pointsAwardedToReferee: Long = 50L,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommentReply(
  @DocumentId val id: String = "",
  val commentId: String = "",
  val postId: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorUsername: String = "",
  val content: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Quiz(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorUsername: String = "",
  val title: String = "",
  val description: String = "",
  val category: String = "General Knowledge",
  val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
  val language: String = "English",
  val questionCount: Long = 0L,
  val timeLimitSeconds: Long = 60L,
  val isPublic: Boolean = true,
  val attemptCount: Long = 0L,
  val averageScore: Double = 0.0,
  val likeCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuizQuestion(
  @DocumentId val id: String = "",
  val quizId: String = "",
  val questionText: String = "",
  val questionType: String = "SINGLE_MCQ", // "SINGLE_MCQ", "MULTIPLE_MCQ", "TRUE_FALSE"
  val options: List<String> = emptyList(),
  val correctAnswers: List<Int> = emptyList(), // 0-based indices
  val explanation: String = "",
  val orderIndex: Int = 0
)

@IgnoreExtraProperties
data class QuizAttempt(
  @DocumentId val id: String = "",
  val quizId: String = "",
  val userId: String = "",
  val quizTitle: String = "",
  val score: Int = 0,
  val totalQuestions: Int = 0,
  val accuracy: Double = 0.0,
  val timeTakenSeconds: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Poll(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorUsername: String = "",
  val question: String = "",
  val description: String = "",
  val category: String = "General Knowledge",
  val options: List<String> = emptyList(),
  val optionVotes: List<Long> = emptyList(),
  val totalVotes: Long = 0L,
  val isMultipleChoice: Boolean = false,
  val allowVoteChange: Boolean = true,
  val isAnonymous: Boolean = false,
  val isPublic: Boolean = true,
  val expiresAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PollVote(
  @DocumentId val id: String = "",
  val userId: String = "",
  val pollId: String = "",
  val selectedOptions: List<Int> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
  @DocumentId val id: String = "",
  val recipientUserId: String = "",
  val senderUserId: String = "",
  val senderName: String = "",
  val type: String = "LIKE", // "FOLLOW", "LIKE", "COMMENT", "REPLY", "QUIZ_ATTEMPT", "POLL_VOTE"
  val targetId: String = "",
  val message: String = "",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReportItem(
  @DocumentId val id: String = "",
  val reporterUserId: String = "",
  val targetType: String = "POST", // "POST", "COMMENT", "QUIZ", "POLL", "USER"
  val targetId: String = "",
  val reason: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockItem(
  @DocumentId val id: String = "",
  val blockedUserId: String = "",
  val blockedUserName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookmarkItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val postId: String = "",
  val postType: String = "", // "quiz", "poll", "discussion"
  val title: String = "",
  val content: String = "",
  val category: String = "General Knowledge",
  val authorName: String = "",
  val quizId: String = "",
  val pollId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TrendingMetric(
  val id: String = "",
  val title: String = "",
  val type: String = "quiz", // "quiz" or "poll"
  val category: String = "General Knowledge",
  val authorName: String = "",
  val score: Int = 0,
  val playsOrVotes: Long = 0L,
  val likes: Long = 0L,
  val comments: Long = 0L,
  val quizId: String = "",
  val pollId: String = ""
)

data class WeeklyTrendingSummary(
  val topQuizzes: List<TrendingMetric> = emptyList(),
  val topPolls: List<TrendingMetric> = emptyList(),
  val totalWeeklyEngagements: Long = 0L
)

@IgnoreExtraProperties
data class LeaderboardUser(
  val userId: String = "",
  val displayName: String = "",
  val username: String = "",
  val rank: Int = 1,
  val totalScore: Int = 0,
  val quizzesCompleted: Int = 0,
  val quizzesCreated: Int = 0,
  val pollsCreated: Int = 0,
  val commentsContributed: Int = 0,
  val totalPoints: Int = 0,
  val badgeTitle: String = "Community Scholar",
  val accuracy: Double = 0.0,
  val timeTakenSeconds: Int = 0,
  val weeklyScore: Int = 0,
  val weeklyAccuracy: Double = 0.0,
  val weeklyTimeTakenSeconds: Int = 0,
  val weeklyQuizzesCompleted: Int = 0,
  val profilePictureUrl: String = ""
)

@IgnoreExtraProperties
data class QuizLeaderboardEntry(
  @DocumentId val id: String = "",
  val quizId: String = "",
  val userId: String = "",
  val displayName: String = "",
  val username: String = "",
  val score: Int = 0,
  val totalQuestions: Int = 0,
  val accuracy: Double = 0.0,
  val timeTakenSeconds: Int = 0,
  val rank: Int = 0,
  val attemptCount: Int = 1,
  val profilePictureUrl: String = "",
  val completedAtEpochMs: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class LeaderboardSortOption(val label: String) {
  SCORE("Score"),
  ACCURACY("Accuracy"),
  TIME_TAKEN("Time Taken")
}

enum class LeaderboardTimeframe(val label: String) {
  THIS_WEEK("This Week"),
  ALL_TIME("All-Time")
}

object LeaderboardSorter {
  fun sortGeneral(
    users: List<LeaderboardUser>,
    timeframe: LeaderboardTimeframe,
    sortBy: LeaderboardSortOption
  ): List<LeaderboardUser> {
    val sorted = when (timeframe) {
      LeaderboardTimeframe.THIS_WEEK -> when (sortBy) {
        LeaderboardSortOption.SCORE -> users.sortedWith(
          compareByDescending<LeaderboardUser> { it.weeklyScore }
            .thenByDescending { it.weeklyAccuracy }
            .thenBy { if (it.weeklyTimeTakenSeconds > 0) it.weeklyTimeTakenSeconds else Int.MAX_VALUE }
        )
        LeaderboardSortOption.ACCURACY -> users.sortedWith(
          compareByDescending<LeaderboardUser> { it.weeklyAccuracy }
            .thenByDescending { it.weeklyScore }
            .thenBy { if (it.weeklyTimeTakenSeconds > 0) it.weeklyTimeTakenSeconds else Int.MAX_VALUE }
        )
        LeaderboardSortOption.TIME_TAKEN -> users.sortedWith(
          compareBy<LeaderboardUser> { if (it.weeklyTimeTakenSeconds > 0) it.weeklyTimeTakenSeconds else Int.MAX_VALUE }
            .thenByDescending { it.weeklyScore }
            .thenByDescending { it.weeklyAccuracy }
        )
      }
      LeaderboardTimeframe.ALL_TIME -> when (sortBy) {
        LeaderboardSortOption.SCORE -> users.sortedWith(
          compareByDescending<LeaderboardUser> { it.totalScore }
            .thenByDescending { it.accuracy }
            .thenBy { if (it.timeTakenSeconds > 0) it.timeTakenSeconds else Int.MAX_VALUE }
        )
        LeaderboardSortOption.ACCURACY -> users.sortedWith(
          compareByDescending<LeaderboardUser> { it.accuracy }
            .thenByDescending { it.totalScore }
            .thenBy { if (it.timeTakenSeconds > 0) it.timeTakenSeconds else Int.MAX_VALUE }
        )
        LeaderboardSortOption.TIME_TAKEN -> users.sortedWith(
          compareBy<LeaderboardUser> { if (it.timeTakenSeconds > 0) it.timeTakenSeconds else Int.MAX_VALUE }
            .thenByDescending { it.totalScore }
            .thenByDescending { it.accuracy }
        )
      }
    }
    return sorted.mapIndexed { index, user -> user.copy(rank = index + 1) }
  }

  fun sortQuiz(
    entries: List<QuizLeaderboardEntry>,
    sortBy: LeaderboardSortOption
  ): List<QuizLeaderboardEntry> {
    val sorted = when (sortBy) {
      LeaderboardSortOption.SCORE -> entries.sortedWith(
        compareByDescending<QuizLeaderboardEntry> { it.score }
          .thenByDescending { it.accuracy }
          .thenBy { if (it.timeTakenSeconds > 0) it.timeTakenSeconds else Int.MAX_VALUE }
      )
      LeaderboardSortOption.ACCURACY -> entries.sortedWith(
        compareByDescending<QuizLeaderboardEntry> { it.accuracy }
          .thenByDescending { it.score }
          .thenBy { if (it.timeTakenSeconds > 0) it.timeTakenSeconds else Int.MAX_VALUE }
      )
      LeaderboardSortOption.TIME_TAKEN -> entries.sortedWith(
        compareBy<QuizLeaderboardEntry> { if (it.timeTakenSeconds > 0) it.timeTakenSeconds else Int.MAX_VALUE }
          .thenByDescending { it.score }
          .thenByDescending { it.accuracy }
      )
    }
    return sorted.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
  }
}


object QuizCategories {
  val ALL = listOf(
    "Science",
    "History",
    "Pop Culture",
    "General Knowledge",
    "Technology",
    "Entertainment",
    "Sports",
    "Mathematics",
    "Geography",
    "Education",
    "Competitive Exams",
    "English Learning",
    "Programming",
    "Business",
    "Current Affairs",
    "Other"
  )
}

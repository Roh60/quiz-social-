package com.example.data.remote

import com.example.data.model.LeaderboardSortOption
import com.example.data.model.LeaderboardTimeframe
import com.example.data.model.LeaderboardUser
import com.example.data.model.Poll
import com.example.data.model.PollVote
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizLeaderboardEntry
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining full CRUD (Create, Read, Update, Delete) and real-time operations
 * for interactive quizzes and polls in Google Cloud Firestore.
 */
interface FirestoreDataRepository {

  // ==================== QUIZ CRUD ====================

  /**
   * CREATE: Adds a new quiz to Firestore under /quizzes/{quizId}, creates questions subcollection,
   * publishes a companion feed post, and increments user's quizCount.
   */
  suspend fun createQuiz(
    title: String,
    description: String,
    category: String,
    difficulty: String,
    language: String = "English",
    timeLimitSeconds: Long = 60,
    isPublic: Boolean = true,
    questions: List<QuizQuestion>
  ): String

  /**
   * READ: Single document fetch for a quiz.
   */
  suspend fun getQuiz(quizId: String): Quiz?

  /**
   * READ: Real-time Flow of quiz metadata with offline-first Room cache support.
   */
  fun observeQuiz(quizId: String): Flow<Quiz?>

  /**
   * READ: Real-time Flow of quizzes created by a specific user.
   */
  fun observeUserQuizzes(userId: String): Flow<List<Quiz>>

  /**
   * READ: Fetches questions subcollection for a given quiz.
   */
  suspend fun getQuizQuestions(quizId: String): List<QuizQuestion>

  /**
   * UPDATE: Updates existing quiz metadata, questions subcollection, and companion feed post.
   */
  suspend fun updateQuiz(
    quizId: String,
    title: String? = null,
    description: String? = null,
    category: String? = null,
    difficulty: String? = null,
    language: String? = null,
    timeLimitSeconds: Long? = null,
    isPublic: Boolean? = null,
    questions: List<QuizQuestion>? = null
  )

  /**
   * DELETE: Deletes quiz from /quizzes/{quizId}, questions subcollection, companion post,
   * decrements user's quizCount, and evicts from local Room database.
   */
  suspend fun deleteQuiz(quizId: String)

  /**
   * EXECUTE & ATTEMPT: Records a completed quiz attempt with Firestore atomic transactions
   * aggregating competition leaderboards and personal best records.
   */
  suspend fun submitQuizAttempt(
    quizId: String,
    quizTitle: String,
    score: Int,
    totalQuestions: Int,
    timeTakenSeconds: Int,
    userAnswers: Map<Int, List<Int>>,
    submissionId: String
  ): QuizAttempt

  /**
   * READ: Real-time leaderboard for an individual quiz.
   */
  fun observeQuizLeaderboard(
    quizId: String,
    sortBy: LeaderboardSortOption = LeaderboardSortOption.SCORE
  ): Flow<List<QuizLeaderboardEntry>>

  /**
   * READ: General competition leaderboards (Weekly and All-Time).
   */
  fun observeCompetitionLeaderboard(
    timeframe: LeaderboardTimeframe = LeaderboardTimeframe.THIS_WEEK,
    sortBy: LeaderboardSortOption = LeaderboardSortOption.SCORE,
    limit: Int = 20
  ): Flow<List<LeaderboardUser>>


  // ==================== POLL CRUD ====================

  /**
   * CREATE: Adds a new poll to Firestore under /polls/{pollId}, publishes companion feed post,
   * and increments user's pollCount.
   */
  suspend fun createPoll(
    question: String,
    description: String = "",
    category: String = "General",
    options: List<String>,
    isMultipleChoice: Boolean = false,
    allowVoteChange: Boolean = true,
    isAnonymous: Boolean = false,
    isPublic: Boolean = true
  ): String

  /**
   * READ: Single document fetch for a poll.
   */
  suspend fun getPoll(pollId: String): Poll?

  /**
   * READ: Real-time Flow of poll with real-time voting percentages and offline-first Room cache support.
   */
  fun observePoll(pollId: String): Flow<Poll?>

  /**
   * READ: Real-time Flow of polls created by a specific user.
   */
  fun observeUserPolls(userId: String): Flow<List<Poll>>

  /**
   * READ: Real-time Flow of user's active vote on a poll.
   */
  fun observeUserVote(pollId: String): Flow<PollVote?>

  /**
   * READ: Real-time Flow of polls that user has completed / voted on.
   */
  fun observeUserVotedPolls(userId: String): Flow<List<Poll>>

  /**
   * UPDATE: Updates existing poll metadata (question, description, category, settings) and companion feed post.
   */
  suspend fun updatePoll(
    pollId: String,
    question: String? = null,
    description: String? = null,
    category: String? = null,
    allowVoteChange: Boolean? = null,
    isPublic: Boolean? = null
  )

  /**
   * DELETE: Deletes poll from /polls/{pollId}, companion post, decrements user's pollCount,
   * and evicts from local Room database.
   */
  suspend fun deletePoll(pollId: String)

  /**
   * VOTE: Submits or changes a vote in a poll using atomic FieldValue increments.
   */
  suspend fun submitPollVote(pollId: String, selectedOptionIndices: List<Int>)
}

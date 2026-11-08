package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BookmarkItem
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizLeaderboardEntry
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.Date

// =========================================================================
// 1. ROOM ENTITIES (Social Feed, Quiz History, Poll Results, Drafts, Bookmarks)
// =========================================================================

/**
 * Cached social feed post entity (Quizzes, Polls, Community Discussions).
 */
@Entity(tableName = "cached_posts")
data class CachedPostEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val authorName: String,
  val authorUsername: String,
  val postType: String,
  val title: String,
  val content: String,
  val category: String,
  val quizId: String,
  val pollId: String,
  val likeCount: Long,
  val commentCount: Long,
  val createdAtEpochMs: Long
)

/**
 * Cached community quiz entity.
 */
@Entity(tableName = "cached_quizzes")
data class CachedQuizEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val authorName: String,
  val authorUsername: String = "",
  val title: String,
  val description: String,
  val category: String,
  val difficulty: String,
  val questionCount: Long,
  val timeLimitSeconds: Long,
  val attemptCount: Long,
  val likeCount: Long = 0L,
  val createdAtEpochMs: Long
)

/**
 * Cached community poll entity with full options, votes, and real-time results breakdown.
 */
@Entity(tableName = "cached_polls")
data class CachedPollEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val authorName: String,
  val authorUsername: String = "",
  val question: String,
  val description: String,
  val category: String,
  val optionsJson: String,      // JSON string array of option labels
  val optionVotesJson: String,  // JSON number array of vote counts
  val totalVotes: Long,
  val isMultipleChoice: Boolean,
  val allowVoteChange: Boolean,
  val isAnonymous: Boolean = false,
  val userVotedIndicesCsv: String = "", // Comma-separated indices of votes cast by user
  val createdAtEpochMs: Long
)

/**
 * Cached user quiz attempt history entity for offline review and performance tracking.
 */
@Entity(tableName = "cached_quiz_history")
data class CachedQuizAttemptEntity(
  @PrimaryKey val id: String,
  val quizId: String,
  val userId: String,
  val quizTitle: String,
  val score: Int,
  val totalQuestions: Int,
  val accuracy: Double,
  val timeTakenSeconds: Int,
  val completedAtEpochMs: Long
)

/**
 * Cached personal bookmarks.
 */
@Entity(tableName = "cached_bookmarks")
data class CachedBookmarkEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val postId: String,
  val postType: String,
  val title: String,
  val category: String,
  val quizId: String = "",
  val authorName: String = "",
  val content: String = "",
  val savedAtEpochMs: Long
)

/**
 * Offline quiz creation draft.
 */
@Entity(tableName = "quiz_drafts")
data class QuizDraftEntity(
  @PrimaryKey(autoGenerate = true) val draftId: Long = 0,
  val title: String,
  val description: String,
  val category: String,
  val difficulty: String,
  val timeLimitSeconds: Long,
  val questionsJson: String,
  val savedAtEpochMs: Long
)

/**
 * Offline poll creation draft.
 */
@Entity(tableName = "poll_drafts")
data class PollDraftEntity(
  @PrimaryKey(autoGenerate = true) val draftId: Long = 0,
  val question: String,
  val description: String,
  val category: String,
  val optionsCsv: String,
  val isMultipleChoice: Boolean,
  val allowVoteChange: Boolean,
  val isAnonymous: Boolean,
  val savedAtEpochMs: Long
)

/**
 * Cached individual quiz leaderboard rankings.
 */
@Entity(tableName = "cached_quiz_leaderboards")
data class CachedQuizLeaderboardEntity(
  @PrimaryKey val id: String, // "$quizId_$userId"
  val quizId: String,
  val userId: String,
  val displayName: String,
  val username: String,
  val score: Int,
  val totalQuestions: Int,
  val accuracy: Double,
  val timeTakenSeconds: Int,
  val rank: Int,
  val attemptCount: Int,
  val profilePictureUrl: String = "",
  val completedAtEpochMs: Long
)

// =========================================================================
// 2. DATA ACCESS OBJECTS (DAOs)
// =========================================================================

/**
 * DAO for caching and querying the social feed (with category filtering and pagination support).
 */
@Dao
interface PostDao {
  @Query("SELECT * FROM cached_posts ORDER BY createdAtEpochMs DESC")
  fun observeCachedPosts(): Flow<List<CachedPostEntity>>

  @Query("SELECT * FROM cached_posts WHERE category = :category ORDER BY createdAtEpochMs DESC")
  fun observeCachedPostsByCategory(category: String): Flow<List<CachedPostEntity>>

  @Query("SELECT * FROM cached_posts WHERE postType = :postType ORDER BY createdAtEpochMs DESC")
  fun observeCachedPostsByType(postType: String): Flow<List<CachedPostEntity>>

  @Query("SELECT * FROM cached_posts ORDER BY likeCount DESC, createdAtEpochMs DESC")
  fun observeCachedPopularPosts(): Flow<List<CachedPostEntity>>

  @Query("SELECT * FROM cached_posts ORDER BY commentCount DESC, likeCount DESC, createdAtEpochMs DESC")
  fun observeCachedTrendingPosts(): Flow<List<CachedPostEntity>>

  @Query("""
    SELECT * FROM cached_posts 
    WHERE (
      title LIKE '%' || :query || '%' 
      OR content LIKE '%' || :query || '%' 
      OR category LIKE '%' || :query || '%' 
      OR authorName LIKE '%' || :query || '%'
      OR authorUsername LIKE '%' || :query || '%'
    )
    ORDER BY createdAtEpochMs DESC
  """)
  fun searchCachedPosts(query: String): Flow<List<CachedPostEntity>>

  @Query("""
    SELECT * FROM cached_posts 
    WHERE (
      :category IS NULL 
      OR :category = 'All' 
      OR category LIKE '%' || :category || '%'
    ) AND (
      :query IS NULL 
      OR :query = '' 
      OR title LIKE '%' || :query || '%' 
      OR content LIKE '%' || :query || '%' 
      OR category LIKE '%' || :query || '%' 
      OR authorName LIKE '%' || :query || '%'
    )
    ORDER BY createdAtEpochMs DESC
  """)
  fun searchCachedPostsWithCategory(query: String?, category: String?): Flow<List<CachedPostEntity>>

  @Query("""
    SELECT * FROM cached_posts 
    WHERE (
      :postType IS NULL 
      OR :postType = 'All' 
      OR postType = :postType
    ) AND (
      :category IS NULL 
      OR :category = 'All' 
      OR category LIKE '%' || :category || '%'
    ) AND (
      :query IS NULL 
      OR :query = '' 
      OR title LIKE '%' || :query || '%' 
      OR content LIKE '%' || :query || '%' 
      OR category LIKE '%' || :query || '%' 
      OR authorName LIKE '%' || :query || '%'
    )
    ORDER BY createdAtEpochMs DESC
  """)
  fun searchCachedPostsWithFilters(query: String?, category: String?, postType: String?): Flow<List<CachedPostEntity>>

  @Query("SELECT * FROM cached_posts WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
  fun observeCachedUserPosts(userId: String): Flow<List<CachedPostEntity>>

  @Query("SELECT * FROM cached_posts WHERE id = :id LIMIT 1")
  suspend fun getPostById(id: String): CachedPostEntity?

  @Query("SELECT * FROM cached_posts ORDER BY createdAtEpochMs DESC")
  suspend fun getCachedPosts(): List<CachedPostEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPosts(posts: List<CachedPostEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPost(post: CachedPostEntity)

  @Query("UPDATE cached_posts SET likeCount = :newCount WHERE id = :postId")
  suspend fun updateLikeCount(postId: String, newCount: Long)

  @Query("DELETE FROM cached_posts WHERE id = :id")
  suspend fun deletePost(id: String)

  @Query("DELETE FROM cached_posts")
  suspend fun clearPosts()
}

/**
 * DAO for caching quizzes.
 */
@Dao
interface QuizDao {
  @Query("SELECT * FROM cached_quizzes ORDER BY createdAtEpochMs DESC")
  fun observeCachedQuizzes(): Flow<List<CachedQuizEntity>>

  @Query("SELECT * FROM cached_quizzes WHERE category = :category ORDER BY createdAtEpochMs DESC")
  fun observeCachedQuizzesByCategory(category: String): Flow<List<CachedQuizEntity>>

  @Query("SELECT * FROM cached_quizzes WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
  fun observeCachedUserQuizzes(userId: String): Flow<List<CachedQuizEntity>>

  @Query("""
    SELECT * FROM cached_quizzes 
    WHERE title LIKE '%' || :query || '%' 
       OR description LIKE '%' || :query || '%' 
       OR category LIKE '%' || :query || '%' 
       OR authorName LIKE '%' || :query || '%'
    ORDER BY createdAtEpochMs DESC
  """)
  fun searchCachedQuizzes(query: String): Flow<List<CachedQuizEntity>>

  @Query("SELECT * FROM cached_quizzes WHERE id = :id LIMIT 1")
  suspend fun getQuizById(id: String): CachedQuizEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuizzes(quizzes: List<CachedQuizEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuiz(quiz: CachedQuizEntity)

  @Query("DELETE FROM cached_quizzes WHERE id = :id")
  suspend fun deleteQuiz(id: String)

  @Query("DELETE FROM cached_quizzes")
  suspend fun clearQuizzes()
}

/**
 * DAO for caching polls and live poll voting results.
 */
@Dao
interface PollDao {
  @Query("SELECT * FROM cached_polls ORDER BY createdAtEpochMs DESC")
  fun observeCachedPolls(): Flow<List<CachedPollEntity>>

  @Query("SELECT * FROM cached_polls WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
  fun observeCachedUserPolls(userId: String): Flow<List<CachedPollEntity>>

  @Query("SELECT * FROM cached_polls WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
  fun observeUserPolls(userId: String): Flow<List<CachedPollEntity>>

  @Query("""
    SELECT * FROM cached_polls 
    WHERE question LIKE '%' || :query || '%' 
       OR description LIKE '%' || :query || '%' 
       OR category LIKE '%' || :query || '%' 
       OR authorName LIKE '%' || :query || '%'
    ORDER BY createdAtEpochMs DESC
  """)
  fun searchCachedPolls(query: String): Flow<List<CachedPollEntity>>

  @Query("SELECT * FROM cached_polls WHERE id = :id LIMIT 1")
  fun observePollById(id: String): Flow<CachedPollEntity?>

  @Query("SELECT * FROM cached_polls WHERE id = :id LIMIT 1")
  suspend fun getPollById(id: String): CachedPollEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPolls(polls: List<CachedPollEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPoll(poll: CachedPollEntity)

  @Query("UPDATE cached_polls SET optionVotesJson = :votesJson, totalVotes = :totalVotes, userVotedIndicesCsv = :votedCsv WHERE id = :id")
  suspend fun updatePollResults(id: String, votesJson: String, totalVotes: Long, votedCsv: String)

  @Query("DELETE FROM cached_polls WHERE id = :id")
  suspend fun deletePoll(id: String)

  @Query("DELETE FROM cached_polls")
  suspend fun clearPolls()
}

/**
 * DAO for caching user quiz attempt history.
 */
@Dao
interface QuizHistoryDao {
  @Query("SELECT * FROM cached_quiz_history WHERE userId = :userId ORDER BY completedAtEpochMs DESC")
  fun observeQuizHistory(userId: String): Flow<List<CachedQuizAttemptEntity>>

  @Query("SELECT * FROM cached_quiz_history WHERE userId = :userId ORDER BY completedAtEpochMs DESC")
  suspend fun getQuizHistory(userId: String): List<CachedQuizAttemptEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttempt(attempt: CachedQuizAttemptEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttempts(attempts: List<CachedQuizAttemptEntity>)

  @Query("DELETE FROM cached_quiz_history WHERE userId = :userId")
  suspend fun clearHistoryForUser(userId: String)
}

/**
 * DAO for offline bookmarks.
 */
@Dao
interface BookmarkDao {
  @Query("SELECT * FROM cached_bookmarks WHERE userId = :userId ORDER BY savedAtEpochMs DESC")
  fun observeBookmarks(userId: String): Flow<List<CachedBookmarkEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(bookmark: CachedBookmarkEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmarks(bookmarks: List<CachedBookmarkEntity>)

  @Query("DELETE FROM cached_bookmarks WHERE postId = :postId AND userId = :userId")
  suspend fun removeBookmark(postId: String, userId: String)

  @Query("DELETE FROM cached_bookmarks WHERE userId = :userId")
  suspend fun clearBookmarks(userId: String)
}

/**
 * DAO for creator drafts.
 */
@Dao
interface DraftDao {
  @Query("SELECT * FROM quiz_drafts ORDER BY savedAtEpochMs DESC")
  fun observeQuizDrafts(): Flow<List<QuizDraftEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveQuizDraft(draft: QuizDraftEntity): Long

  @Query("DELETE FROM quiz_drafts WHERE draftId = :id")
  suspend fun deleteQuizDraft(id: Long)

  @Query("SELECT * FROM poll_drafts ORDER BY savedAtEpochMs DESC")
  fun observePollDrafts(): Flow<List<PollDraftEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun savePollDraft(draft: PollDraftEntity): Long

  @Query("DELETE FROM poll_drafts WHERE draftId = :id")
  suspend fun deletePollDraft(id: Long)
}

/**
 * DAO for caching and querying individual quiz leaderboards.
 */
@Dao
interface QuizLeaderboardDao {
  @Query("SELECT * FROM cached_quiz_leaderboards WHERE quizId = :quizId ORDER BY score DESC, accuracy DESC, timeTakenSeconds ASC")
  fun observeLeaderboard(quizId: String): Flow<List<CachedQuizLeaderboardEntity>>

  @Query("SELECT * FROM cached_quiz_leaderboards WHERE quizId = :quizId ORDER BY score DESC, accuracy DESC, timeTakenSeconds ASC")
  suspend fun getLeaderboard(quizId: String): List<CachedQuizLeaderboardEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntries(entries: List<CachedQuizLeaderboardEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntry(entry: CachedQuizLeaderboardEntity)

  @Query("DELETE FROM cached_quiz_leaderboards WHERE quizId = :quizId")
  suspend fun clearLeaderboard(quizId: String)
}

// =========================================================================
// 3. DATABASE CONTAINER
// =========================================================================

@Database(
  entities = [
    CachedPostEntity::class,
    CachedQuizEntity::class,
    CachedPollEntity::class,
    CachedQuizAttemptEntity::class,
    CachedBookmarkEntity::class,
    QuizDraftEntity::class,
    PollDraftEntity::class,
    CachedQuizLeaderboardEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class QuizPollDatabase : RoomDatabase() {
  abstract fun postDao(): PostDao
  abstract fun quizDao(): QuizDao
  abstract fun pollDao(): PollDao
  abstract fun quizHistoryDao(): QuizHistoryDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun draftDao(): DraftDao
  abstract fun quizLeaderboardDao(): QuizLeaderboardDao

  companion object {
    @Volatile
    private var INSTANCE: QuizPollDatabase? = null

    fun getDatabase(context: Context): QuizPollDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          QuizPollDatabase::class.java,
          "quizpoll_community.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

// =========================================================================
// 4. MODEL <-> ENTITY CONVERSION EXTENSIONS
// =========================================================================

fun Post.toCachedEntity(): CachedPostEntity {
  return CachedPostEntity(
    id = this.id,
    userId = this.userId,
    authorName = this.authorName,
    authorUsername = this.authorUsername,
    postType = this.postType,
    title = this.title,
    content = this.content,
    category = this.category,
    quizId = this.quizId,
    pollId = this.pollId,
    likeCount = this.likeCount,
    commentCount = this.commentCount,
    createdAtEpochMs = this.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

fun CachedPostEntity.toModel(): Post {
  return Post(
    id = this.id,
    userId = this.userId,
    authorName = this.authorName,
    authorUsername = this.authorUsername,
    postType = this.postType,
    title = this.title,
    content = this.content,
    category = this.category,
    quizId = this.quizId,
    pollId = this.pollId,
    likeCount = this.likeCount,
    commentCount = this.commentCount,
    createdAt = Timestamp(Date(this.createdAtEpochMs))
  )
}

fun Quiz.toCachedEntity(): CachedQuizEntity {
  return CachedQuizEntity(
    id = this.id,
    userId = this.userId,
    authorName = this.authorName,
    authorUsername = this.authorUsername,
    title = this.title,
    description = this.description,
    category = this.category,
    difficulty = this.difficulty,
    questionCount = this.questionCount,
    timeLimitSeconds = this.timeLimitSeconds,
    attemptCount = this.attemptCount,
    likeCount = this.likeCount,
    createdAtEpochMs = this.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

fun CachedQuizEntity.toModel(): Quiz {
  return Quiz(
    id = this.id,
    userId = this.userId,
    authorName = this.authorName,
    authorUsername = this.authorUsername,
    title = this.title,
    description = this.description,
    category = this.category,
    difficulty = this.difficulty,
    questionCount = this.questionCount,
    timeLimitSeconds = this.timeLimitSeconds,
    attemptCount = this.attemptCount,
    likeCount = this.likeCount,
    createdAt = Timestamp(Date(this.createdAtEpochMs))
  )
}

fun Poll.toCachedEntity(userVotedIndices: List<Int> = emptyList()): CachedPollEntity {
  val optArray = JSONArray()
  this.options.forEach { optArray.put(it) }

  val votesArray = JSONArray()
  this.optionVotes.forEach { votesArray.put(it) }

  val votedCsv = userVotedIndices.joinToString(",")

  return CachedPollEntity(
    id = this.id,
    userId = this.userId,
    authorName = this.authorName,
    authorUsername = this.authorUsername,
    question = this.question,
    description = this.description,
    category = this.category,
    optionsJson = optArray.toString(),
    optionVotesJson = votesArray.toString(),
    totalVotes = this.totalVotes,
    isMultipleChoice = this.isMultipleChoice,
    allowVoteChange = this.allowVoteChange,
    isAnonymous = this.isAnonymous,
    userVotedIndicesCsv = votedCsv,
    createdAtEpochMs = this.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

fun CachedPollEntity.toModel(): Poll {
  val optionsList = mutableListOf<String>()
  try {
    val arr = JSONArray(this.optionsJson)
    for (i in 0 until arr.length()) {
      optionsList.add(arr.getString(i))
    }
  } catch (_: Exception) {}

  val votesList = mutableListOf<Long>()
  try {
    val arr = JSONArray(this.optionVotesJson)
    for (i in 0 until arr.length()) {
      votesList.add(arr.getLong(i))
    }
  } catch (_: Exception) {}

  return Poll(
    id = this.id,
    userId = this.userId,
    authorName = this.authorName,
    authorUsername = this.authorUsername,
    question = this.question,
    description = this.description,
    category = this.category,
    options = optionsList,
    optionVotes = votesList,
    totalVotes = this.totalVotes,
    isMultipleChoice = this.isMultipleChoice,
    allowVoteChange = this.allowVoteChange,
    isAnonymous = this.isAnonymous,
    createdAt = Timestamp(Date(this.createdAtEpochMs))
  )
}

fun QuizAttempt.toCachedEntity(): CachedQuizAttemptEntity {
  return CachedQuizAttemptEntity(
    id = this.id,
    quizId = this.quizId,
    userId = this.userId,
    quizTitle = this.quizTitle,
    score = this.score,
    totalQuestions = this.totalQuestions,
    accuracy = this.accuracy,
    timeTakenSeconds = this.timeTakenSeconds,
    completedAtEpochMs = this.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

fun CachedQuizAttemptEntity.toModel(): QuizAttempt {
  return QuizAttempt(
    id = this.id,
    quizId = this.quizId,
    userId = this.userId,
    quizTitle = this.quizTitle,
    score = this.score,
    totalQuestions = this.totalQuestions,
    accuracy = this.accuracy,
    timeTakenSeconds = this.timeTakenSeconds,
    createdAt = Timestamp(Date(this.completedAtEpochMs))
  )
}

fun BookmarkItem.toCachedEntity(): CachedBookmarkEntity {
  return CachedBookmarkEntity(
    id = this.id,
    userId = this.userId,
    postId = this.postId,
    postType = this.postType,
    title = this.title,
    category = this.category,
    quizId = this.quizId,
    authorName = this.authorName,
    content = this.content,
    savedAtEpochMs = this.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

fun CachedBookmarkEntity.toModel(): BookmarkItem {
  return BookmarkItem(
    id = this.id,
    userId = this.userId,
    postId = this.postId,
    postType = this.postType,
    title = this.title,
    category = this.category,
    quizId = this.quizId,
    authorName = this.authorName,
    content = this.content,
    createdAt = Timestamp(Date(this.savedAtEpochMs))
  )
}

fun QuizLeaderboardEntry.toCachedEntity(): CachedQuizLeaderboardEntity {
  return CachedQuizLeaderboardEntity(
    id = if (this.id.isNotBlank()) this.id else "${this.quizId}_${this.userId}",
    quizId = this.quizId,
    userId = this.userId,
    displayName = this.displayName,
    username = this.username,
    score = this.score,
    totalQuestions = this.totalQuestions,
    accuracy = this.accuracy,
    timeTakenSeconds = this.timeTakenSeconds,
    rank = this.rank,
    attemptCount = this.attemptCount,
    profilePictureUrl = this.profilePictureUrl,
    completedAtEpochMs = this.completedAtEpochMs.takeIf { it > 0 } ?: (this.createdAt?.toDate()?.time ?: System.currentTimeMillis())
  )
}

fun CachedQuizLeaderboardEntity.toModel(): QuizLeaderboardEntry {
  return QuizLeaderboardEntry(
    id = this.id,
    quizId = this.quizId,
    userId = this.userId,
    displayName = this.displayName,
    username = this.username,
    score = this.score,
    totalQuestions = this.totalQuestions,
    accuracy = this.accuracy,
    timeTakenSeconds = this.timeTakenSeconds,
    rank = this.rank,
    attemptCount = this.attemptCount,
    profilePictureUrl = this.profilePictureUrl,
    completedAtEpochMs = this.completedAtEpochMs,
    createdAt = Timestamp(Date(this.completedAtEpochMs))
  )
}

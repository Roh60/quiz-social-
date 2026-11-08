package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.CachedPostEntity
import com.example.data.local.PostDao
import com.example.data.model.ActivityPointsCalculator
import com.example.data.model.BlockItem
import com.example.data.model.BookmarkItem
import com.example.data.model.Comment
import com.example.data.model.CommentReply
import com.example.data.model.LeaderboardUser
import com.example.data.model.NotificationItem
import com.example.data.model.Poll
import com.example.data.model.PollVote
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.ReferralRecord
import com.example.data.model.ReportItem
import com.example.data.model.TrendingMetric
import com.example.data.model.UserProfile
import com.example.data.model.WeeklyTrendingSummary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import com.example.data.local.BookmarkDao
import com.example.data.local.CachedBookmarkEntity
import com.example.data.local.CachedPollEntity
import com.example.data.local.CachedQuizAttemptEntity
import com.example.data.local.CachedQuizEntity
import com.example.data.local.CachedQuizLeaderboardEntity
import com.example.data.local.PollDao
import com.example.data.local.QuizDao
import com.example.data.local.QuizHistoryDao
import com.example.data.local.QuizLeaderboardDao
import com.example.data.local.toCachedEntity
import com.example.data.local.toModel
import com.example.data.model.LeaderboardSortOption
import com.example.data.model.LeaderboardSorter
import com.example.data.model.LeaderboardTimeframe
import com.example.data.model.QuizLeaderboardEntry
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class QuizPollRepository(
  private val db: FirebaseFirestore,
  private val postDao: PostDao? = null,
  private val pollDao: PollDao? = null,
  private val quizDao: QuizDao? = null,
  private val quizHistoryDao: QuizHistoryDao? = null,
  private val bookmarkDao: BookmarkDao? = null,
  private val quizLeaderboardDao: QuizLeaderboardDao? = null,
  private val context: Context? = null
) : FirestoreDataRepository {
  constructor(
    context: Context,
    postDao: PostDao? = null,
    pollDao: PollDao? = null,
    quizDao: QuizDao? = null,
    quizHistoryDao: QuizHistoryDao? = null,
    bookmarkDao: BookmarkDao? = null,
    quizLeaderboardDao: QuizLeaderboardDao? = null
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    postDao,
    pollDao,
    quizDao,
    quizHistoryDao,
    bookmarkDao,
    quizLeaderboardDao,
    context.applicationContext
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before performing this action.")
  }

  fun getCurrentUserId(): String? = auth.currentUser?.uid

  // ---------------- USER & USERNAME ----------------

  suspend fun getOrCreateUserProfile(
    initialUsername: String? = null,
    initialDisplayName: String? = null
  ): UserProfile {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val snapshot = docRef.get().await()

    if (snapshot.exists()) {
      val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
      if (existing.referralCode.isBlank()) {
        val code = generateReferralCode(existing.username, uid)
        try {
          docRef.update("referralCode", code)
        } catch (_: Exception) {}
        return existing.copy(referralCode = code)
      }
      return existing
    }

    val userEmail = auth.currentUser?.email ?: "user"
    val defaultUsername = initialUsername?.takeIf { it.isNotBlank() }
      ?: userEmail.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "_")
    val defaultName = initialDisplayName?.takeIf { it.isNotBlank() }
      ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
      ?: "Quiz Enthusiast"

    val refCode = generateReferralCode(defaultUsername, uid)
    val newProfile = UserProfile(
      userId = uid,
      username = defaultUsername,
      displayName = defaultName,
      bio = "Exploring knowledge & sharing quizzes with the community!",
      language = "English",
      quizCount = 0L,
      pollCount = 0L,
      followersCount = 0L,
      followingCount = 0L,
      referralCode = refCode
    )

    docRef.set(newProfile).await()
    // Also reserve username
    try {
      db.collection("usernames").document(defaultUsername).set(
        mapOf("username" to defaultUsername, "userId" to uid, "createdAt" to FieldValue.serverTimestamp())
      ).await()
    } catch (_: Exception) {}

    return newProfile
  }

  fun generateReferralCode(username: String, uid: String): String {
    val clean = username.filter { it.isLetterOrDigit() }.take(4).uppercase().ifBlank { "QP" }
    val suffix = uid.takeLast(4).uppercase().ifBlank { "7K4X" }
    return "QP-$clean$suffix"
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { it.toObject(UserProfile::class.java) }
        .catch { e ->
          if (e is Exception) handleFirestoreError(e, OperationType.READ, "users/$userId")
          emit(null)
        }
    )
  }

  suspend fun updateUserProfile(
    displayName: String,
    bio: String,
    language: String,
    profilePictureUrl: String = ""
  ) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val updates = mutableMapOf<String, Any>(
      "displayName" to displayName,
      "bio" to bio,
      "language" to language,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (profilePictureUrl.isNotBlank()) {
      updates["profilePictureUrl"] = profilePictureUrl
    }
    docRef.update(updates).await()
  }

  // ---------------- POSTS & FEED ----------------

  suspend fun createDiscussionPost(title: String, content: String, category: String): String {
    val uid = requireUserId()
    val profile = getOrCreateUserProfile()
    val postRef = db.collection("posts").document()
    val post = Post(
      userId = uid,
      authorName = profile.displayName,
      authorUsername = profile.username,
      postType = "discussion",
      title = title,
      content = content,
      category = category,
      likeCount = 0L,
      commentCount = 0L
    )
    postRef.set(post).await()
    return postRef.id
  }

  override suspend fun createQuiz(
    title: String,
    description: String,
    category: String,
    difficulty: String,
    language: String,
    timeLimitSeconds: Long,
    isPublic: Boolean,
    questions: List<QuizQuestion>
  ): String {
    val uid = requireUserId()
    val profile = getOrCreateUserProfile()
    val quizRef = db.collection("quizzes").document()
    val quiz = Quiz(
      userId = uid,
      authorName = profile.displayName,
      authorUsername = profile.username,
      title = title,
      description = description,
      category = category,
      difficulty = difficulty,
      language = language,
      questionCount = questions.size.toLong(),
      timeLimitSeconds = timeLimitSeconds,
      isPublic = isPublic,
      attemptCount = 0L,
      averageScore = 0.0
    )
    quizRef.set(quiz).await()

    // Public question documents never contain correctAnswers.
    for ((index, q) in questions.withIndex()) {
      val qDoc = quizRef.collection("questions").document()
      qDoc.set(mapOf(
        "quizId" to quizRef.id,
        "questionText" to q.questionText,
        "questionType" to q.questionType,
        "options" to q.options,
        "explanation" to q.explanation,
        "orderIndex" to index
      )).await()
      quizRef.collection("answerKeys").document(qDoc.id).set(
        mapOf("correctAnswers" to q.correctAnswers)
      ).await()
    }

    // Create companion post for feed if public
    if (isPublic) {
      val postRef = db.collection("posts").document()
      val post = Post(
        userId = uid,
        authorName = profile.displayName,
        authorUsername = profile.username,
        postType = "quiz",
        title = title,
        content = description.ifBlank { "Test your knowledge on $title ($category - $difficulty)!" },
        category = category,
        quizId = quizRef.id
      )
      postRef.set(post).await()
    }

    // Update user quiz count
    db.collection("users").document(uid).update("quizCount", FieldValue.increment(1)).await()
    return quizRef.id
  }

  override suspend fun updateQuiz(
    quizId: String,
    title: String?,
    description: String?,
    category: String?,
    difficulty: String?,
    language: String?,
    timeLimitSeconds: Long?,
    isPublic: Boolean?,
    questions: List<QuizQuestion>?
  ) {
    val uid = requireUserId()
    val quizRef = db.collection("quizzes").document(quizId)
    val snap = quizRef.get().await()
    val quiz = snap.toObject(Quiz::class.java)
      ?: throw IllegalArgumentException("Quiz with ID $quizId not found.")
    if (quiz.userId != uid) {
      throw IllegalStateException("Only the author of the quiz can update its content.")
    }

    val updates = mutableMapOf<String, Any>()
    title?.let { updates["title"] = it }
    description?.let { updates["description"] = it }
    category?.let { updates["category"] = it }
    difficulty?.let { updates["difficulty"] = it }
    language?.let { updates["language"] = it }
    timeLimitSeconds?.let { updates["timeLimitSeconds"] = it }
    isPublic?.let { updates["isPublic"] = it }
    questions?.let { updates["questionCount"] = it.size.toLong() }
    updates["updatedAt"] = FieldValue.serverTimestamp()

    quizRef.update(updates).await()

    // Replace questions subcollection if updated questions provided
    if (questions != null) {
      try {
        val existingQuestionsSnap = quizRef.collection("questions").get().await()
        val existingKeysSnap = quizRef.collection("answerKeys").get().await()
        for (qDoc in existingQuestionsSnap.documents) qDoc.reference.delete().await()
        for (keyDoc in existingKeysSnap.documents) keyDoc.reference.delete().await()
        for ((index, q) in questions.withIndex()) {
          val newQDoc = quizRef.collection("questions").document()
          newQDoc.set(mapOf(
            "quizId" to quizId,
            "questionText" to q.questionText,
            "questionType" to q.questionType,
            "options" to q.options,
            "explanation" to q.explanation,
            "orderIndex" to index
          )).await()
          quizRef.collection("answerKeys").document(newQDoc.id).set(
            mapOf("correctAnswers" to q.correctAnswers)
          ).await()
        }
      } catch (_: Exception) {}
    }

    // Keep companion feed post in sync
    try {
      val postsSnap = db.collection("posts")
        .whereEqualTo("quizId", quizId)
        .whereEqualTo("userId", uid)
        .get()
        .await()
      for (pDoc in postsSnap.documents) {
        val postUpdates = mutableMapOf<String, Any>()
        title?.let { postUpdates["title"] = it }
        description?.let { postUpdates["content"] = it }
        category?.let { postUpdates["category"] = it }
        postUpdates["updatedAt"] = FieldValue.serverTimestamp()
        pDoc.reference.update(postUpdates).await()
      }
    } catch (_: Exception) {}

    // Update Room SQLite local cache
    try {
      val updatedQuiz = quiz.copy(
        title = title ?: quiz.title,
        description = description ?: quiz.description,
        category = category ?: quiz.category,
        difficulty = difficulty ?: quiz.difficulty,
        language = language ?: quiz.language,
        timeLimitSeconds = timeLimitSeconds ?: quiz.timeLimitSeconds,
        isPublic = isPublic ?: quiz.isPublic,
        questionCount = questions?.size?.toLong() ?: quiz.questionCount
      )
      quizDao?.insertQuiz(updatedQuiz.toCachedEntity())
    } catch (_: Exception) {}
  }

  override suspend fun createPoll(
    question: String,
    description: String,
    category: String,
    options: List<String>,
    isMultipleChoice: Boolean,
    allowVoteChange: Boolean,
    isAnonymous: Boolean,
    isPublic: Boolean
  ): String {
    val uid = requireUserId()
    val profile = getOrCreateUserProfile()
    val pollRef = db.collection("polls").document()
    val initialVotes = options.map { 0L }
    val poll = Poll(
      userId = uid,
      authorName = profile.displayName,
      authorUsername = profile.username,
      question = question,
      description = description,
      category = category,
      options = options,
      optionVotes = initialVotes,
      totalVotes = 0L,
      isMultipleChoice = isMultipleChoice,
      allowVoteChange = allowVoteChange,
      isAnonymous = isAnonymous,
      isPublic = isPublic
    )
    pollRef.set(poll).await()

    if (isPublic) {
      val postRef = db.collection("posts").document()
      val post = Post(
        userId = uid,
        authorName = profile.displayName,
        authorUsername = profile.username,
        postType = "poll",
        title = question,
        content = description.ifBlank { "Cast your vote and join the discussion!" },
        category = category,
        pollId = pollRef.id
      )
      postRef.set(post).await()
    }

    db.collection("users").document(uid).update("pollCount", FieldValue.increment(1)).await()
    return pollRef.id
  }

  override suspend fun updatePoll(
    pollId: String,
    question: String?,
    description: String?,
    category: String?,
    allowVoteChange: Boolean?,
    isPublic: Boolean?
  ) {
    val uid = requireUserId()
    val pollRef = db.collection("polls").document(pollId)
    val snap = pollRef.get().await()
    val poll = snap.toObject(Poll::class.java)
      ?: throw IllegalArgumentException("Poll with ID $pollId not found.")
    if (poll.userId != uid) {
      throw IllegalStateException("Only the author of the poll can update its settings.")
    }

    val updates = mutableMapOf<String, Any>()
    question?.let { updates["question"] = it }
    description?.let { updates["description"] = it }
    category?.let { updates["category"] = it }
    allowVoteChange?.let { updates["allowVoteChange"] = it }
    isPublic?.let { updates["isPublic"] = it }
    updates["updatedAt"] = FieldValue.serverTimestamp()

    pollRef.update(updates).await()

    // Keep companion feed post in sync
    try {
      val postsSnap = db.collection("posts")
        .whereEqualTo("pollId", pollId)
        .whereEqualTo("userId", uid)
        .get()
        .await()
      for (pDoc in postsSnap.documents) {
        val postUpdates = mutableMapOf<String, Any>()
        question?.let { postUpdates["title"] = it }
        description?.let { postUpdates["content"] = it }
        category?.let { postUpdates["category"] = it }
        postUpdates["updatedAt"] = FieldValue.serverTimestamp()
        pDoc.reference.update(postUpdates).await()
      }
    } catch (_: Exception) {}

    // Update Room SQLite local cache
    try {
      val updatedPoll = poll.copy(
        question = question ?: poll.question,
        description = description ?: poll.description,
        category = category ?: poll.category,
        allowVoteChange = allowVoteChange ?: poll.allowVoteChange,
        isPublic = isPublic ?: poll.isPublic
      )
      pollDao?.insertPoll(updatedPoll.toCachedEntity())
    } catch (_: Exception) {}
  }

  fun observeFeed(
    tab: String = "For You",
    categoryFilter: String? = null
  ): Flow<List<Post>> = flow {
    requireUserId()
    var baseQuery: Query = db.collection("posts")

    if (categoryFilter != null && categoryFilter != "All") {
      baseQuery = baseQuery.whereEqualTo("category", categoryFilter)
    }

    val query = when (tab) {
      "Trending" -> baseQuery.orderBy("likeCount", Query.Direction.DESCENDING).limit(30)
      "Latest" -> baseQuery.orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
      else -> baseQuery.orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
    }

    // Offline-first: Emit locally cached posts from Room immediately for instant UI render
    postDao?.let { dao ->
      try {
        val cached = if (categoryFilter != null && categoryFilter != "All") {
          dao.getCachedPosts().filter { it.category.equals(categoryFilter, ignoreCase = true) }
        } else {
          dao.getCachedPosts()
        }
        if (cached.isNotEmpty()) {
          emit(cached.map { it.toModel() })
        }
      } catch (_: Exception) {}
    }

    emitAll(
      query.snapshots()
        .map { snapshot ->
          val posts = snapshot.toObjects(Post::class.java)
          // Cache to room if available
          postDao?.let { dao ->
            val entities = posts.map { p -> p.toCachedEntity() }
            dao.insertPosts(entities)
          }
          posts
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, "posts")
          // Return cached posts on error if available
          val cached = postDao?.getCachedPosts()?.map { it.toModel() } ?: emptyList()
          emit(cached)
        }
    )
  }

  suspend fun getPostById(postId: String): Post? {
    requireUserId()
    val doc = db.collection("posts").document(postId).get().await()
    return doc.toObject(Post::class.java)
  }

  suspend fun deletePost(postId: String) {
    val uid = requireUserId()
    val doc = db.collection("posts").document(postId)
    val snap = doc.get().await()
    if (snap.getString("userId") == uid) {
      doc.delete().await()
    }
  }

  suspend fun refreshFeedAndSync(
    tab: String = "For You",
    categoryFilter: String? = null
  ): List<Post> {
    requireUserId()
    var baseQuery: Query = db.collection("posts")
    if (categoryFilter != null && categoryFilter != "All") {
      baseQuery = baseQuery.whereEqualTo("category", categoryFilter)
    }
    val query = when (tab) {
      "Trending" -> baseQuery.orderBy("likeCount", Query.Direction.DESCENDING).limit(30)
      "Latest" -> baseQuery.orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
      else -> baseQuery.orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
    }

    val snapshot = query.get().await()
    val posts = snapshot.toObjects(Post::class.java)

    // Sync into local Room cache
    postDao?.let { dao ->
      dao.insertPosts(posts.map { it.toCachedEntity() })
    }

    // Sync companion quizzes into Room
    posts.filter { it.postType == "quiz" && it.quizId.isNotBlank() }.forEach { post ->
      try {
        val qSnap = db.collection("quizzes").document(post.quizId).get().await()
        val q = qSnap.toObject(Quiz::class.java)
        if (q != null) {
          quizDao?.insertQuiz(q.toCachedEntity())
        }
      } catch (_: Exception) {}
    }

    // Sync companion polls into Room
    posts.filter { it.postType == "poll" && it.pollId.isNotBlank() }.forEach { post ->
      try {
        val pSnap = db.collection("polls").document(post.pollId).get().await()
        val p = pSnap.toObject(Poll::class.java)
        if (p != null) {
          pollDao?.insertPoll(p.toCachedEntity())
        }
      } catch (_: Exception) {}
    }

    return posts
  }

  fun searchCachedPostsWithFuzzyMatching(
    query: String,
    category: String? = null,
    postType: String? = null
  ): Flow<List<Post>> {
    val cleanCategory = if (category == "All") null else category
    val cleanType = if (postType == "All") null else {
      when (postType?.lowercase()) {
        "quizzes" -> "quiz"
        "polls" -> "poll"
        else -> postType?.lowercase()
      }
    }

    return postDao?.searchCachedPostsWithFilters(
      query = query.trim(),
      category = cleanCategory,
      postType = cleanType
    )?.map { list -> list.map { it.toModel() } } ?: flow { emit(emptyList()) }
  }

  // ---------------- LIKES & BOOKMARKS ----------------

  suspend fun toggleLike(postId: String, currentlyLiked: Boolean) {
    requireUserId()
    FirebaseFunctions.getInstance("asia-south1").getHttpsCallable("toggleLike")
      .call(mapOf("postId" to postId)).await()
  }

  fun observeIsLiked(postId: String): Flow<Boolean> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("posts").document(postId).collection("likes").document(uid)
        .snapshots()
        .map { it.exists() }
        .catch { emit(false) }
    )
  }

  suspend fun toggleBookmark(
    postId: String,
    postType: String,
    title: String,
    currentlyBookmarked: Boolean,
    content: String = "",
    category: String = "General Knowledge",
    authorName: String = "",
    quizId: String = "",
    pollId: String = ""
  ) {
    val uid = requireUserId()
    val userBookmarkRef = db.collection("users").document(uid).collection("bookmarks").document(postId)
    val postBookmarkRef = db.collection("posts").document(postId).collection("bookmarks").document(uid)

    if (currentlyBookmarked) {
      try { userBookmarkRef.delete().await() } catch (_: Exception) {}
      try { postBookmarkRef.delete().await() } catch (_: Exception) {}
    } else {
      val bookmark = BookmarkItem(
        userId = uid,
        postId = postId,
        postType = postType,
        title = title,
        content = content,
        category = category,
        authorName = authorName,
        quizId = quizId,
        pollId = pollId
      )
      try { userBookmarkRef.set(bookmark).await() } catch (_: Exception) {}
      try { postBookmarkRef.set(bookmark).await() } catch (_: Exception) {}
    }
  }

  fun observeUserBookmarks(userId: String): Flow<List<BookmarkItem>> = flow {
    emitAll(
      db.collection("users").document(userId).collection("bookmarks")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(BookmarkItem::class.java).sortedByDescending {
            it.createdAt?.toDate()?.time ?: 0L
          }
        }
        .catch { emit(emptyList()) }
    )
  }

  fun observeIsBookmarked(postId: String): Flow<Boolean> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("users").document(uid).collection("bookmarks").document(postId)
        .snapshots()
        .map { it.exists() }
        .catch {
          emitAll(
            db.collection("posts").document(postId).collection("bookmarks").document(uid)
              .snapshots()
              .map { it.exists() }
              .catch { emit(false) }
          )
        }
    )
  }

  // ---------------- COMMENTS & REPLIES ----------------

  fun observeComments(postId: String): Flow<List<Comment>> = flow {
    requireUserId()
    emitAll(
      db.collection("posts").document(postId).collection("comments")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { it.toObjects(Comment::class.java) }
        .catch { e ->
          if (e is Exception) handleFirestoreError(e, OperationType.LIST, "posts/$postId/comments")
          emit(emptyList())
        }
    )
  }

  suspend fun addComment(postId: String, content: String): Comment {
    val uid = requireUserId()
    val profile = getOrCreateUserProfile()
    val commentRef = db.collection("posts").document(postId).collection("comments").document()
    val comment = Comment(
      postId = postId,
      userId = uid,
      authorName = profile.displayName,
      authorUsername = profile.username,
      content = content,
      likeCount = 0L,
      replyCount = 0L
    )
    commentRef.set(comment).await()
    db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1)).await()
    try {
      db.collection("users").document(uid).update("commentCount", FieldValue.increment(1)).await()
    } catch (_: Exception) {}

    // Send real-time notification to post / quiz creator
    try {
      val postDoc = db.collection("posts").document(postId).get().await()
      val post = postDoc.toObject(Post::class.java)
      if (post != null && post.userId != uid && post.userId.isNotBlank()) {
        val notifTitle = if (post.postType == "quiz") "New Quiz Comment" else "New Comment"
        val notifMessage = "${profile.displayName} commented on your ${if (post.postType == "quiz") "quiz" else "post"}: \"${post.title.take(35)}\""
        val notifRef = db.collection("notifications").document()
        val notif = NotificationItem(
          id = notifRef.id,
          recipientUserId = post.userId,
          senderUserId = uid,
          senderName = profile.displayName,
          type = "COMMENT",
          targetId = postId,
          message = notifMessage,
          isRead = false
        )
        notifRef.set(notif).await()
        context?.let { QuizPollMessagingService.showSystemNotification(it, notifTitle, notifMessage) }
      }
    } catch (_: Exception) {}

    return comment.copy(id = commentRef.id)
  }

  suspend fun getOrCreatePostForQuiz(quiz: Quiz): String {
    val uid = requireUserId()
    try {
      val existingQuery = db.collection("posts")
        .whereEqualTo("quizId", quiz.id)
        .limit(1)
        .get()
        .await()
      if (!existingQuery.isEmpty) {
        return existingQuery.documents.first().id
      }
    } catch (_: Exception) {}

    try {
      val directSnap = db.collection("posts").document(quiz.id).get().await()
      if (directSnap.exists()) {
        return quiz.id
      }
    } catch (_: Exception) {}

    // Provision companion post for quiz so comments and discussions can attach
    val postRef = db.collection("posts").document(quiz.id)
    val profile = getOrCreateUserProfile()
    val post = Post(
      id = quiz.id,
      userId = quiz.userId.ifBlank { uid },
      authorName = quiz.authorName.ifBlank { profile.displayName },
      authorUsername = quiz.authorUsername.ifBlank { profile.username },
      postType = "quiz",
      title = quiz.title,
      content = quiz.description.ifBlank { "Test your knowledge on ${quiz.title}!" },
      category = quiz.category,
      quizId = quiz.id,
      commentCount = 0L,
      likeCount = 0L
    )
    postRef.set(post).await()
    return quiz.id
  }

  fun observeCommentsForQuiz(quizId: String): Flow<List<Comment>> = flow {
    requireUserId()
    var targetPostId = quizId
    try {
      val snap = db.collection("posts").whereEqualTo("quizId", quizId).limit(1).get().await()
      if (!snap.isEmpty) {
        targetPostId = snap.documents.first().id
      }
    } catch (_: Exception) {}

    emitAll(observeComments(targetPostId))
  }

  suspend fun addCommentForQuiz(quiz: Quiz, content: String): Comment {
    val postId = getOrCreatePostForQuiz(quiz)
    return addComment(postId, content)
  }

  fun observeReplies(postId: String, commentId: String): Flow<List<CommentReply>> = flow {
    requireUserId()
    emitAll(
      db.collection("posts").document(postId).collection("comments").document(commentId)
        .collection("replies")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { it.toObjects(CommentReply::class.java) }
        .catch { emit(emptyList()) }
    )
  }

  suspend fun addReply(postId: String, commentId: String, content: String): CommentReply {
    val uid = requireUserId()
    val profile = getOrCreateUserProfile()
    val replyRef = db.collection("posts").document(postId).collection("comments").document(commentId)
      .collection("replies").document()
    val reply = CommentReply(
      commentId = commentId,
      postId = postId,
      userId = uid,
      authorName = profile.displayName,
      authorUsername = profile.username,
      content = content
    )
    replyRef.set(reply).await()
    db.collection("posts").document(postId).collection("comments").document(commentId)
      .update("replyCount", FieldValue.increment(1)).await()
    try {
      db.collection("users").document(uid).update("commentCount", FieldValue.increment(1)).await()
    } catch (_: Exception) {}
    return reply.copy(id = replyRef.id)
  }

  // ---------------- DEDICATED COMMENTS COLLECTION (QUIZZES & POLLS) ----------------

  /**
   * Real-time listener for comments collection with sub-collections for quizzes and polls:
   * e.g. comments/quizzes/{quizId}/items or comments/polls/{pollId}/items
   */
  fun observeTargetComments(targetType: String, targetId: String): Flow<List<Comment>> = flow {
    requireUserId()
    val subCol = when (targetType.lowercase()) {
      "quiz", "quizzes" -> "quizzes"
      "poll", "polls" -> "polls"
      else -> "posts"
    }

    emitAll(
      db.collection("comments").document(subCol).collection(targetId)
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { it.toObjects(Comment::class.java) }
        .catch {
          // Fallback to post-level comments
          emitAll(observeComments(targetId))
        }
    )
  }

  suspend fun addTargetComment(targetType: String, targetId: String, content: String): Comment {
    val uid = requireUserId()
    val profile = getOrCreateUserProfile()
    val subCol = when (targetType.lowercase()) {
      "quiz", "quizzes" -> "quizzes"
      "poll", "polls" -> "polls"
      else -> "posts"
    }

    val commentRef = db.collection("comments").document(subCol).collection(targetId).document()
    val comment = Comment(
      id = commentRef.id,
      postId = targetId,
      targetType = subCol,
      targetId = targetId,
      userId = uid,
      authorName = profile.displayName,
      authorUsername = profile.username,
      content = content.trim(),
      likeCount = 0L,
      replyCount = 0L
    )

    commentRef.set(comment).await()

    // Also mirror to posts collection for feed-level backwards compatibility
    try {
      val postCommentRef = db.collection("posts").document(targetId).collection("comments").document(commentRef.id)
      postCommentRef.set(comment).await()
      db.collection("posts").document(targetId).update("commentCount", FieldValue.increment(1)).await()
    } catch (_: Exception) {}

    try {
      db.collection("users").document(uid).update("commentCount", FieldValue.increment(1)).await()
    } catch (_: Exception) {}

    return comment
  }

  suspend fun deleteComment(postId: String, commentId: String) {
    val uid = requireUserId()
    try {
      val commentDoc = db.collection("posts").document(postId).collection("comments").document(commentId).get().await()
      val comment = commentDoc.toObject(Comment::class.java)
      if (comment != null && comment.userId == uid) {
        db.collection("posts").document(postId).collection("comments").document(commentId).delete().await()
        db.collection("posts").document(postId).update("commentCount", FieldValue.increment(-1)).await()
      }
    } catch (_: Exception) {}
  }

  suspend fun deleteTargetComment(targetType: String, targetId: String, commentId: String) {
    val uid = requireUserId()
    val subCol = when (targetType.lowercase()) {
      "quiz", "quizzes" -> "quizzes"
      "poll", "polls" -> "polls"
      else -> "posts"
    }

    try {
      val ref = db.collection("comments").document(subCol).collection(targetId).document(commentId)
      val snap = ref.get().await()
      val comment = snap.toObject(Comment::class.java)
      if (comment != null && comment.userId == uid) {
        ref.delete().await()
      }
    } catch (_: Exception) {}

    deleteComment(targetId, commentId)
  }

  // ---------------- REFERRAL SYSTEM ----------------

  suspend fun processReferralCode(enteredCode: String): Result<String> {
    requireUserId()
    val cleanCode = enteredCode.trim().uppercase()
    if (cleanCode.isBlank()) return Result.failure(IllegalArgumentException("Please enter a valid referral code."))
    return try {
      val response = FirebaseFunctions.getInstance("asia-south1").getHttpsCallable("processReferralCode")
        .call(mapOf("referralCode" to cleanCode)).await()
      val data = response.data as? Map<*, *>
      Result.success(data?.get("message") as? String ?: "Referral applied successfully.")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeReferralRecords(): Flow<List<ReferralRecord>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("referrals")
        .whereEqualTo("referrerUserId", uid)
        .snapshots()
        .map { it.toObjects(ReferralRecord::class.java) }
        .catch { emit(emptyList()) }
    )
  }

  // ---------------- QUIZ SYSTEM ----------------

  override suspend fun getQuiz(quizId: String): Quiz? {
    requireUserId()
    val doc = db.collection("quizzes").document(quizId).get().await()
    return doc.toObject(Quiz::class.java)
  }

  override fun observeQuiz(quizId: String): Flow<Quiz?> = flow {
    requireUserId()
    // Offline-first: emit cached quiz from Room if available
    quizDao?.getQuizById(quizId)?.let { emit(it.toModel()) }

    emitAll(
      db.collection("quizzes").document(quizId)
        .snapshots()
        .map { snapshot ->
          val q = snapshot.toObject(Quiz::class.java)
          if (q != null) {
            quizDao?.insertQuiz(q.toCachedEntity())
          }
          q
        }
        .catch {
          val fallback = quizDao?.getQuizById(quizId)?.toModel()
          emit(fallback)
        }
    )
  }

  override suspend fun getQuizQuestions(quizId: String): List<QuizQuestion> {
    val uid = requireUserId()
    val quizRef = db.collection("quizzes").document(quizId)
    val quizSnap = quizRef.get().await()
    val isOwner = quizSnap.getString("userId") == uid
    val snap = quizRef.collection("questions")
      .orderBy("orderIndex", Query.Direction.ASCENDING)
      .get().await()
    val questions = snap.toObjects(QuizQuestion::class.java)
    if (!isOwner) {
      // Correct answers are never returned to a participant before a completed attempt.
      return questions.map { it.copy(correctAnswers = emptyList()) }
    }
    val answerKeys = quizRef.collection("answerKeys").get().await().documents
      .associate { it.id to (it.get("correctAnswers") as? List<*>)?.mapNotNull { value -> (value as? Number)?.toInt() }.orEmpty() }
    return questions.mapIndexed { index, question ->
      val documentId = snap.documents.getOrNull(index)?.id
      question.copy(correctAnswers = answerKeys[documentId] ?: question.correctAnswers)
    }
  }

  suspend fun getQuizAnswerReview(quizId: String, attemptId: String): Map<Int, List<Int>> {
    requireUserId()
    val response = FirebaseFunctions.getInstance("asia-south1")
      .getHttpsCallable("getQuizAnswerReview")
      .call(mapOf("quizId" to quizId, "attemptId" to attemptId)).await()
    val data = response.data as? Map<*, *> ?: return emptyMap()
    val raw = data["answers"] as? Map<*, *> ?: return emptyMap()
    return raw.mapNotNull { (key, value) ->
      val index = key?.toString()?.toIntOrNull() ?: return@mapNotNull null
      val answers = (value as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
      index to answers
    }.toMap()
  }

  override suspend fun submitQuizAttempt(
    quizId: String,
    quizTitle: String,
    score: Int,
    totalQuestions: Int,
    timeTakenSeconds: Int,
    userAnswers: Map<Int, List<Int>>,
    submissionId: String
  ): QuizAttempt {
    val uid = requireUserId()
    val payload = mapOf(
      "quizId" to quizId,
      "submissionId" to submissionId,
      "timeTakenSeconds" to timeTakenSeconds.coerceIn(0, 3600),
      "answers" to userAnswers.entries.associate { it.key.toString() to it.value }
    )
    val response = FirebaseFunctions.getInstance("asia-south1")
      .getHttpsCallable("submitQuizAttempt")
      .call(payload).await()
    val data = response.data as? Map<*, *>
      ?: throw IllegalStateException("Invalid quiz submission response.")
    val serverScore = (data["score"] as? Number)?.toInt()
      ?: throw IllegalStateException("Server did not return a validated quiz score.")
    val serverTotal = (data["totalQuestions"] as? Number)?.toInt() ?: totalQuestions
    val serverTime = (data["timeTakenSeconds"] as? Number)?.toInt() ?: timeTakenSeconds
    val attemptId = data["attemptId"] as? String ?: ""
    val accuracy = if (serverTotal > 0) serverScore.toDouble() / serverTotal * 100.0 else 0.0
    val attempt = QuizAttempt(
      id = attemptId,
      quizId = quizId,
      userId = uid,
      quizTitle = quizTitle,
      score = serverScore,
      totalQuestions = serverTotal,
      accuracy = accuracy,
      timeTakenSeconds = serverTime
    )
    quizHistoryDao?.insertAttempt(attempt.toCachedEntity())
    return attempt
  }

  fun observeUserAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
    requireUserId()
    // Emit locally cached history if available for instant responsiveness
    quizHistoryDao?.let { dao ->
      val cached = dao.getQuizHistory(userId).map { it.toModel() }
      if (cached.isNotEmpty()) {
        emit(cached)
      }
    }

    emitAll(
      db.collection("quizAttempts")
        .whereEqualTo("userId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          val attempts = snapshot.toObjects(QuizAttempt::class.java)
          quizHistoryDao?.insertAttempts(attempts.map { it.toCachedEntity() })
          attempts
        }
        .catch {
          val cached = quizHistoryDao?.getQuizHistory(userId)?.map { it.toModel() } ?: emptyList()
          emit(cached)
        }
    )
  }

  override fun observeUserQuizzes(userId: String): Flow<List<Quiz>> = flow {
    requireUserId()
    emitAll(
      db.collection("quizzes")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          val quizzes = snapshot.toObjects(Quiz::class.java)
          quizDao?.insertQuizzes(quizzes.map { it.toCachedEntity() })
          quizzes
        }
        .catch { emit(emptyList()) }
    )
  }

  fun observeQuizzes(category: String? = null): Flow<List<Quiz>> = flow {
    requireUserId()
    var q: Query = db.collection("quizzes")
    if (category != null && category != "All" && category.isNotBlank()) {
      q = q.whereEqualTo("category", category)
    }
    emitAll(
      q.snapshots()
        .map { snapshot ->
          val quizzes = snapshot.toObjects(Quiz::class.java).filter { it.isPublic }
          quizDao?.insertQuizzes(quizzes.map { it.toCachedEntity() })
          quizzes
        }
        .catch { emit(emptyList()) }
    )
  }


  /**
   * Observe general quiz competition leaderboard sortable by score, accuracy, and time taken,
   * with timeframe filtering for "This Week" (last 7 days) and "All-Time".
   */
  override fun observeCompetitionLeaderboard(
    timeframe: LeaderboardTimeframe,
    sortBy: LeaderboardSortOption,
    limit: Int
  ): Flow<List<LeaderboardUser>> = flow {
    requireUserId()
    emitAll(
      db.collection("competitionLeaderboards")
        .limit(30)
        .snapshots()
        .map { snapshot ->
          val firestoreEntries = snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val totalScore = (data["totalScore"] as? Number)?.toInt() ?: 0
            val quizzesCompleted = (data["quizzesCompleted"] as? Number)?.toInt() ?: 0
            val accuracy = (data["accuracy"] as? Number)?.toDouble() ?: 0.0
            val timeTakenSeconds = (data["timeTakenSeconds"] as? Number)?.toInt() ?: 0
            val weeklyScore = (data["weeklyScore"] as? Number)?.toInt() ?: 0
            val weeklyAccuracy = (data["weeklyAccuracy"] as? Number)?.toDouble() ?: 0.0
            val weeklyTimeTakenSeconds = (data["weeklyTimeTakenSeconds"] as? Number)?.toInt() ?: 0
            val weeklyQuizzesCompleted = (data["weeklyQuizzesCompleted"] as? Number)?.toInt() ?: 0

            LeaderboardUser(
              userId = (data["userId"] as? String) ?: doc.id,
              displayName = (data["displayName"] as? String) ?: "Scholar",
              username = (data["username"] as? String) ?: "scholar",
              totalScore = totalScore,
              quizzesCompleted = quizzesCompleted,
              accuracy = accuracy,
              timeTakenSeconds = timeTakenSeconds,
              weeklyScore = weeklyScore,
              weeklyAccuracy = weeklyAccuracy,
              weeklyTimeTakenSeconds = weeklyTimeTakenSeconds,
              weeklyQuizzesCompleted = weeklyQuizzesCompleted,
              totalPoints = totalScore * 10,
              badgeTitle = (data["badgeTitle"] as? String) ?: ActivityPointsCalculator.calculateLevel(totalScore * 10).second
            )
          }

          LeaderboardSorter.sortGeneral(firestoreEntries, timeframe, sortBy).take(limit)
        }
        .catch {
          emit(emptyList())
        }
    )
  }

  /**
   * Observe leaderboard for a specific individual quiz sortable by score, accuracy, and time taken.
   */
  override fun observeQuizLeaderboard(
    quizId: String,
    sortBy: LeaderboardSortOption
  ): Flow<List<QuizLeaderboardEntry>> = flow {
    requireUserId()
    // 1. Emit locally cached entries from Room SQLite first for instant offline responsiveness
    quizLeaderboardDao?.let { dao ->
      val cached = dao.getLeaderboard(quizId).map { it.toModel() }
      if (cached.isNotEmpty()) {
        emit(LeaderboardSorter.sortQuiz(cached, sortBy))
      }
    }

    // 2. Real-time Firestore snapshot listener on quizzes/{quizId}/leaderboard
    emitAll(
      db.collection("quizzes").document(quizId).collection("leaderboard")
        .snapshots()
        .map { snapshot ->
          val firestoreEntries = snapshot.toObjects(QuizLeaderboardEntry::class.java)
          val entriesToDisplay = firestoreEntries

          // Cache incoming Firestore entries into Room
          quizLeaderboardDao?.insertEntries(entriesToDisplay.map { it.toCachedEntity() })
          LeaderboardSorter.sortQuiz(entriesToDisplay, sortBy)
        }
        .catch {
          val cached = quizLeaderboardDao?.getLeaderboard(quizId)?.map { it.toModel() } ?: emptyList()
          emit(LeaderboardSorter.sortQuiz(cached, sortBy))
        }
    )
  }

  fun observeTopLeaderboard(limit: Int = 10): Flow<List<LeaderboardUser>> {
    return observeCompetitionLeaderboard(
      timeframe = LeaderboardTimeframe.THIS_WEEK,
      sortBy = LeaderboardSortOption.SCORE,
      limit = limit
    )
  }

  override suspend fun deleteQuiz(quizId: String) {
    val uid = requireUserId()
    val quizDoc = db.collection("quizzes").document(quizId).get().await()
    val quiz = quizDoc.toObject(Quiz::class.java)
    if (quiz?.userId == uid) {
      val questions = db.collection("quizzes").document(quizId).collection("questions").get().await()
      val answerKeys = db.collection("quizzes").document(quizId).collection("answerKeys").get().await()
      val leaderboard = db.collection("quizzes").document(quizId).collection("leaderboard").get().await()
      for (doc in questions.documents) doc.reference.delete().await()
      for (doc in answerKeys.documents) doc.reference.delete().await()
      for (doc in leaderboard.documents) doc.reference.delete().await()
      db.collection("quizzes").document(quizId).delete().await()
      db.collection("users").document(uid).update("quizCount", FieldValue.increment(-1)).await()
      try {
        val postsQuery = db.collection("posts")
          .whereEqualTo("quizId", quizId)
          .whereEqualTo("userId", uid)
          .get()
          .await()
        for (doc in postsQuery.documents) {
          doc.reference.delete().await()
        }
      } catch (_: Exception) {}
      try {
        quizLeaderboardDao?.clearLeaderboard(quizId)
      } catch (_: Exception) {}
    }
  }

  // ---------------- POLL SYSTEM ----------------

  override suspend fun getPoll(pollId: String): Poll? {
    requireUserId()
    val doc = db.collection("polls").document(pollId).get().await()
    return doc.toObject(Poll::class.java)
  }

  override fun observePoll(pollId: String): Flow<Poll?> = flow {
    requireUserId()
    // Offline-first: emit cached poll from Room if available
    pollDao?.getPollById(pollId)?.let { emit(it.toModel()) }

    emitAll(
      db.collection("polls").document(pollId)
        .snapshots()
        .map { snapshot ->
          val p = snapshot.toObject(Poll::class.java)
          if (p != null) {
            pollDao?.insertPoll(p.toCachedEntity())
          }
          p
        }
        .catch {
          val fallback = pollDao?.getPollById(pollId)?.toModel()
          emit(fallback)
        }
    )
  }

  override fun observeUserVote(pollId: String): Flow<PollVote?> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("polls").document(pollId).collection("votes").document(uid)
        .snapshots()
        .map { it.toObject(PollVote::class.java) }
        .catch { emit(null) }
    )
  }

  override suspend fun submitPollVote(pollId: String, selectedOptionIndices: List<Int>) {
    requireUserId()
    FirebaseFunctions.getInstance("asia-south1").getHttpsCallable("castPollVote")
      .call(mapOf("pollId" to pollId, "selectedOptionIndices" to selectedOptionIndices.distinct()))
      .await()
  }

  override fun observeUserVotedPolls(userId: String): Flow<List<Poll>> = flow {
    requireUserId()
    emitAll(
      db.collection("users").document(userId).collection("votedPolls")
        .snapshots()
        .map { snapshot ->
          val pollIds = snapshot.documents.map { it.id }
          if (pollIds.isEmpty()) {
            emptyList()
          } else {
            val polls = pollIds.mapNotNull { pid ->
              try {
                val pDoc = db.collection("polls").document(pid).get().await()
                pDoc.toObject(Poll::class.java)
              } catch (_: Exception) {
                pollDao?.getPollById(pid)?.toModel()
              }
            }
            pollDao?.insertPolls(polls.map { it.toCachedEntity() })
            polls
          }
        }
        .catch {
          val cached = pollDao?.observeUserPolls(userId)?.map { it.map { p -> p.toModel() } }
          if (cached != null) emitAll(cached) else emit(emptyList())
        }
    )
  }

  suspend fun toggleQuizLike(quiz: Quiz, currentlyLiked: Boolean) {
    val postId = getOrCreatePostForQuiz(quiz)
    toggleLike(postId, currentlyLiked)
  }

  fun observeQuizIsLiked(quizId: String): Flow<Boolean> = flow {
    requireUserId()
    var targetPostId = quizId
    try {
      val snap = db.collection("posts").whereEqualTo("quizId", quizId).limit(1).get().await()
      if (!snap.isEmpty) {
        targetPostId = snap.documents.first().id
      }
    } catch (_: Exception) {}
    emitAll(observeIsLiked(targetPostId))
  }

  suspend fun syncFcmToken() {
    try {
      val uid = getCurrentUserId() ?: return
      val token = FirebaseMessaging.getInstance().token.await()
      db.collection("users").document(uid).update(
        mapOf(
          "fcmToken" to token,
          "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (_: Exception) {}
  }

  fun observeWeeklyTrendingSummary(): Flow<WeeklyTrendingSummary> = flow {
    requireUserId()
    emitAll(
      db.collection("posts")
        .orderBy("likeCount", Query.Direction.DESCENDING)
        .limit(30)
        .snapshots()
        .map { snapshot ->
          val posts = snapshot.toObjects(Post::class.java)

          val topQuizzes = posts.filter { it.postType == "quiz" }.map { post ->
            val score = ((post.likeCount * 4) + (post.commentCount * 5) + 24).toInt()
            TrendingMetric(
              id = post.id,
              title = post.title,
              type = "quiz",
              category = post.category,
              authorName = post.authorName,
              score = score,
              playsOrVotes = (post.likeCount * 3) + 15,
              likes = post.likeCount,
              comments = post.commentCount,
              quizId = post.quizId
            )
          }.sortedByDescending { it.score }.take(5)

          val topPolls = posts.filter { it.postType == "poll" }.map { post ->
            val score = ((post.likeCount * 4) + (post.commentCount * 5) + 18).toInt()
            TrendingMetric(
              id = post.id,
              title = post.title,
              type = "poll",
              category = post.category,
              authorName = post.authorName,
              score = score,
              playsOrVotes = (post.likeCount * 2) + 12,
              likes = post.likeCount,
              comments = post.commentCount,
              pollId = post.pollId
            )
          }.sortedByDescending { it.score }.take(5)

          val totalEngagement = topQuizzes.sumOf { it.score.toLong() } + topPolls.sumOf { it.score.toLong() }
          WeeklyTrendingSummary(
            topQuizzes = topQuizzes,
            topPolls = topPolls,
            totalWeeklyEngagements = totalEngagement
          )
        }
        .catch { emit(WeeklyTrendingSummary()) }
    )
  }

  override fun observeUserPolls(userId: String): Flow<List<Poll>> = flow {
    requireUserId()
    emitAll(
      db.collection("polls")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          val polls = snapshot.toObjects(Poll::class.java)
          pollDao?.insertPolls(polls.map { it.toCachedEntity() })
          polls
        }
        .catch {
          val cached = pollDao?.observeUserPolls(userId)?.map { it.map { p -> p.toModel() } }
          if (cached != null) emitAll(cached) else emit(emptyList())
        }
    )
  }

  override suspend fun deletePoll(pollId: String) {
    val uid = requireUserId()
    val pollDoc = db.collection("polls").document(pollId).get().await()
    val poll = pollDoc.toObject(Poll::class.java)
    if (poll?.userId == uid) {
      db.collection("polls").document(pollId).delete().await()
      db.collection("users").document(uid).update("pollCount", FieldValue.increment(-1)).await()
      try {
        val postsQuery = db.collection("posts")
          .whereEqualTo("pollId", pollId)
          .whereEqualTo("userId", uid)
          .get()
          .await()
        for (doc in postsQuery.documents) {
          doc.reference.delete().await()
        }
      } catch (_: Exception) {}
      try {
        pollDao?.deletePoll(pollId)
      } catch (_: Exception) {}
    }
  }

  fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
    requireUserId()
    emitAll(
      db.collection("follows").document(userId).collection("following")
        .snapshots()
        .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        .catch { emit(emptySet()) }
    )
  }

  fun observeSuggestedUsers(currentUserId: String, limit: Long = 12): Flow<List<UserProfile>> = flow {
    requireUserId()
    emitAll(
      db.collection("users")
        .orderBy("quizCount", Query.Direction.DESCENDING)
        .limit(limit.coerceIn(1, 30))
        .snapshots()
        .map { snapshot -> snapshot.toObjects(UserProfile::class.java).filter { it.userId.isNotBlank() && it.userId != currentUserId } }
        .catch { emit(emptyList()) }
    )
  }

  // ---------------- FOLLOWS ----------------

  suspend fun followUser(targetUserId: String, targetDisplayName: String, targetUsername: String) {
    val uid = requireUserId()
    if (uid == targetUserId) return
    FirebaseFunctions.getInstance("asia-south1").getHttpsCallable("followUser")
      .call(mapOf("targetUserId" to targetUserId)).await()
  }

  suspend fun unfollowUser(targetUserId: String) {
    requireUserId()
    FirebaseFunctions.getInstance("asia-south1").getHttpsCallable("unfollowUser")
      .call(mapOf("targetUserId" to targetUserId)).await()
  }

  fun observeIsFollowing(targetUserId: String): Flow<Boolean> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("follows").document(uid).collection("following").document(targetUserId)
        .snapshots()
        .map { it.exists() }
        .catch { emit(false) }
    )
  }

  // ---------------- NOTIFICATIONS ----------------

  fun observeNotifications(): Flow<List<NotificationItem>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("notifications")
        .whereEqualTo("recipientUserId", uid)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { it.toObjects(NotificationItem::class.java) }
        .catch { emit(emptyList()) }
    )
  }

  suspend fun markNotificationRead(notificationId: String) {
    requireUserId()
    db.collection("notifications").document(notificationId).update("isRead", true).await()
  }

  suspend fun deleteNotification(notificationId: String) {
    requireUserId()
    db.collection("notifications").document(notificationId).delete().await()
  }

  // ---------------- MODERATION & REPORTS ----------------

  suspend fun reportContent(targetType: String, targetId: String, reason: String, details: String) {
    val uid = requireUserId()
    val report = ReportItem(
      reporterUserId = uid,
      targetType = targetType,
      targetId = targetId,
      reason = reason,
      details = details
    )
    db.collection("reports").document().set(report).await()
  }

  suspend fun blockUser(targetUserId: String, targetUserName: String) {
    val uid = requireUserId()
    val block = BlockItem(blockedUserId = targetUserId, blockedUserName = targetUserName)
    db.collection("blocks").document(uid).collection("users").document(targetUserId).set(block).await()
  }

  suspend fun unblockUser(targetUserId: String) {
    val uid = requireUserId()
    db.collection("blocks").document(uid).collection("users").document(targetUserId).delete().await()
  }

  fun observeBlockedUsers(): Flow<List<BlockItem>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("blocks").document(uid).collection("users")
        .snapshots()
        .map { it.toObjects(BlockItem::class.java) }
        .catch { emit(emptyList()) }
    )
  }

  // ---------------- ACCOUNT DELETION & EXPORT ----------------

  suspend fun deleteUserAccountWorkflow() {
    val uid = requireUserId()
    // Admin SDK recursively deletes the account's server-side data and Auth identity.
    FirebaseFunctions.getInstance("asia-south1").getHttpsCallable("deleteUserAccount")
      .call(emptyMap<String, Any>()).await()
    auth.signOut()
    postDao?.clearPosts()
    quizDao?.clearQuizzes()
    pollDao?.clearPolls()
    quizHistoryDao?.clearHistoryForUser(uid)
    bookmarkDao?.clearBookmarks(uid)
  }

  // ---------------- SEED STARTER CONTENT ----------------

  suspend fun seedStarterContentIfEmpty() {
    // Production must never create fictional users, quizzes or polls automatically.
    // Real community content is created only by signed-in users.
  }
}


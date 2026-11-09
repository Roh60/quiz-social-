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
    try {
      val customDbId = context.applicationContext.getString(R.string.firestore_database_id)
      if (customDbId.isNotBlank()) {
        FirebaseFirestore.getInstance(customDbId)
      } else {
        FirebaseFirestore.getInstance()
      }
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed to initialize custom Firestore database, falling back to default instance: ${e.message}")
      FirebaseFirestore.getInstance()
    },
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
    val targetUid = userId.ifBlank { auth.currentUser?.uid ?: "" }
    if (targetUid.isBlank()) {
      emit(null)
      return@flow
    }
    emitAll(
      db.collection("users").document(targetUid)
        .snapshots()
        .map { it.toObject(UserProfile::class.java) }
        .catch { e ->
          if (e is Exception) handleFirestoreError(e, OperationType.READ, "users/$targetUid")
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

    // Add questions subcollection
    for ((index, q) in questions.withIndex()) {
      val qDoc = quizRef.collection("questions").document()
      qDoc.set(q.copy(quizId = quizRef.id, orderIndex = index)).await()
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
        for (qDoc in existingQuestionsSnap.documents) {
          qDoc.reference.delete().await()
        }
        for ((index, q) in questions.withIndex()) {
          val newQDoc = quizRef.collection("questions").document()
          newQDoc.set(q.copy(quizId = quizId, orderIndex = index)).await()
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
    val uid = requireUserId()
    val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
    val postRef = db.collection("posts").document(postId)

    if (currentlyLiked) {
      likeRef.delete().await()
      postRef.update("likeCount", FieldValue.increment(-1)).await()
    } else {
      likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
      postRef.update("likeCount", FieldValue.increment(1)).await()
    }
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
    val targetUid = userId.ifBlank { auth.currentUser?.uid ?: "" }
    if (targetUid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("users").document(targetUid).collection("bookmarks")
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
        val notifRef = db.collection("users").document(post.userId).collection("notifications").document()
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
    val uid = requireUserId()
    val cleanCode = enteredCode.trim().uppercase()
    if (cleanCode.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter a valid referral code."))
    }

    val myProfile = getOrCreateUserProfile()
    if (myProfile.referredBy.isNotBlank()) {
      return Result.failure(IllegalStateException("You have already claimed a referral bonus!"))
    }
    if (myProfile.referralCode.equals(cleanCode, ignoreCase = true)) {
      return Result.failure(IllegalArgumentException("You cannot use your own referral code!"))
    }

    val referrerSnap = try {
      db.collection("users")
        .whereEqualTo("referralCode", cleanCode)
        .limit(1)
        .get()
        .await()
    } catch (e: Exception) {
      return Result.failure(e)
    }

    if (referrerSnap.isEmpty) {
      return Result.failure(IllegalArgumentException("Referral code not found. Please verify with your friend."))
    }

    val referrerDoc = referrerSnap.documents.first()
    val referrerId = referrerDoc.id
    val referrerUsername = referrerDoc.getString("username") ?: "Community Friend"

    val refRecordRef = db.collection("referrals").document()
    val refRecord = ReferralRecord(
      id = refRecordRef.id,
      referrerUserId = referrerId,
      referrerUsername = referrerUsername,
      refereeUserId = uid,
      refereeUsername = myProfile.username,
      referralCode = cleanCode,
      pointsAwardedToReferrer = 100L,
      pointsAwardedToReferee = 50L
    )

    try {
      db.runBatch { batch ->
        batch.update(db.collection("users").document(uid), "referredBy", cleanCode)
        batch.update(db.collection("competitionLeaderboards").document(uid), "totalScore", FieldValue.increment(5))
        batch.update(
          db.collection("users").document(referrerId),
          mapOf(
            "referralCount" to FieldValue.increment(1),
            "referralPointsEarned" to FieldValue.increment(100)
          )
        )
        batch.update(db.collection("competitionLeaderboards").document(referrerId), "totalScore", FieldValue.increment(10))
        batch.set(refRecordRef, refRecord)
      }.await()
      return Result.success("Success! You earned +50 Welcome Bonus Points, and @$referrerUsername received +100 points!")
    } catch (e: Exception) {
      return Result.failure(e)
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
    requireUserId()
    val snap = db.collection("quizzes").document(quizId).collection("questions")
      .orderBy("orderIndex", Query.Direction.ASCENDING)
      .get().await()
    return snap.toObjects(QuizQuestion::class.java)
  }

  override suspend fun submitQuizAttempt(
    quizId: String,
    quizTitle: String,
    score: Int,
    totalQuestions: Int,
    timeTakenSeconds: Int
  ): QuizAttempt {
    val uid = requireUserId()
    val accuracy = if (totalQuestions > 0) (score.toDouble() / totalQuestions) * 100 else 0.0
    val attemptRef = db.collection("quizAttempts").document()
    val attempt = QuizAttempt(
      quizId = quizId,
      userId = uid,
      quizTitle = quizTitle,
      score = score,
      totalQuestions = totalQuestions,
      accuracy = accuracy,
      timeTakenSeconds = timeTakenSeconds
    )
    attemptRef.set(attempt).await()

    val userName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Quiz Scholar"
    val userHandle = auth.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "scholar"
    val now = System.currentTimeMillis()

    // Firestore Transaction: Atomic & secure update of quiz and general leaderboards
    try {
      val quizLeaderboardRef = db.collection("quizzes").document(quizId).collection("leaderboard").document(uid)
      val compLeaderboardRef = db.collection("competitionLeaderboards").document(uid)
      val quizDocRef = db.collection("quizzes").document(quizId)

      db.runTransaction { transaction ->
        // 1. Check existing individual quiz leaderboard entry to determine personal best
        val existingQuizEntrySnap = transaction.get(quizLeaderboardRef)
        val prevScore = existingQuizEntrySnap.getLong("score")?.toInt() ?: -1
        val prevAccuracy = existingQuizEntrySnap.getDouble("accuracy") ?: -1.0
        val prevTime = existingQuizEntrySnap.getLong("timeTakenSeconds")?.toInt() ?: Int.MAX_VALUE
        val prevAttempts = existingQuizEntrySnap.getLong("attemptCount")?.toInt() ?: 0

        val isBetter = prevScore < 0 ||
          (score > prevScore) ||
          (score == prevScore && accuracy > prevAccuracy) ||
          (score == prevScore && accuracy == prevAccuracy && timeTakenSeconds < prevTime)

        if (isBetter) {
          transaction.set(
            quizLeaderboardRef,
            mapOf(
              "quizId" to quizId,
              "userId" to uid,
              "displayName" to userName,
              "username" to userHandle,
              "score" to score,
              "totalQuestions" to totalQuestions,
              "accuracy" to accuracy,
              "timeTakenSeconds" to timeTakenSeconds,
              "attemptCount" to (prevAttempts + 1),
              "completedAtEpochMs" to now,
              "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
          )
        } else {
          transaction.set(
            quizLeaderboardRef,
            mapOf(
              "attemptCount" to (prevAttempts + 1),
              "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
          )
        }

        // 2. Aggregate General Competition Leaderboard statistics
        val existingCompSnap = transaction.get(compLeaderboardRef)
        val prevTotalScore = existingCompSnap.getLong("totalScore")?.toInt() ?: 0
        val prevQuizzesCompleted = existingCompSnap.getLong("quizzesCompleted")?.toInt() ?: 0
        val prevOverallAccuracy = existingCompSnap.getDouble("accuracy") ?: 0.0
        val prevTotalTime = existingCompSnap.getLong("timeTakenSeconds")?.toInt() ?: 0

        val newQuizzesCompleted = prevQuizzesCompleted + 1
        val newTotalScore = prevTotalScore + score
        val newAccuracy = if (newQuizzesCompleted > 0) {
          ((prevOverallAccuracy * prevQuizzesCompleted) + accuracy) / newQuizzesCompleted
        } else accuracy
        val newTotalTime = prevTotalTime + timeTakenSeconds

        // Weekly metrics (last 7 days window)
        val lastAttemptTime = existingCompSnap.getLong("lastAttemptEpochMs") ?: 0L
        val isWithinPastWeek = (now - lastAttemptTime) <= (7L * 24 * 3600 * 1000L)
        val prevWeeklyScore = if (isWithinPastWeek) (existingCompSnap.getLong("weeklyScore")?.toInt() ?: 0) else 0
        val prevWeeklyQuizzes = if (isWithinPastWeek) (existingCompSnap.getLong("weeklyQuizzesCompleted")?.toInt() ?: 0) else 0
        val prevWeeklyAcc = if (isWithinPastWeek) (existingCompSnap.getDouble("weeklyAccuracy") ?: 0.0) else 0.0
        val prevWeeklyTime = if (isWithinPastWeek) (existingCompSnap.getLong("weeklyTimeTakenSeconds")?.toInt() ?: 0) else 0

        val newWeeklyQuizzes = prevWeeklyQuizzes + 1
        val newWeeklyScore = prevWeeklyScore + score
        val newWeeklyAcc = if (newWeeklyQuizzes > 0) {
          ((prevWeeklyAcc * prevWeeklyQuizzes) + accuracy) / newWeeklyQuizzes
        } else accuracy
        val newWeeklyTime = prevWeeklyTime + timeTakenSeconds

        transaction.set(
          compLeaderboardRef,
          mapOf(
            "userId" to uid,
            "displayName" to userName,
            "username" to userHandle,
            "totalScore" to newTotalScore,
            "quizzesCompleted" to newQuizzesCompleted,
            "accuracy" to newAccuracy,
            "timeTakenSeconds" to newTotalTime,
            "weeklyScore" to newWeeklyScore,
            "weeklyQuizzesCompleted" to newWeeklyQuizzes,
            "weeklyAccuracy" to newWeeklyAcc,
            "weeklyTimeTakenSeconds" to newWeeklyTime,
            "lastAttemptEpochMs" to now,
            "badgeTitle" to ActivityPointsCalculator.calculateLevel(newTotalScore * 10).second,
            "updatedAt" to FieldValue.serverTimestamp()
          ),
          com.google.firebase.firestore.SetOptions.merge()
        )

        // 3. Increment quiz attemptCount atomically
        transaction.update(quizDocRef, mapOf("attemptCount" to FieldValue.increment(1)))
        null
      }.await()
    } catch (_: Exception) {
      try {
        db.collection("quizzes").document(quizId).update(
          mapOf("attemptCount" to FieldValue.increment(1))
        ).await()
      } catch (_: Exception) {}
    }

    // Cache to Room SQLite for instant offline availability
    quizLeaderboardDao?.let { dao ->
      dao.insertEntry(
        CachedQuizLeaderboardEntity(
          id = "${quizId}_${uid}",
          quizId = quizId,
          userId = uid,
          displayName = userName,
          username = userHandle,
          score = score,
          totalQuestions = totalQuestions,
          accuracy = accuracy,
          timeTakenSeconds = timeTakenSeconds,
          rank = 1,
          attemptCount = 1,
          completedAtEpochMs = now
        )
      )
    }

    val createdAttempt = attempt.copy(id = attemptRef.id)
    quizHistoryDao?.insertAttempt(createdAttempt.toCachedEntity())
    return createdAttempt
  }

  fun observeUserAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
    val targetUid = userId.ifBlank { auth.currentUser?.uid ?: "" }
    if (targetUid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    // Emit locally cached history if available for instant responsiveness
    quizHistoryDao?.let { dao ->
      val cached = dao.getQuizHistory(targetUid).map { it.toModel() }
      if (cached.isNotEmpty()) {
        emit(cached)
      }
    }

    emitAll(
      db.collection("quizAttempts")
        .whereEqualTo("userId", targetUid)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          val attempts = snapshot.toObjects(QuizAttempt::class.java)
          quizHistoryDao?.insertAttempts(attempts.map { it.toCachedEntity() })
          attempts
        }
        .catch {
          val cached = quizHistoryDao?.getQuizHistory(targetUid)?.map { it.toModel() } ?: emptyList()
          emit(cached)
        }
    )
  }

  override fun observeUserQuizzes(userId: String): Flow<List<Quiz>> = flow {
    val targetUid = userId.ifBlank { auth.currentUser?.uid ?: "" }
    if (targetUid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("quizzes")
        .whereEqualTo("userId", targetUid)
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

  private fun getSeededScholars(): List<LeaderboardUser> {
    return listOf(
      LeaderboardUser(
        userId = "scholar_elena",
        displayName = "Dr. Elena Rostov",
        username = "elena_hist",
        quizzesCompleted = 42,
        quizzesCreated = 18,
        pollsCreated = 22,
        commentsContributed = 54,
        totalScore = 410,
        totalPoints = 1420,
        badgeTitle = "Grandmaster",
        accuracy = 96.5,
        timeTakenSeconds = 25,
        weeklyScore = 180,
        weeklyAccuracy = 97.0,
        weeklyTimeTakenSeconds = 24,
        weeklyQuizzesCompleted = 18
      ),
      LeaderboardUser(
        userId = "scholar_david",
        displayName = "David Kumar",
        username = "david_code",
        quizzesCompleted = 38,
        quizzesCreated = 15,
        pollsCreated = 19,
        commentsContributed = 48,
        totalScore = 370,
        totalPoints = 1280,
        badgeTitle = "Master Architect",
        accuracy = 92.0,
        timeTakenSeconds = 34,
        weeklyScore = 160,
        weeklyAccuracy = 93.5,
        weeklyTimeTakenSeconds = 32,
        weeklyQuizzesCompleted = 16
      ),
      LeaderboardUser(
        userId = "scholar_sarah",
        displayName = "Sarah Jenkins",
        username = "sarah_bio",
        quizzesCompleted = 35,
        quizzesCreated = 14,
        pollsCreated = 16,
        commentsContributed = 42,
        totalScore = 340,
        totalPoints = 1150,
        badgeTitle = "Senior Scholar",
        accuracy = 94.0,
        timeTakenSeconds = 29,
        weeklyScore = 150,
        weeklyAccuracy = 95.0,
        weeklyTimeTakenSeconds = 28,
        weeklyQuizzesCompleted = 15
      ),
      LeaderboardUser(
        userId = "scholar_sophia",
        displayName = "Sophia Chen",
        username = "sophia_math",
        quizzesCompleted = 27,
        quizzesCreated = 12,
        pollsCreated = 10,
        commentsContributed = 29,
        totalScore = 265,
        totalPoints = 890,
        badgeTitle = "Logic Master",
        accuracy = 98.0,
        timeTakenSeconds = 20,
        weeklyScore = 115,
        weeklyAccuracy = 99.0,
        weeklyTimeTakenSeconds = 18,
        weeklyQuizzesCompleted = 11
      ),
      LeaderboardUser(
        userId = "scholar_marcus",
        displayName = "Marcus Aurelius",
        username = "marcus_thought",
        quizzesCompleted = 29,
        quizzesCreated = 11,
        pollsCreated = 14,
        commentsContributed = 35,
        totalScore = 280,
        totalPoints = 980,
        badgeTitle = "Philosophy Lead",
        accuracy = 89.5,
        timeTakenSeconds = 45,
        weeklyScore = 120,
        weeklyAccuracy = 90.0,
        weeklyTimeTakenSeconds = 43,
        weeklyQuizzesCompleted = 12
      ),
      LeaderboardUser(
        userId = "scholar_alex",
        displayName = "Alex Rivera",
        username = "alex_space",
        quizzesCompleted = 24,
        quizzesCreated = 9,
        pollsCreated = 12,
        commentsContributed = 24,
        totalScore = 230,
        totalPoints = 760,
        badgeTitle = "Science Pioneer",
        accuracy = 87.5,
        timeTakenSeconds = 38,
        weeklyScore = 95,
        weeklyAccuracy = 88.0,
        weeklyTimeTakenSeconds = 36,
        weeklyQuizzesCompleted = 9
      ),
      LeaderboardUser(
        userId = "scholar_chloe",
        displayName = "Chloe Bennett",
        username = "chloe_pop",
        quizzesCompleted = 21,
        quizzesCreated = 8,
        pollsCreated = 15,
        commentsContributed = 31,
        totalScore = 205,
        totalPoints = 670,
        badgeTitle = "Pop Culture Guru",
        accuracy = 85.0,
        timeTakenSeconds = 40,
        weeklyScore = 85,
        weeklyAccuracy = 86.0,
        weeklyTimeTakenSeconds = 39,
        weeklyQuizzesCompleted = 8
      ),
      LeaderboardUser(
        userId = "scholar_priya",
        displayName = "Priya Sharma",
        username = "priya_lit",
        quizzesCompleted = 19,
        quizzesCreated = 7,
        pollsCreated = 9,
        commentsContributed = 26,
        totalScore = 185,
        totalPoints = 590,
        badgeTitle = "Literature Lead",
        accuracy = 91.0,
        timeTakenSeconds = 31,
        weeklyScore = 75,
        weeklyAccuracy = 92.0,
        weeklyTimeTakenSeconds = 30,
        weeklyQuizzesCompleted = 7
      ),
      LeaderboardUser(
        userId = "scholar_liam",
        displayName = "Liam O'Connor",
        username = "liam_sports",
        quizzesCompleted = 17,
        quizzesCreated = 6,
        pollsCreated = 11,
        commentsContributed = 20,
        totalScore = 165,
        totalPoints = 520,
        badgeTitle = "Sports Analyst",
        accuracy = 84.0,
        timeTakenSeconds = 48,
        weeklyScore = 60,
        weeklyAccuracy = 85.0,
        weeklyTimeTakenSeconds = 46,
        weeklyQuizzesCompleted = 6
      )
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

          val seeded = getSeededScholars()
          val mergedMap = linkedMapOf<String, LeaderboardUser>()
          seeded.forEach { mergedMap[it.userId] = it }
          firestoreEntries.forEach { mergedMap[it.userId] = it }

          val sorted = LeaderboardSorter.sortGeneral(mergedMap.values.toList(), timeframe, sortBy)
          sorted.take(limit)
        }
        .catch {
          emit(LeaderboardSorter.sortGeneral(getSeededScholars(), timeframe, sortBy).take(limit))
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
          val entriesToDisplay = if (firestoreEntries.isNotEmpty()) {
            firestoreEntries
          } else {
            // Provide realistic competition benchmarks for new quizzes
            listOf(
              QuizLeaderboardEntry(
                id = "${quizId}_elena",
                quizId = quizId,
                userId = "scholar_elena",
                displayName = "Dr. Elena Rostov",
                username = "elena_hist",
                score = 10,
                totalQuestions = 10,
                accuracy = 100.0,
                timeTakenSeconds = 24,
                completedAtEpochMs = System.currentTimeMillis() - 3600000L
              ),
              QuizLeaderboardEntry(
                id = "${quizId}_david",
                quizId = quizId,
                userId = "scholar_david",
                displayName = "David Kumar",
                username = "david_code",
                score = 9,
                totalQuestions = 10,
                accuracy = 90.0,
                timeTakenSeconds = 31,
                completedAtEpochMs = System.currentTimeMillis() - 7200000L
              ),
              QuizLeaderboardEntry(
                id = "${quizId}_sarah",
                quizId = quizId,
                userId = "scholar_sarah",
                displayName = "Sarah Jenkins",
                username = "sarah_bio",
                score = 8,
                totalQuestions = 10,
                accuracy = 80.0,
                timeTakenSeconds = 28,
                completedAtEpochMs = System.currentTimeMillis() - 10800000L
              ),
              QuizLeaderboardEntry(
                id = "${quizId}_sophia",
                quizId = quizId,
                userId = "scholar_sophia",
                displayName = "Sophia Chen",
                username = "sophia_math",
                score = 8,
                totalQuestions = 10,
                accuracy = 80.0,
                timeTakenSeconds = 22,
                completedAtEpochMs = System.currentTimeMillis() - 14400000L
              )
            )
          }

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
    val selectedOptions = selectedOptionIndices
    val uid = requireUserId()
    val pollRef = db.collection("polls").document(pollId)
    val voteRef = pollRef.collection("votes").document(uid)

    db.runTransaction { transaction ->
      val pollSnap = transaction.get(pollRef)
      val currentPoll = pollSnap.toObject(Poll::class.java) ?: return@runTransaction
      val voteSnap = transaction.get(voteRef)

      val previousVote = if (voteSnap.exists()) voteSnap.toObject(PollVote::class.java) else null

      if (previousVote != null && !currentPoll.allowVoteChange) {
        throw IllegalStateException("Vote change is not allowed on this poll.")
      }

      val updatedOptionVotes = currentPoll.optionVotes.toMutableList()

      // Decrement previous votes if changing
      previousVote?.selectedOptions?.forEach { prevIndex ->
        if (prevIndex in updatedOptionVotes.indices) {
          updatedOptionVotes[prevIndex] = (updatedOptionVotes[prevIndex] - 1).coerceAtLeast(0)
        }
      }

      // Increment new votes
      selectedOptions.forEach { newIndex ->
        if (newIndex in updatedOptionVotes.indices) {
          updatedOptionVotes[newIndex] = updatedOptionVotes[newIndex] + 1
        }
      }

      val deltaTotal = if (previousVote == null) 1L else 0L

      transaction.update(
        pollRef,
        mapOf(
          "optionVotes" to updatedOptionVotes,
          "totalVotes" to currentPoll.totalVotes + deltaTotal
        )
      )

      val newVote = PollVote(
        userId = uid,
        pollId = pollId,
        selectedOptions = selectedOptions
      )
      transaction.set(voteRef, newVote)
    }.await()

    // Persist to user's completed voted polls collection
    try {
      db.collection("users").document(uid).collection("votedPolls").document(pollId)
        .set(
          mapOf(
            "pollId" to pollId,
            "selectedOptions" to selectedOptions,
            "votedAt" to FieldValue.serverTimestamp()
          )
        ).await()
    } catch (_: Exception) {}

    // Send real-time notification to poll author
    try {
      val pollSnap = db.collection("polls").document(pollId).get().await()
      val poll = pollSnap.toObject(Poll::class.java)
      if (poll != null && poll.userId != uid && poll.userId.isNotBlank()) {
        val notifTitle = "New Poll Vote"
        val notifMessage = "Someone voted on your poll: \"${poll.question.take(35)}\""
        val notifRef = db.collection("users").document(poll.userId).collection("notifications").document()
        val notif = NotificationItem(
          id = notifRef.id,
          recipientUserId = poll.userId,
          senderUserId = uid,
          senderName = "Community Voter",
          type = "POLL_VOTE",
          targetId = pollId,
          message = notifMessage,
          isRead = false
        )
        notifRef.set(notif).await()
        context?.let { QuizPollMessagingService.showSystemNotification(it, notifTitle, notifMessage) }
      }
    } catch (_: Exception) {}
  }

  override fun observeUserVotedPolls(userId: String): Flow<List<Poll>> = flow {
    val targetUid = userId.ifBlank { auth.currentUser?.uid ?: "" }
    if (targetUid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("users").document(targetUid).collection("votedPolls")
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
    val targetUid = userId.ifBlank { auth.currentUser?.uid ?: "" }
    if (targetUid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("polls")
        .whereEqualTo("userId", targetUid)
        .snapshots()
        .map { snapshot ->
          val polls = snapshot.toObjects(Poll::class.java)
          pollDao?.insertPolls(polls.map { it.toCachedEntity() })
          polls
        }
        .catch {
          val cached = pollDao?.observeUserPolls(targetUid)?.map { it.map { p -> p.toModel() } }
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

  // ---------------- FOLLOWS ----------------

  suspend fun followUser(targetUserId: String, targetDisplayName: String, targetUsername: String) {
    val uid = requireUserId()
    if (uid == targetUserId) return
    val userProfile = getOrCreateUserProfile()

    val myFollowingRef = db.collection("follows").document(uid).collection("following").document(targetUserId)
    val theirFollowersRef = db.collection("follows").document(targetUserId).collection("followers").document(uid)

    myFollowingRef.set(
      mapOf(
        "userId" to targetUserId,
        "displayName" to targetDisplayName,
        "username" to targetUsername,
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()

    theirFollowersRef.set(
      mapOf(
        "userId" to uid,
        "displayName" to userProfile.displayName,
        "username" to userProfile.username,
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()

    db.collection("users").document(uid).update("followingCount", FieldValue.increment(1)).await()
    db.collection("users").document(targetUserId).update("followersCount", FieldValue.increment(1)).await()
  }

  suspend fun unfollowUser(targetUserId: String) {
    val uid = requireUserId()
    db.collection("follows").document(uid).collection("following").document(targetUserId).delete().await()
    db.collection("follows").document(targetUserId).collection("followers").document(uid).delete().await()
    db.collection("users").document(uid).update("followingCount", FieldValue.increment(-1)).await()
    db.collection("users").document(targetUserId).update("followersCount", FieldValue.increment(-1)).await()
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
    val authUser = auth.currentUser

    // 1. Delete user posts in /posts
    try {
      val postsSnap = db.collection("posts").whereEqualTo("userId", uid).get().await()
      for (pDoc in postsSnap.documents) {
        try {
          val comments = pDoc.reference.collection("comments").get().await()
          for (c in comments.documents) c.reference.delete().await()
          val likes = pDoc.reference.collection("likes").get().await()
          for (l in likes.documents) l.reference.delete().await()
        } catch (_: Exception) {}
        pDoc.reference.delete().await()
      }
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed deleting user posts: ${e.message}")
    }

    // 2. Delete user quizzes in /quizzes
    try {
      val quizzesSnap = db.collection("quizzes").whereEqualTo("userId", uid).get().await()
      for (qDoc in quizzesSnap.documents) {
        try {
          val questions = qDoc.reference.collection("questions").get().await()
          for (q in questions.documents) q.reference.delete().await()
          val lb = qDoc.reference.collection("leaderboard").get().await()
          for (entry in lb.documents) entry.reference.delete().await()
        } catch (_: Exception) {}
        qDoc.reference.delete().await()
      }
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed deleting user quizzes: ${e.message}")
    }

    // 3. Delete user polls in /polls
    try {
      val pollsSnap = db.collection("polls").whereEqualTo("userId", uid).get().await()
      for (pDoc in pollsSnap.documents) {
        try {
          val votes = pDoc.reference.collection("votes").get().await()
          for (v in votes.documents) v.reference.delete().await()
        } catch (_: Exception) {}
        pDoc.reference.delete().await()
      }
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed deleting user polls: ${e.message}")
    }

    // 4. Delete user quiz attempts in /quizAttempts
    try {
      val attemptsSnap = db.collection("quizAttempts").whereEqualTo("userId", uid).get().await()
      for (aDoc in attemptsSnap.documents) {
        aDoc.reference.delete().await()
      }
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed deleting quiz attempts: ${e.message}")
    }

    // 5. Delete bookmarks, votedPolls, and notifications subcollections in /users/{uid}
    try {
      val bookmarks = db.collection("users").document(uid).collection("bookmarks").get().await()
      for (b in bookmarks.documents) b.reference.delete().await()

      val votedPolls = db.collection("users").document(uid).collection("votedPolls").get().await()
      for (vp in votedPolls.documents) vp.reference.delete().await()

      val notifications = db.collection("users").document(uid).collection("notifications").get().await()
      for (n in notifications.documents) n.reference.delete().await()
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed deleting user subcollections: ${e.message}")
    }

    // 6. Delete competition leaderboard entry in /competitionLeaderboards/{uid}
    try {
      db.collection("competitionLeaderboards").document(uid).delete().await()
    } catch (_: Exception) {}

    // 7. Delete username reservation
    try {
      val userDoc = db.collection("users").document(uid).get().await()
      val username = userDoc.getString("username")
      if (!username.isNullOrBlank()) {
        db.collection("usernames").document(username).delete().await()
      }
    } catch (_: Exception) {}

    // 8. Delete user profile document
    try {
      db.collection("users").document(uid).delete().await()
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Failed deleting user profile document: ${e.message}")
    }

    // 9. Evict local Room SQLite databases
    try {
      postDao?.clearPosts()
      quizDao?.clearQuizzes()
      pollDao?.clearPolls()
      quizHistoryDao?.clearHistoryForUser(uid)
      bookmarkDao?.clearBookmarks(uid)
    } catch (_: Exception) {}

    // 10. Delete Firebase Authentication account permanently
    try {
      authUser?.delete()?.await()
    } catch (e: Exception) {
      Log.w("QuizPollRepository", "Auth user deletion error (may require re-auth): ${e.message}")
    }
  }

  // ---------------- SEED STARTER CONTENT ----------------

  suspend fun seedStarterContentIfEmpty() {
    try {
      val postsSnap = db.collection("posts").limit(1).get().await()
      if (!postsSnap.isEmpty) return // already has content

      // 1. Seed Kotlin & Compose Quiz
      createQuiz(
        title = "Kotlin & Jetpack Compose Masters",
        description = "Test your knowledge of StateFlow, Recomposition, Coroutines, and MVVM Architecture.",
        category = "Programming",
        difficulty = "Medium",
        language = "English",
        timeLimitSeconds = 60,
        isPublic = true,
        questions = listOf(
          QuizQuestion(
            questionText = "Which function in Compose collects StateFlow safely with Android lifecycle awareness?",
            questionType = "SINGLE_MCQ",
            options = listOf("collectAsState()", "collectAsStateWithLifecycle()", "observeAsState()", "toState()"),
            correctAnswers = listOf(1),
            explanation = "collectAsStateWithLifecycle() pauses Flow emission when the Android Lifecycle falls below the specified min active state."
          ),
          QuizQuestion(
            questionText = "What is the primary purpose of remember in a Jetpack Compose composable?",
            questionType = "SINGLE_MCQ",
            options = listOf("Store state across activity restarts", "Preserve state across recompositions", "Persist state to SQLite", "Synchronize with Firebase"),
            correctAnswers = listOf(1),
            explanation = "remember stores a value in the Composition memory across recompositions."
          ),
          QuizQuestion(
            questionText = "True or False: In Kotlin, val declarations create deeply immutable objects.",
            questionType = "TRUE_FALSE",
            options = listOf("True", "False"),
            correctAnswers = listOf(1),
            explanation = "val only makes the reference read-only; the referenced object's internal properties can still be mutable."
          )
        )
      )

      // 2. Seed Interactive Community Poll
      createPoll(
        question = "Which Android Architecture pattern do you prefer for large scale apps?",
        description = "Vote and see what modern mobile engineers in our community are building with in 2026.",
        category = "Programming",
        options = listOf("MVVM with StateFlow", "MVI with Orbit / Mavericks", "Clean Architecture with UseCases", "Simple ViewModel with LiveData"),
        isMultipleChoice = false,
        allowVoteChange = true,
        isAnonymous = false,
        isPublic = true
      )

      // 3. Seed Science Quiz
      createQuiz(
        title = "Solar System & Planetary Science",
        description = "Explore cosmic mysteries, planetary physics, and astronomical facts.",
        category = "Science",
        difficulty = "Easy",
        language = "English",
        timeLimitSeconds = 45,
        isPublic = true,
        questions = listOf(
          QuizQuestion(
            questionText = "Which planet has the highest surface temperature in our solar system?",
            questionType = "SINGLE_MCQ",
            options = listOf("Mercury", "Venus", "Mars", "Jupiter"),
            correctAnswers = listOf(1),
            explanation = "Venus is hotter than Mercury due to its dense greenhouse atmosphere of carbon dioxide."
          ),
          QuizQuestion(
            questionText = "Which moon of Saturn is famous for active icy geysers and a subsurface ocean?",
            questionType = "SINGLE_MCQ",
            options = listOf("Titan", "Enceladus", "Europa", "Ganymede"),
            correctAnswers = listOf(1),
            explanation = "Enceladus spews water plumes from cryovolcanoes at its south pole."
          )
        )
      )

      // 4. Seed History Quiz
      createQuiz(
        title = "Ancient World Wonders & Historic Empires",
        description = "Discover the architectural achievements and monumental milestones of ancient civilizations.",
        category = "History",
        difficulty = "Medium",
        language = "English",
        timeLimitSeconds = 60,
        isPublic = true,
        questions = listOf(
          QuizQuestion(
            questionText = "Which ancient wonder was located in the city of Babylon?",
            questionType = "SINGLE_MCQ",
            options = listOf("Colossus of Rhodes", "Hanging Gardens", "Lighthouse of Alexandria", "Temple of Artemis"),
            correctAnswers = listOf(1),
            explanation = "The Hanging Gardens of Babylon were one of the Seven Wonders of the Ancient World."
          ),
          QuizQuestion(
            questionText = "In what year did the Western Roman Empire fall?",
            questionType = "SINGLE_MCQ",
            options = listOf("312 AD", "410 AD", "476 AD", "1453 AD"),
            correctAnswers = listOf(2),
            explanation = "476 AD is widely recognized by historians as the fall of the Western Roman Empire."
          )
        )
      )

      // 5. Seed Pop Culture Poll
      createPoll(
        question = "Which cinematic universe has the most iconic world-building?",
        description = "Vote in our weekly pop culture community matchup!",
        category = "Pop Culture",
        options = listOf("The Lord of the Rings", "Star Wars Galaxy", "Marvel Cinematic Universe", "Harry Potter Wizarding World"),
        isMultipleChoice = false,
        allowVoteChange = true,
        isAnonymous = false,
        isPublic = true
      )

      // 6. Seed Discussion
      createDiscussionPost(
        title = "Best practices for offline-first Android apps with Room and Firestore",
        content = "When designing an educational and social app, how do you handle optimistic local mutations versus server-authoritative timestamps in Firestore? Let's discuss cache invalidation strategies and conflict resolution.",
        category = "Technology"
      )
    } catch (_: Exception) {}
  }
}


package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Gives coins after the user does something (posts, finishes a quiz) and returns a short message
 * to show on screen. Nothing fails silently any more: if coins cannot be added, the reason is shown.
 */
class RewardCoordinator(private val db: FirebaseFirestore) {

  private val wallet = WalletRepository(db)

  /** +5 coins for a discussion post (needs at least 15 characters of text). */
  suspend fun rewardDiscussion(postId: String): String? {
    val uid = Firebase.auth.currentUser?.uid ?: return null
    return try {
      val doc = db.collection("posts").document(postId).get().await()
      val title = doc.getString("title") ?: "Discussion"
      val content = doc.getString("content") ?: ""
      if (content.trim().length < 15) {
        "Write at least 15 characters in your post to earn coins."
      } else {
        postMessage(wallet.claimPostReward(uid, postId, title, content))
      }
    } catch (e: Exception) {
      Log.e("RewardCoordinator", "Discussion reward failed", e)
      "Could not add post coins: ${e.message}"
    }
  }

  /** +5 coins for a new quiz or poll. */
  suspend fun rewardCreatedContent(contentId: String, label: String): String? {
    val uid = Firebase.auth.currentUser?.uid ?: return null
    return try {
      postMessage(wallet.claimPostReward(uid, contentId, "New $label", "New $label published by you"))
    } catch (e: Exception) {
      Log.e("RewardCoordinator", "Content reward failed", e)
      "Could not add post coins: ${e.message}"
    }
  }

  /** Daily challenge (+5 coins, up to 5 quizzes a day) and +2 coins for every correct answer. */
  suspend fun rewardQuiz(quizId: String, correctAnswers: Int): String? {
    val uid = Firebase.auth.currentUser?.uid ?: return null
    var total = 0L
    var problem: String? = null

    wallet.recordQuizCompletionDailyChallenge(uid, quizId)
      .onSuccess { total += it }
      .onFailure {
        Log.e("RewardCoordinator", "Daily challenge reward failed", it)
        problem = "Could not add quiz coins: ${it.message}"
      }

    try {
      val quizRef = db.collection("quizzes").document(quizId)
      val ownerId = quizRef.get().await().getString("userId")
      if (correctAnswers > 0 && ownerId != uid) {
        val questionIds = quizRef.collection("questions").get().await().documents.map { it.id }
        questionIds.take(correctAnswers).forEach { questionId ->
          wallet.claimCorrectAnswerReward(uid, questionId, quizId)
            .onSuccess { total += it }
            .onFailure {
              Log.e("RewardCoordinator", "Answer reward failed", it)
              problem = "Could not add answer coins: ${it.message}"
            }
        }
      }
    } catch (e: Exception) {
      Log.e("RewardCoordinator", "Answer rewards failed", e)
      problem = "Could not add answer coins: ${e.message}"
    }

    return if (total > 0) "+$total coins earned!" else problem
  }

  private fun postMessage(result: Result<Long>): String? =
    result.fold(
      onSuccess = { coins -> if (coins > 0) "+$coins coins for your post!" else null },
      onFailure = {
        Log.e("RewardCoordinator", "Post reward failed", it)
        "Could not add post coins: ${it.message}"
      }
    )
}

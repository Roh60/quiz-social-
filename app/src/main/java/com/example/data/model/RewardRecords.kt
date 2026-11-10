package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Immutable record of a user's daily check-in (20 coins once per calendar day).
 */
data class DailyCheckinRecord(
  @DocumentId
  val checkinId: String = "", // Format: ${userId}_${dateString}
  val userId: String = "",
  val dateString: String = "", // yyyy-MM-dd
  val rewardCoins: Long = 20L,
  val streakDays: Int = 1,
  @ServerTimestamp
  val claimedAt: Timestamp? = null
)

/**
 * Tracks daily challenge quiz completions (5 quizzes/day, 5 coins each, max 25 coins).
 */
data class DailyChallengeRecord(
  @DocumentId
  val challengeId: String = "", // Format: ${userId}_${dateString}
  val userId: String = "",
  val dateString: String = "", // yyyy-MM-dd
  val completedQuizIds: List<String> = emptyList(),
  val completedCount: Int = 0,
  val totalCoinsEarned: Long = 0L, // Max 25 coins
  @ServerTimestamp
  val updatedAt: Timestamp? = null
)

/**
 * Ensures each question answer reward is awarded at most once per user.
 */
data class AnswerRewardRecord(
  @DocumentId
  val rewardId: String = "", // Format: ${userId}_${questionId}
  val userId: String = "",
  val questionId: String = "",
  val quizId: String = "",
  val rewardCoins: Long = 2L,
  @ServerTimestamp
  val claimedAt: Timestamp? = null
)

/**
 * Server-validated community post reward record (5 coins per eligible original post).
 */
data class PostRewardRecord(
  @DocumentId
  val postId: String = "",
  val userId: String = "",
  val postTitle: String = "",
  val rewardCoins: Long = 5L,
  val contentSnippet: String = "",
  @ServerTimestamp
  val rewardedAt: Timestamp? = null
)

/**
 * Idempotent audit record for completed rewarded video ad watches.
 */
data class RewardedAdClaimRecord(
  @DocumentId
  val claimId: String = "",
  val userId: String = "",
  val adNetwork: String = "ADMOB", // ADMOB or INMOBI
  val adUnitId: String = "",
  val rewardCoins: Long = 30L,
  val isCashConvertible: Boolean = false,
  @ServerTimestamp
  val claimedAt: Timestamp? = null
)

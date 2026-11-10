package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

/**
 * Immutable financial ledger record for coin and reward operations.
 */
data class CoinTransaction(
  @DocumentId
  val transactionId: String = "",
  val userId: String = "",
  // Transaction Types:
  // REWARDED_AD, POST_REWARD, DAILY_CHALLENGE, CORRECT_ANSWER, DAILY_CHECKIN,
  // REFERRAL_REWARD, WITHDRAWAL_REQUEST, WITHDRAWAL_APPROVED, WITHDRAWAL_REJECTED,
  // WITHDRAWAL_PAID, REFUND_OR_REVERSAL
  val type: String = "CREDIT",
  val source: String = "QUIZ_REWARD",
  val coinAmount: Long = 0L,
  val paisaEquivalent: Long = 0L,
  val isCashConvertible: Boolean = true,
  val description: String = "",
  val idempotencyKey: String = "",
  val status: String = "COMPLETED", // COMPLETED, PENDING, REVERSED, REJECTED
  val verificationStatus: String = "VERIFIED_BACKEND",
  @ServerTimestamp
  val createdAt: Timestamp? = null
) {
  val formattedInr: String
    get() = if (paisaEquivalent > 0L) {
      String.format(Locale.getDefault(), "₹%.2f", paisaEquivalent / 100.0)
    } else {
      "₹0.00"
    }
}

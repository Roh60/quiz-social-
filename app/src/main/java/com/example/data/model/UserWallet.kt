package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

/**
 * Represents a user's secure financial ledger balance in QuizPoll Community.
 *
 * CONVERSION RULES:
 * - 1000 cash-convertible coins = ₹5 INR (500 paise).
 * - Exact integer/paisa accounting is used; floating point numbers are never used for balances.
 * - Non-cash AdMob reward points are kept strictly separate and cannot be converted to cash/UPI.
 */
data class UserWallet(
  @DocumentId
  val userId: String = "",
  // Cash-convertible coins earned from community knowledge challenges, quizzes, and verified offers
  val cashConvertibleCoins: Long = 0L,
  // Strictly in-app, non-transferable reward points (from Google AdMob rewarded ads)
  val appOnlyRewardPoints: Long = 0L,
  // Coins currently held in active withdrawal requests pending review
  val pendingWithdrawalCoins: Long = 0L,
  val pendingWithdrawalPaisa: Long = 0L,
  // Historical payout tracking
  val approvedWithdrawalPaisa: Long = 0L,
  val paidWithdrawalPaisa: Long = 0L,
  val rejectedWithdrawalPaisa: Long = 0L,
  // Lifetime and daily earning stats
  val totalEarnedLifetimeCoins: Long = 0L,
  val todayEarnedCoins: Long = 0L,
  val lastEarningDate: String = "",
  @ServerTimestamp
  val updatedAt: Timestamp? = null
) {
  /**
   * Exact INR value in paise for cash-convertible coins.
   * 1000 coins = ₹5.00 = 500 paise (1 coin = 0.5 paise).
   */
  val inrValuePaise: Long
    get() = (cashConvertibleCoins * 500L) / 1000L

  /**
   * Estimated INR value as a Double for presentation purposes only.
   */
  val inrValueDouble: Double
    get() = inrValuePaise / 100.0

  fun getFormattedInr(): String {
    return String.format(Locale.getDefault(), "₹%.2f", inrValueDouble)
  }

  fun getFormattedPendingInr(): String {
    return String.format(Locale.getDefault(), "₹%.2f", pendingWithdrawalPaisa / 100.0)
  }

  fun getFormattedPaidInr(): String {
    return String.format(Locale.getDefault(), "₹%.2f", paidWithdrawalPaisa / 100.0)
  }

  val totalCombinedDisplayCoins: Long
    get() = cashConvertibleCoins + appOnlyRewardPoints

  val availableCoins: Long
    get() = cashConvertibleCoins

  val pendingCoins: Long
    get() = pendingWithdrawalCoins

  val totalCoinBalance: Long
    get() = cashConvertibleCoins + pendingWithdrawalCoins

  val totalEarnedCoins: Long
    get() = totalEarnedLifetimeCoins

  val totalWithdrawnInr: Double
    get() = paidWithdrawalPaisa / 100.0

  val redeemedCoins: Long
    get() = if (paidWithdrawalPaisa > 0L) (paidWithdrawalPaisa * 1000L) / 500L else 0L
}

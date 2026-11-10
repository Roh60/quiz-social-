package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Global monetization, earning, and withdrawal configuration controlled by administrators.
 *
 * DEFAULT CONVERSION:
 * 1000 coins = ₹5 INR (coinsPerRupee = 200).
 */
data class EarningConfig(
  @DocumentId
  val configId: String = "global",
  val coinsPerRupee: Long = 200L, // 1000 coins = ₹5 INR
  val minWithdrawalCoins: Long = 2000L, // Minimum threshold: 2000 coins = ₹10 INR
  val maxWithdrawalCoinsPerDay: Long = 50000L, // Max per day: 50,000 coins = ₹250 INR
  val isAdMobEnabled: Boolean = true,
  val isInMobiEnabled: Boolean = true,
  val isAdMobRewardedAllowed: Boolean = true,
  // Strict compliance: False unless verified publisher agreement permits real money conversion
  val isInMobiCashConversionAllowed: Boolean = false,
  // Withdrawals stay paused until the wallet is hardened. Admin can open them from the admin screen.
  val isWithdrawalPaused: Boolean = true,
  val pauseReason: String = "Withdrawals will open soon.",
  // Reward amounts for community activities
  val quizWinCoins: Long = 0L, // not part of the agreed economy
  val pollVoteCoins: Long = 10L,
  val admobRewardPoints: Long = 100L, // App-only non-cash points
  @ServerTimestamp
  val updatedAt: Timestamp? = null
) {
  fun calculateInrPaise(coins: Long): Long {
    // 1000 coins = 500 paise (₹5.00) => coins * 500 / 1000 = coins / 2
    return (coins * 500L) / 1000L
  }

  fun calculateInrRupees(coins: Long): Double {
    return calculateInrPaise(coins) / 100.0
  }
}

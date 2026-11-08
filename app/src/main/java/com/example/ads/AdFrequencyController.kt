package com.example.ads

import android.os.SystemClock

/**
 * AdFrequencyController enforces strict advertising policies:
 * - Interstitials: maximum one after three completed quizzes initially
 * - Cooldown periods between full-screen ads
 * - No ads during quiz questions, active timers, or poll voting
 */
class AdFrequencyController {

  private var completedQuizCount: Int = 0
  private var lastInterstitialShownTimestamp: Long = 0L

  @Synchronized
  fun onQuizCompleted(): Boolean {
    completedQuizCount++
    return canShowInterstitial()
  }

  @Synchronized
  fun canShowInterstitial(): Boolean {
    if (!AdConfiguration.isAdsEnabled) return false
    val now = SystemClock.elapsedRealtime()
    val isCooledDown = (now - lastInterstitialShownTimestamp) >= AdConfiguration.INTERSTITIAL_COOLDOWN_MILLIS
    val reachedInterval = completedQuizCount >= AdConfiguration.INTERSTITIAL_QUIZ_INTERVAL
    return reachedInterval && isCooledDown
  }

  @Synchronized
  fun onInterstitialShown() {
    completedQuizCount = 0
    lastInterstitialShownTimestamp = SystemClock.elapsedRealtime()
  }

  fun getCompletedQuizCount(): Int = completedQuizCount

  fun reset() {
    completedQuizCount = 0
    lastInterstitialShownTimestamp = 0L
  }
}

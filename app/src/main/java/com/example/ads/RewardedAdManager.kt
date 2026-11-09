package com.example.ads

import android.app.Activity
import android.util.Log

/**
 * RewardedAdManager handles user-initiated rewarded ads (e.g. bonus hints, extra retry).
 * Explicitly confirms user intent and safely invokes user reward callbacks upon completion.
 */
class RewardedAdManager {

  private var isRewardedAdReady: Boolean = false

  fun loadRewardedAd(activity: Activity) {
    if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) return
    Log.d("RewardedAdManager", "Pre-loading test rewarded ad: ${AdConfiguration.TEST_REWARDED_AD_UNIT_ID}")
    isRewardedAdReady = true
  }

  fun showRewardedAd(
    activity: Activity,
    onUserEarnedReward: (rewardType: String, amount: Int) -> Unit,
    onAdDismissed: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
  ) {
    if (!isRewardedAdReady) {
      onAdFailed("Rewarded video is currently not ready. Please try again shortly.")
      return
    }

    Log.d("RewardedAdManager", "Presenting rewarded ad for user-initiated bonus benefit.")
    isRewardedAdReady = false

    // Simulate completion with reward callback
    onUserEarnedReward("BONUS_QUIZ_HINT", 1)
    onAdDismissed()
  }

  fun isReady(): Boolean = isRewardedAdReady
}

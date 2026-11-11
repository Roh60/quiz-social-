package com.example.ads

import android.app.Activity
import android.util.Log

/**
 * RewardedAdManager shows a real Google AdMob rewarded video (through com.example.data.ads.AdMobManager).
 * The reward is an in-app benefit only (never cash).
 */
class RewardedAdManager {

  fun loadRewardedAd(activity: Activity) {
    if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) return
    Log.d("RewardedAdManager", "Pre-loading AdMob rewarded ad")
    com.example.data.ads.AdMobManager.preloadRewardedAd(activity)
  }

  fun showRewardedAd(
    activity: Activity,
    onUserEarnedReward: (rewardType: String, amount: Int) -> Unit,
    onAdDismissed: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
  ) {
    if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) {
      onAdFailed("Ads are not available right now.")
      return
    }
    com.example.data.ads.AdMobManager.showRewardedAd(
      activity = activity,
      onUserEarnedReward = { points, rewardType ->
        onUserEarnedReward(rewardType, points.toInt())
        onAdDismissed()
      },
      onFailure = { reason -> onAdFailed(reason) }
    )
  }

  fun isReady(): Boolean = com.example.data.ads.AdMobManager.isRewardedAdReady()
}

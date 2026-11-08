package com.example.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Rewards are emitted only by the real SDK's onUserEarnedReward callback. */
class RewardedAdManager {
  @Volatile private var rewardedAd: RewardedAd? = null

  fun loadRewardedAd(activity: Activity) {
    if (!AdConfiguration.isAdsEnabled || !ConsentManager.canRequestAds()) return
    RewardedAd.load(activity, AdConfiguration.rewardedAdUnitId(),
      AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad; Log.d("RewardedAd", "Loaded") }
        override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null; Log.w("RewardedAd", error.message) }
      })
  }

  fun showRewardedAd(activity: Activity, onUserEarnedReward: (String, Int) -> Unit,
                     onAdDismissed: () -> Unit = {}, onAdFailed: (String) -> Unit = {}) {
    val ad = rewardedAd
    if (ad == null) { onAdFailed("Rewarded ad is not ready. Please try again shortly."); return }
    rewardedAd = null
    ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() { loadRewardedAd(activity); onAdDismissed() }
      override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
        loadRewardedAd(activity); onAdFailed(error.message)
      }
    }
    ad.show(activity) { reward -> onUserEarnedReward(reward.type.ifBlank { "BONUS" }, reward.amount) }
  }

  fun isReady(): Boolean = rewardedAd != null
}

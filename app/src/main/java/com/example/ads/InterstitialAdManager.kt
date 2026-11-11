package com.example.ads

import android.app.Activity
import android.util.Log

/**
 * InterstitialAdManager decides WHEN an interstitial may appear (cooldown and frequency caps)
 * and delegates the real ad to the Google AdMob SDK wrapper (com.example.data.ads.AdMobManager).
 */
class InterstitialAdManager(
  private val frequencyController: AdFrequencyController
) {

  fun loadInterstitial(activity: Activity) {
    if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) return
    Log.d("InterstitialAdManager", "Pre-loading AdMob interstitial")
    com.example.data.ads.AdMobManager.preloadInterstitialAd(activity)
  }

  fun showInterstitialIfAllowed(
    activity: Activity,
    onAdDismissed: () -> Unit = {}
  ): Boolean {
    if (!ConsentManager.canRequestAds() || !frequencyController.canShowInterstitial()) {
      onAdDismissed()
      return false
    }
    if (!com.example.data.ads.AdMobManager.isInterstitialReady()) {
      loadInterstitial(activity)
      onAdDismissed()
      return false
    }

    Log.d("InterstitialAdManager", "Showing interstitial ad at natural transition point.")
    frequencyController.onInterstitialShown()
    com.example.data.ads.AdMobManager.showInterstitialAd(activity, onAdDismissed)
    return true
  }

  fun isReady(): Boolean = com.example.data.ads.AdMobManager.isInterstitialReady()
}

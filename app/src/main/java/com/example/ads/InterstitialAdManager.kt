package com.example.ads

import android.app.Activity
import android.util.Log

/**
 * InterstitialAdManager handles full-screen interstitial ad loading and presentation.
 * Adheres strictly to natural transitions, cooldown periods, and frequency caps.
 */
class InterstitialAdManager(
  private val frequencyController: AdFrequencyController
) {

  private var isInterstitialReady: Boolean = false

  fun loadInterstitial(activity: Activity) {
    if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) return

    Log.d("InterstitialAdManager", "Pre-loading test interstitial ad: ${AdConfiguration.TEST_INTERSTITIAL_AD_UNIT_ID}")
    isInterstitialReady = true
  }

  fun showInterstitialIfAllowed(
    activity: Activity,
    onAdDismissed: () -> Unit = {}
  ): Boolean {
    if (!isInterstitialReady || !frequencyController.canShowInterstitial()) {
      onAdDismissed()
      return false
    }

    Log.d("InterstitialAdManager", "Showing interstitial ad at natural transition point.")
    frequencyController.onInterstitialShown()
    isInterstitialReady = false // reset for next cycle

    // In production with Google Mobile Ads SDK, invoke interstitialAd.show(activity)
    onAdDismissed()
    return true
  }

  fun isReady(): Boolean = isInterstitialReady
}

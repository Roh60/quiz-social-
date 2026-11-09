package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Unified AdsManager facade orchestrating all monetization and advertising subsystems:
 * - AdMob & Mediation initialization
 * - Consent and privacy management (UMP)
 * - Frequency capping and natural transitions
 * - Banner, Native, Interstitial, and Rewarded lifecycle
 */
object AdsManager {

  val frequencyController = AdFrequencyController()
  val mediationManager = MediationManager()
  val bannerAdManager = BannerAdManager()
  val nativeAdManager = NativeAdManager()
  val interstitialAdManager = InterstitialAdManager(frequencyController)
  val rewardedAdManager = RewardedAdManager()
  val adMobManager = AdMobManager()

  private var isInitialized = false

  fun initialize(context: Context) {
    if (isInitialized) return

    ConsentManager.initialize(context) {
      adMobManager.initialize(context) {
        mediationManager.initialize(context)
        isInitialized = true
        Log.i("AdsManager", "AdsManager initialized with ${mediationManager.getActivePartners().size} active mediation partners.")
      }
    }
  }

  fun onQuizCompleted(activity: Activity? = null, onDismissed: () -> Unit = {}) {
    val shouldShow = frequencyController.onQuizCompleted()
    if (shouldShow && activity != null) {
      interstitialAdManager.showInterstitialIfAllowed(activity, onDismissed)
    } else {
      onDismissed()
    }
  }

  fun preloadAds(activity: Activity) {
    if (!isInitialized) initialize(activity)
    interstitialAdManager.loadInterstitial(activity)
    rewardedAdManager.loadRewardedAd(activity)
  }

  fun showRewardedAd(
    activity: Activity?,
    onUserEarnedReward: (rewardType: String, amount: Int) -> Unit,
    onAdFailed: (String) -> Unit = {}
  ) {
    if (activity != null) {
      rewardedAdManager.showRewardedAd(activity, onUserEarnedReward, onAdFailed = onAdFailed)
    } else {
      onAdFailed("Activity not available.")
    }
  }
}

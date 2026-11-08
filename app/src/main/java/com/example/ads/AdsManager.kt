package com.example.ads

import android.app.Activity
import android.util.Log

object AdsManager {
  val frequencyController = AdFrequencyController()
  val mediationManager = MediationManager()
  val bannerAdManager = BannerAdManager()
  val nativeAdManager = NativeAdManager()
  val interstitialAdManager = InterstitialAdManager(frequencyController)
  val rewardedAdManager = RewardedAdManager()
  val adMobManager = AdMobManager()
  @Volatile private var isInitialized = false

  fun initialize(activity: Activity) {
    if (isInitialized) return
    ConsentManager.initialize(activity) { canRequest ->
      if (!canRequest || !AdConfiguration.isAdsEnabled) {
        Log.i("AdsManager", "Ads not requested: consent unavailable or ads disabled")
        isInitialized = true
        return@initialize
      }
      adMobManager.initialize(activity) {
        mediationManager.initialize()
        isInitialized = true
        preloadAds(activity)
        Log.i("AdsManager", "Initialized. Present adapters: ${mediationManager.getActivePartners()}")
      }
    }
  }

  fun showPrivacyOptions(activity: Activity) {
    ConsentManager.showPrivacyOptions(activity) {
      if (ConsentManager.canRequestAds()) {
        isInitialized = false
        initialize(activity)
      }
    }
  }

  fun onQuizCompleted(activity: Activity? = null, onDismissed: () -> Unit = {}) {
    val eligible = frequencyController.onQuizCompleted()
    if (eligible && activity != null) interstitialAdManager.showInterstitialIfAllowed(activity, onDismissed)
    else onDismissed()
  }

  fun preloadAds(activity: Activity) {
    if (!isInitialized || !ConsentManager.canRequestAds()) return
    interstitialAdManager.loadInterstitial(activity)
    rewardedAdManager.loadRewardedAd(activity)
  }

  fun showRewardedAd(activity: Activity?, onUserEarnedReward: (String, Int) -> Unit,
                     onAdFailed: (String) -> Unit = {}) {
    if (activity == null) { onAdFailed("Activity not available."); return }
    if (!isInitialized || !ConsentManager.canRequestAds()) { onAdFailed("Ad consent is not ready."); return }
    rewardedAdManager.showRewardedAd(activity, onUserEarnedReward, onAdFailed = onAdFailed)
  }
}

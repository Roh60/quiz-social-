package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Google AdMob SDK initialization, loading, and presentation.
 *
 * POLICY COMPLIANCE:
 * - Rewarded Ads grant ONLY in-app non-transferable reward points (e.g. bonus quiz attempts).
 * - Never converts directly to real-world monetary compensation, UPI, or cash.
 * - Enforces minimum frequency intervals and prevents duplicate reward execution.
 */
object AdMobManager {

  private const val TAG = "AdMobManager"

  private val isInitialized = AtomicBoolean(false)
  private var isAdMobEnabled: Boolean = true

  private var rewardedAd: RewardedAd? = null
  private var isRewardedLoading = false
  private var lastRewardedShowTime = 0L

  private var interstitialAd: InterstitialAd? = null
  private var isInterstitialLoading = false
  private var lastInterstitialShowTime = 0L

  fun initialize(context: Context, onComplete: () -> Unit = {}) {
    if (isInitialized.getAndSet(true)) {
      onComplete()
      return
    }

    try {
      MobileAds.initialize(context) { status ->
        Log.i(TAG, "AdMob MobileAds initialized successfully: $status")
        onComplete()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize AdMob MobileAds", e)
      onComplete()
    }
  }

  fun setAdMobEnabled(enabled: Boolean) {
    isAdMobEnabled = enabled
  }

  fun isEnabled(): Boolean = isAdMobEnabled

  // ---------------- REWARDED ADS ----------------

  fun isRewardedAdReady(): Boolean = isAdMobEnabled && rewardedAd != null

  fun preloadRewardedAd(context: Context, adUnitId: String = AdConstants.ADMOB_PROD_REWARDED_ID) {
    if (!isAdMobEnabled || isRewardedLoading || rewardedAd != null) return

    isRewardedLoading = true
    val adRequest = AdRequest.Builder().build()

    RewardedAd.load(
      context,
      adUnitId,
      adRequest,
      object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) {
          rewardedAd = ad
          isRewardedLoading = false
          Log.d(TAG, "AdMob Rewarded ad loaded and cached.")
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
          rewardedAd = null
          isRewardedLoading = false
          Log.w(TAG, "AdMob Rewarded ad failed to load: ${loadAdError.message}")
        }
      }
    )
  }

  /**
   * Shows the preloaded Rewarded Ad.
   *
   * @param activity The calling activity.
   * @param onUserEarnedReward Callback when the user finishes viewing the ad and earns
   *                           non-transferable App-Only Points.
   * @param onFailure Callback if the ad fails to show or is unavailable.
   */
  fun showRewardedAd(
    activity: Activity,
    onUserEarnedReward: (pointsEarned: Long, rewardType: String) -> Unit,
    onFailure: (reason: String) -> Unit
  ) {
    if (!isAdMobEnabled) {
      onFailure("AdMob is temporarily disabled by platform administration.")
      return
    }

    val currentTime = System.currentTimeMillis()
    if (currentTime - lastRewardedShowTime < AdConstants.MIN_TIME_BETWEEN_REWARDED_ADS_MS) {
      val remainingSec = (AdConstants.MIN_TIME_BETWEEN_REWARDED_ADS_MS - (currentTime - lastRewardedShowTime)) / 1000
      onFailure("Please wait $remainingSec seconds before viewing another sponsored bonus ad.")
      return
    }

    val ad = rewardedAd
    if (ad == null) {
      preloadRewardedAd(activity)
      onFailure("Sponsored video is preparing. Please try again in a few moments.")
      return
    }

    var rewardGranted = false

    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        rewardedAd = null
        lastRewardedShowTime = System.currentTimeMillis()
        // Preload next ad automatically
        preloadRewardedAd(activity)
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        rewardedAd = null
        Log.e(TAG, "Failed to display AdMob rewarded ad: ${adError.message}")
        onFailure("Failed to display ad: ${adError.message}")
      }

      override fun onAdShowedFullScreenContent() {
        Log.d(TAG, "AdMob Rewarded ad displayed on screen.")
      }
    }

    ad.show(activity) { rewardItem ->
      // AdMob User Reward callback: only in-app non-cash points per policy
      if (!rewardGranted) {
        rewardGranted = true
        val amount = if (rewardItem.amount > 0) rewardItem.amount.toLong() else AdConstants.ADMOB_REWARD_QUIZ_POINTS
        val type = rewardItem.type.ifBlank { "APP_BONUS_POINTS" }
        Log.i(TAG, "User earned compliant app-only reward: $amount $type")
        onUserEarnedReward(amount, type)
      }
    }
  }

  // ---------------- INTERSTITIAL ADS ----------------

  fun isInterstitialReady(): Boolean = isAdMobEnabled && interstitialAd != null

  fun preloadInterstitialAd(context: Context, adUnitId: String = AdConstants.ADMOB_PROD_INTERSTITIAL_ID) {
    if (!isAdMobEnabled || isInterstitialLoading || interstitialAd != null) return

    isInterstitialLoading = true
    val adRequest = AdRequest.Builder().build()

    InterstitialAd.load(
      context,
      adUnitId,
      adRequest,
      object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
          interstitialAd = ad
          isInterstitialLoading = false
          Log.d(TAG, "AdMob Interstitial ad loaded.")
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
          interstitialAd = null
          isInterstitialLoading = false
          Log.w(TAG, "AdMob Interstitial ad failed to load: ${loadAdError.message}")
        }
      }
    )
  }

  fun showInterstitialAd(
    activity: Activity,
    onDismissed: () -> Unit = {}
  ) {
    if (!isAdMobEnabled) {
      onDismissed()
      return
    }

    val currentTime = System.currentTimeMillis()
    if (currentTime - lastInterstitialShowTime < AdConstants.MIN_TIME_BETWEEN_INTERSTITIALS_MS) {
      onDismissed()
      return
    }

    val ad = interstitialAd
    if (ad == null) {
      preloadInterstitialAd(activity)
      onDismissed()
      return
    }

    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        interstitialAd = null
        lastInterstitialShowTime = System.currentTimeMillis()
        preloadInterstitialAd(activity)
        onDismissed()
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        interstitialAd = null
        preloadInterstitialAd(activity)
        onDismissed()
      }
    }

    ad.show(activity)
  }
}

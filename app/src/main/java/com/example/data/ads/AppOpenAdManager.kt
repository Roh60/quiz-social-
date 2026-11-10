package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * AppOpenAdManager coordinates loading and displaying Google AdMob App Open Ads
 * when users foreground or resume the app.
 *
 * UX & POLICY RULES:
 * - Does not show during first launch before the app UI has initialized.
 * - Does not interrupt critical workflows (answering quizzes, entering UPI withdrawal details).
 * - Implements a 4-hour ad expiration check and a 5-minute cooldown between displays.
 * - Respects user consent (Google UMP).
 */
object AppOpenAdManager {

  private const val TAG = "AppOpenAdManager"
  private const val FOUR_HOURS_MILLIS = 4 * 3600 * 1000L
  private const val COOLDOWN_MILLIS = 300_000L // 5 minutes

  private var appOpenAd: AppOpenAd? = null
  private var isLoadingAd = false
  private var isShowingAd = false
  private var loadTime: Long = 0L
  private var lastShowTime: Long = 0L

  var isAppOpenEnabled: Boolean = true

  /**
   * Checks whether a valid, non-expired ad is currently cached in memory.
   */
  fun isAdAvailable(): Boolean {
    return isAppOpenEnabled &&
      appOpenAd != null &&
      (Date().time - loadTime) < FOUR_HOURS_MILLIS
  }

  /**
   * Preloads an App Open Ad into memory.
   */
  fun loadAd(context: Context, adUnitId: String = AdConstants.ADMOB_TEST_APP_OPEN_ID) {
    if (!isAppOpenEnabled || isLoadingAd || isAdAvailable()) {
      return
    }

    isLoadingAd = true
    val request = AdRequest.Builder().build()

    AppOpenAd.load(
      context,
      adUnitId,
      request,
      object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
          appOpenAd = ad
          isLoadingAd = false
          loadTime = Date().time
          runCatching { Log.d(TAG, "App Open Ad successfully loaded and cached.") }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
          isLoadingAd = false
          appOpenAd = null
          runCatching { Log.w(TAG, "App Open Ad failed to load: ${loadAdError.message}") }
        }
      }
    )
  }

  /**
   * Displays the App Open Ad if available and not within the cooldown period.
   *
   * @param activity The current visible Activity.
   * @param onComplete Callback invoked when the ad finishes, fails, or is skipped.
   */
  fun showAdIfAvailable(
    activity: Activity,
    onComplete: () -> Unit = {}
  ) {
    if (isShowingAd) {
      runCatching { Log.d(TAG, "App Open Ad is already displaying.") }
      onComplete()
      return
    }

    if (!isAdAvailable()) {
      loadAd(activity)
      onComplete()
      return
    }

    val now = System.currentTimeMillis()
    if (now - lastShowTime < COOLDOWN_MILLIS) {
      runCatching { Log.d(TAG, "App Open Ad skipped: cooldown active.") }
      onComplete()
      return
    }

    val ad = appOpenAd ?: run {
      onComplete()
      return
    }

    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        appOpenAd = null
        isShowingAd = false
        lastShowTime = System.currentTimeMillis()
        loadAd(activity)
        onComplete()
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        appOpenAd = null
        isShowingAd = false
        runCatching { Log.e(TAG, "App Open Ad failed to show: ${adError.message}") }
        loadAd(activity)
        onComplete()
      }

      override fun onAdShowedFullScreenContent() {
        isShowingAd = true
        runCatching { Log.d(TAG, "App Open Ad displayed to user.") }
      }
    }

    isShowingAd = true
    ad.show(activity)
  }
}

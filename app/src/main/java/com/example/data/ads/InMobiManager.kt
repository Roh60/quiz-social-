package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages InMobi SDK initialization, loading, and display of full-screen / rewarded placements.
 *
 * POLICY & PUBLISHER AGREEMENT COMPLIANCE:
 * - Cash-convertible InMobi rewards are DISABLED by default unless explicit written publisher
 *   permission is verified and toggled via the Admin configuration.
 * - Non-cash app rewards (extra quiz attempts / community points) are permitted.
 */
object InMobiManager {

  private const val TAG = "InMobiManager"

  private val isInitialized = AtomicBoolean(false)
  private var isInMobiEnabled: Boolean = true

  // Strict compliance switch: Must be false unless publisher agreement confirms cash conversion
  private var isCashConvertibleRewardAllowed: Boolean = false

  private var rewardedInterstitial: InMobiInterstitial? = null
  private var isRewardedLoading: Boolean = false
  private var lastRewardedShowTime: Long = 0L

  fun initialize(
    context: Context,
    accountId: String = AdConstants.INMOBI_PROD_ACCOUNT_ID,
    onComplete: (success: Boolean) -> Unit = {}
  ) {
    if (isInitialized.getAndSet(true)) {
      onComplete(true)
      return
    }

    try {
      InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
      val consent = JSONObject().apply {
        put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
      }

      InMobiSdk.init(context, accountId, consent, object : SdkInitializationListener {
        override fun onInitializationComplete(error: Error?) {
          if (error != null) {
            Log.e(TAG, "InMobi Initialization Failed: ${error.message}")
            onComplete(false)
          } else {
            Log.i(TAG, "InMobi Initialization Succeeded")
            onComplete(true)
          }
        }
      })
    } catch (e: Exception) {
      Log.e(TAG, "Exception during InMobi SDK initialization", e)
      onComplete(false)
    }
  }

  fun setInMobiEnabled(enabled: Boolean) {
    isInMobiEnabled = enabled
  }

  fun isEnabled(): Boolean = isInMobiEnabled

  fun setCashConvertibleRewardAllowed(allowed: Boolean) {
    isCashConvertibleRewardAllowed = allowed
    runCatching { Log.i(TAG, "InMobi Cash Convertible Reward Allowed set to: $allowed") }
  }

  fun isCashConvertibleRewardAllowed(): Boolean = isCashConvertibleRewardAllowed

  fun isRewardedAdReady(): Boolean = isInMobiEnabled && (rewardedInterstitial?.isReady() == true)

  fun preloadRewardedAd(context: Context, placementId: Long = AdConstants.INMOBI_PROD_REWARDED_PLACEMENT_ID) {
    if (!isInMobiEnabled || isRewardedLoading || (rewardedInterstitial?.isReady() == true)) return

    isRewardedLoading = true

    val listener = object : InterstitialAdEventListener() {
      override fun onAdLoadSucceeded(ad: InMobiInterstitial, info: AdMetaInfo) {
        super.onAdLoadSucceeded(ad, info)
        isRewardedLoading = false
        Log.d(TAG, "InMobi Rewarded Ad loaded successfully.")
      }

      override fun onAdLoadFailed(ad: InMobiInterstitial, status: InMobiAdRequestStatus) {
        super.onAdLoadFailed(ad, status)
        isRewardedLoading = false
        Log.w(TAG, "InMobi Rewarded Ad failed to load: ${status.message} (code: ${status.statusCode})")
      }

      override fun onAdDismissed(ad: InMobiInterstitial) {
        super.onAdDismissed(ad)
        lastRewardedShowTime = System.currentTimeMillis()
        // Preload next
        preloadRewardedAd(context, placementId)
      }
    }

    try {
      rewardedInterstitial = InMobiInterstitial(context, placementId, listener)
      rewardedInterstitial?.load()
    } catch (e: Exception) {
      isRewardedLoading = false
      Log.e(TAG, "Error initiating InMobi interstitial load", e)
    }
  }

  fun showRewardedAd(
    activity: Activity,
    placementId: Long = AdConstants.INMOBI_PROD_REWARDED_PLACEMENT_ID,
    onRewardUnlocked: (rewardValue: Long, isCashConvertible: Boolean) -> Unit,
    onFailure: (reason: String) -> Unit
  ) {
    if (!isInMobiEnabled) {
      onFailure("InMobi network is disabled by platform administration.")
      return
    }

    val currentTime = System.currentTimeMillis()
    if (currentTime - lastRewardedShowTime < AdConstants.MIN_TIME_BETWEEN_REWARDED_ADS_MS) {
      val remainingSec = (AdConstants.MIN_TIME_BETWEEN_REWARDED_ADS_MS - (currentTime - lastRewardedShowTime)) / 1000
      onFailure("Please wait $remainingSec seconds before watching another sponsored video.")
      return
    }

    val ad = rewardedInterstitial
    if (ad == null || !ad.isReady()) {
      preloadRewardedAd(activity, placementId)
      onFailure("Sponsored video is loading. Please try again in a few moments.")
      return
    }

    var rewardGranted = false

    val activeListener = object : InterstitialAdEventListener() {
      override fun onRewardsUnlocked(ad: InMobiInterstitial, rewards: Map<Any, Any>) {
        super.onRewardsUnlocked(ad, rewards)
        if (!rewardGranted) {
          rewardGranted = true
          // Strict compliance: only convertible if admin verified agreement
          val isConvertible = isCashConvertibleRewardAllowed
          val rewardCoins = AdConstants.INMOBI_REWARD_COINS
          Log.i(TAG, "InMobi rewards unlocked: $rewards (granted $rewardCoins, convertible=$isConvertible)")
          onRewardUnlocked(rewardCoins, isConvertible)
        }
      }

      override fun onAdDisplayFailed(ad: InMobiInterstitial) {
        super.onAdDisplayFailed(ad)
        Log.e(TAG, "InMobi onAdDisplayFailed")
        onFailure("Failed to display sponsored video.")
      }

      override fun onAdDismissed(ad: InMobiInterstitial) {
        super.onAdDismissed(ad)
        lastRewardedShowTime = System.currentTimeMillis()
        preloadRewardedAd(activity, placementId)
      }
    }

    try {
      // Re-bind listener for the show event
      val interstitialWithListener = InMobiInterstitial(activity, placementId, activeListener)
      if (ad.isReady()) {
        ad.show()
      } else {
        interstitialWithListener.load()
        onFailure("Video is preparing. Please try again in 5 seconds.")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error displaying InMobi ad", e)
      onFailure("Could not display ad: ${e.message}")
    }
  }
}

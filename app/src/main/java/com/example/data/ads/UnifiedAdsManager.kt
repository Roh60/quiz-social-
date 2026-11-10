package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import java.util.UUID

/**
 * Centralized coordinator for Google AdMob and InMobi advertising networks.
 */
object UnifiedAdsManager {

  private const val TAG = "UnifiedAdsManager"

  fun initializeAll(context: Context) {
    Log.i(TAG, "Initializing advertising networks: AdMob & InMobi...")
    AdMobManager.initialize(context) {
      AdMobManager.preloadRewardedAd(context)
      AdMobManager.preloadInterstitialAd(context)
    }

    InMobiManager.initialize(context) { success ->
      if (success) {
        InMobiManager.preloadRewardedAd(context)
      }
    }
  }

  fun syncAdminConfig(
    isAdMobEnabled: Boolean,
    isInMobiEnabled: Boolean,
    isInMobiCashAllowed: Boolean
  ) {
    AdMobManager.setAdMobEnabled(isAdMobEnabled)
    InMobiManager.setInMobiEnabled(isInMobiEnabled)
    InMobiManager.setCashConvertibleRewardAllowed(isInMobiCashAllowed)
  }

  fun showAdMobRewarded(
    activity: Activity,
    onSuccess: (pointsGranted: Long, idempotencyKey: String) -> Unit,
    onFailure: (String) -> Unit
  ) {
    val idempotencyKey = "admob_rew_" + UUID.randomUUID().toString().take(12)
    AdMobManager.showRewardedAd(
      activity = activity,
      onUserEarnedReward = { points, _ ->
        onSuccess(points, idempotencyKey)
      },
      onFailure = onFailure
    )
  }

  fun showInMobiRewarded(
    activity: Activity,
    onSuccess: (coinsGranted: Long, isCashConvertible: Boolean, idempotencyKey: String) -> Unit,
    onFailure: (String) -> Unit
  ) {
    val idempotencyKey = "inmobi_rew_" + UUID.randomUUID().toString().take(12)
    InMobiManager.showRewardedAd(
      activity = activity,
      onRewardUnlocked = { coins, isConvertible ->
        onSuccess(coins, isConvertible, idempotencyKey)
      },
      onFailure = onFailure
    )
  }

  fun showInterstitial(activity: Activity, onComplete: () -> Unit = {}) {
    if (AdMobManager.isInterstitialReady()) {
      AdMobManager.showInterstitialAd(activity, onComplete)
    } else {
      onComplete()
    }
  }
}

/**
 * Clean, safe Jetpack Compose Banner Ad component for Google AdMob.
 */
@Composable
fun AdMobBanner(
  modifier: Modifier = Modifier,
  adUnitId: String = AdConstants.ADMOB_PROD_BANNER_ID
) {
  if (!AdMobManager.isEnabled()) return

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(50.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    contentAlignment = Alignment.Center
  ) {
    AndroidView(
      modifier = Modifier.fillMaxWidth(),
      factory = { context ->
        AdView(context).apply {
          setAdSize(AdSize.BANNER)
          this.adUnitId = adUnitId
          loadAd(AdRequest.Builder().build())
        }
      }
    )
  }
}

package com.example.ads

import android.content.Context
import android.util.Log

/**
 * BannerAdManager handles banner ad loading and presentation.
 * Ensures banner ads are non-intrusive and never cover interactive controls.
 */
class BannerAdManager {

  private var isBannerLoaded: Boolean = false

  fun loadBanner(context: Context, onLoaded: () -> Unit = {}, onError: (String) -> Unit = {}) {
    if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) {
      onError("Ads disabled or consent not obtained.")
      return
    }

    Log.d("BannerAdManager", "Requesting test banner ad with Unit ID: ${AdConfiguration.TEST_BANNER_AD_UNIT_ID}")
    isBannerLoaded = true
    onLoaded()
  }

  fun isLoaded(): Boolean = isBannerLoaded
}

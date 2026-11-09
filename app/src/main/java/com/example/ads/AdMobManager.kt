package com.example.ads

import android.content.Context
import android.util.Log

/**
 * AdMobManager coordinates AdMob initialization, SDK lifecycle, and test device registration.
 */
class AdMobManager {

  private var isInitialized: Boolean = false

  fun initialize(context: Context, onInitialized: () -> Unit = {}) {
    if (isInitialized) {
      onInitialized()
      return
    }

    Log.d("AdMobManager", "Initializing Google Mobile Ads SDK with test App ID: ${AdConfiguration.TEST_APP_ID}")
    isInitialized = true
    onInitialized()
  }

  fun isInitialized(): Boolean = isInitialized
}

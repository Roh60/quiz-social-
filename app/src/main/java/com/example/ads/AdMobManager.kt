package com.example.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus

class AdMobManager {
  @Volatile private var isInitialized = false

  fun initialize(context: Context, onInitialized: (InitializationStatus?) -> Unit = {}) {
    if (isInitialized) { onInitialized(null); return }
    MobileAds.initialize(context) { status ->
      isInitialized = true
      status.adapterStatusMap.forEach { (name, adapter) ->
        Log.i("AdMobManager", "Adapter $name: ${adapter.initializationState}, ${adapter.description}")
      }
      onInitialized(status)
    }
  }

  fun isInitialized(): Boolean = isInitialized
}

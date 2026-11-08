package com.example.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

class BannerAdManager {
  fun createBanner(context: Context, adUnitId: String = AdConfiguration.bannerAdUnitId()): AdView? {
    if (!AdConfiguration.isAdsEnabled || !ConsentManager.canRequestAds()) return null
    return AdView(context).apply {
      this.adUnitId = adUnitId
      setAdSize(AdSize.BANNER)
      loadAd(AdRequest.Builder().build())
    }
  }
}

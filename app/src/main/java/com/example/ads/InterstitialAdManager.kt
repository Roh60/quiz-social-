package com.example.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val frequencyController: AdFrequencyController) {
  @Volatile private var interstitialAd: InterstitialAd? = null

  fun loadInterstitial(activity: Activity) {
    if (!AdConfiguration.isAdsEnabled || !ConsentManager.canRequestAds()) return
    InterstitialAd.load(activity, AdConfiguration.interstitialAdUnitId(),
      AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad; Log.d("InterstitialAd", "Loaded") }
        override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null; Log.w("InterstitialAd", error.message) }
      })
  }

  fun showInterstitialIfAllowed(activity: Activity, onAdDismissed: () -> Unit = {}): Boolean {
    val ad = interstitialAd
    if (ad == null || !frequencyController.canShowInterstitial()) { onAdDismissed(); return false }
    interstitialAd = null
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() { frequencyController.onInterstitialShown(); loadInterstitial(activity); onAdDismissed() }
      override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) { Log.w("InterstitialAd", error.message); loadInterstitial(activity); onAdDismissed() }
      override fun onAdShowedFullScreenContent() { Log.d("InterstitialAd", "Shown") }
    }
    ad.show(activity)
    return true
  }

  fun isReady(): Boolean = interstitialAd != null
}

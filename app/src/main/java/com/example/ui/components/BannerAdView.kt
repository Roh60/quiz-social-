package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdConfiguration
import com.example.ads.AdsManager
import com.example.ads.ConsentManager
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(modifier: Modifier = Modifier, adUnitId: String = AdConfiguration.bannerAdUnitId()) {
  val context = LocalContext.current
  if (!AdConfiguration.isAdsEnabled || !ConsentManager.canRequestAds()) return
  val adView = androidx.compose.runtime.remember(context, adUnitId) {
    AdsManager.bannerAdManager.createBanner(context, adUnitId)
  } ?: return
  DisposableEffect(adView) { onDispose { adView.destroy() } }
  AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth())
}

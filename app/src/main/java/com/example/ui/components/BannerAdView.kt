package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ads.AdConfiguration
import com.example.ads.ConsentManager
import com.example.data.ads.AdConstants
import com.example.data.ads.AdMobBanner

/**
 * Real Google AdMob banner. Debug builds show Google test ads, release builds show the live ad unit.
 */
@Composable
fun BannerAdView(
  modifier: Modifier = Modifier,
  adUnitId: String = AdConstants.ADMOB_PROD_BANNER_ID
) {
  if (!ConsentManager.canRequestAds() || !AdConfiguration.isAdsEnabled) return

  AdMobBanner(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    adUnitId = adUnitId
  )
}

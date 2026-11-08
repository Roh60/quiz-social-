package com.example.ads

import com.example.BuildConfig

/** Central ad configuration. Test IDs are the safe default until publisher IDs are configured. */
object AdConfiguration {
  const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
  const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
  const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
  const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
  const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

  const val NATIVE_AD_INTERVAL_POSTS = 10
  const val INTERSTITIAL_QUIZ_INTERVAL = 3
  const val INTERSTITIAL_COOLDOWN_MILLIS = 90_000L

  @Volatile var isTestMode: Boolean = BuildConfig.ADS_TEST_MODE
  @Volatile var isAdsEnabled: Boolean = true

  fun bannerAdUnitId(): String = if (isTestMode || BuildConfig.ADMOB_BANNER_AD_UNIT_ID.isBlank()) TEST_BANNER_AD_UNIT_ID else BuildConfig.ADMOB_BANNER_AD_UNIT_ID
  fun interstitialAdUnitId(): String = if (isTestMode || BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID.isBlank()) TEST_INTERSTITIAL_AD_UNIT_ID else BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
  fun rewardedAdUnitId(): String = if (isTestMode || BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.isBlank()) TEST_REWARDED_AD_UNIT_ID else BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
  fun nativeAdUnitId(): String = if (isTestMode || BuildConfig.ADMOB_NATIVE_AD_UNIT_ID.isBlank()) TEST_NATIVE_AD_UNIT_ID else BuildConfig.ADMOB_NATIVE_AD_UNIT_ID

  enum class MediationPartner(val networkName: String, val adapterName: String) {
    ADMOB("Google AdMob", "com.google.ads.mediation.admob.AdMobAdapter"),
    INMOBI("InMobi", "com.google.ads.mediation.inmobi.InMobiMediationAdapter")
  }
}

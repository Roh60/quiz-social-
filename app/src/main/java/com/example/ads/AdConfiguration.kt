package com.example.ads

/**
 * AdConfiguration defines ad unit configurations, official Google AdMob test IDs,
 * frequency rules, and supported mediation partner demand networks.
 */
object AdConfiguration {

  // Test Ad Unit IDs provided officially by Google Mobile Ads SDK for testing
  const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
  const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
  const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
  const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
  const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

  // Policy placement rules
  const val NATIVE_AD_INTERVAL_POSTS = 10 // Native ad inserted approximately every 8-12 organic posts
  const val INTERSTITIAL_QUIZ_INTERVAL = 3 // Maximum one interstitial after three completed quizzes
  const val INTERSTITIAL_COOLDOWN_MILLIS = 90_000L // 90 seconds minimum cooldown between interstitials

  // Flag to keep live IDs disabled until production configuration is supplied in AI Studio Secrets
  var isTestMode: Boolean = true
  var isAdsEnabled: Boolean = true

  /**
   * Potential mediation network demand partners supported via Google Mobile Ads Mediation.
   */
  enum class MediationPartner(
    val networkName: String,
    val adapterName: String,
    val supportsBidding: Boolean
  ) {
    GOOGLE_ADMOB("Google AdMob", "com.google.ads.mediation.admob.AdMobAdapter", true),
    APPLOVIN("AppLovin MAX", "com.google.ads.mediation.applovin.AppLovinMediationAdapter", true),
    UNITY_ADS("Unity Ads", "com.google.ads.mediation.unity.UnityMediationAdapter", true),
    INMOBI("InMobi", "com.google.ads.mediation.inmobi.InMobiMediationAdapter", true),
    META_AUDIENCE_NETWORK("Meta Audience Network", "com.google.ads.mediation.facebook.FacebookMediationAdapter", true),
    MINTEGRAL("Mintegral", "com.google.ads.mediation.mintegral.MintegralMediationAdapter", true),
    PANGLE("Pangle", "com.google.ads.mediation.pangle.PangleMediationAdapter", true)
  }
}

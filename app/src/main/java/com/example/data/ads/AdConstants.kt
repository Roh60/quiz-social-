package com.example.data.ads

/**
 * Centralized ad unit identifiers, placement IDs, and policy configurations for QuizPoll Community.
 *
 * POLICY NOTICE:
 * - Google AdMob Rewarded Ads provide strictly IN-APP non-transferable points/quiz attempts.
 *   They MUST NOT be converted to real-world monetary compensation, UPI, or cash.
 * - InMobi Cash Conversion requires explicit publisher agreement and is disabled by default.
 */
object AdConstants {

  // AdMob Application ID: Configured in AndroidManifest.xml
  const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"

  // Official Google Sample/Test Ad Unit IDs (Use these for development & testing)
  const val ADMOB_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
  const val ADMOB_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
  const val ADMOB_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
  const val ADMOB_TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247690410"
  const val ADMOB_TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257390722"

  // Production AdMob Placeholders (Set real IDs in production build / secrets)
  const val ADMOB_PROD_BANNER_ID = ADMOB_TEST_BANNER_ID
  const val ADMOB_PROD_INTERSTITIAL_ID = ADMOB_TEST_INTERSTITIAL_ID
  const val ADMOB_PROD_REWARDED_ID = ADMOB_TEST_REWARDED_ID
  const val ADMOB_PROD_NATIVE_ID = ADMOB_TEST_NATIVE_ID
  const val ADMOB_PROD_APP_OPEN_ID = ADMOB_TEST_APP_OPEN_ID

  // InMobi Placement & Account Identifiers
  const val INMOBI_TEST_ACCOUNT_ID = "1471550843414"
  const val INMOBI_TEST_BANNER_PLACEMENT_ID = 1471550843415L
  const val INMOBI_TEST_INTERSTITIAL_PLACEMENT_ID = 1471550843416L
  const val INMOBI_TEST_REWARDED_PLACEMENT_ID = 1471550843417L

  // Production InMobi Placeholders
  const val INMOBI_PROD_ACCOUNT_ID = INMOBI_TEST_ACCOUNT_ID
  const val INMOBI_PROD_BANNER_PLACEMENT_ID = INMOBI_TEST_BANNER_PLACEMENT_ID
  const val INMOBI_PROD_INTERSTITIAL_PLACEMENT_ID = INMOBI_TEST_INTERSTITIAL_PLACEMENT_ID
  const val INMOBI_PROD_REWARDED_PLACEMENT_ID = INMOBI_TEST_REWARDED_PLACEMENT_ID

  // Coin Reward System Rules
  const val REWARDED_AD_COINS = 30L // Voluntarily watching 1 complete eligible rewarded ad
  const val COMMUNITY_POST_COINS = 5L // Eligible original post
  const val DAILY_CHALLENGE_PER_QUIZ_COINS = 5L // Per completed challenge quiz
  const val DAILY_CHALLENGE_MAX_COINS = 25L // 5 quizzes * 5 coins = 25 coins max
  const val CORRECT_ANSWER_COINS = 2L // Per eligible correct answer
  const val DAILY_CHECKIN_COINS = 20L // Daily check-in
  const val REFERRAL_REWARD_COINS = 500L // Verified new user referral

  // AdMob Rewarded Points Policy: Non-transferable app bonus points
  const val ADMOB_REWARD_QUIZ_POINTS = 100L

  // InMobi Rewarded Points
  const val INMOBI_REWARD_COINS = 50L

  // Frequency caps (in milliseconds)
  const val MIN_TIME_BETWEEN_REWARDED_ADS_MS = 60_000L // 1 minute
  const val MIN_TIME_BETWEEN_INTERSTITIALS_MS = 120_000L // 2 minutes
  const val MIN_TIME_BETWEEN_APP_OPEN_MS = 300_000L // 5 minutes
}

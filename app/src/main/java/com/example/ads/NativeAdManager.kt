package com.example.ads

import android.content.Context
import android.util.Log

data class SponsoredAdItem(
  val id: String,
  val headline: String,
  val body: String,
  val callToAction: String,
  val sponsorName: String,
  val category: String = "Sponsored"
)

/**
 * NativeAdManager manages sponsored native feed ads inserted approximately every 8-12 posts.
 * Must be visually distinct and clearly labeled as "Sponsored" or "Ad" per Google Play policy.
 */
class NativeAdManager {

  private val sampleSponsoredAds = listOf(
    SponsoredAdItem(
      id = "native_ad_1",
      headline = "Google Cloud Certification Practice",
      body = "Prepare for Professional Cloud Architect & Associate Cloud Engineer exams with practice tests.",
      callToAction = "Learn More",
      sponsorName = "Cloud Academy"
    ),
    SponsoredAdItem(
      id = "native_ad_2",
      headline = "Master Modern Android with Jetpack Compose",
      body = "Hands-on tutorials covering state management, animations, and clean architecture.",
      callToAction = "Explore Courses",
      sponsorName = "DevSkills Global"
    ),
    SponsoredAdItem(
      id = "native_ad_3",
      headline = "Supercharge Your Tech Career",
      body = "Interactive coding quizzes and algorithm challenges tailored for software engineers.",
      callToAction = "Start Practice",
      sponsorName = "CodeSprint"
    )
  )

  fun getNativeAdForPosition(itemIndex: Int): SponsoredAdItem? {
    // The built-in "sponsored" cards were fake placeholders, which looks like a fake ad and is not
    // allowed. No sponsored card is shown until a real AdMob native ad is wired in.
    return null
  }

  fun recordImpression(adId: String) {
    Log.d("NativeAdManager", "Impression recorded for native ad: $adId")
  }

  fun recordClick(adId: String) {
    Log.d("NativeAdManager", "Click recorded for native ad: $adId")
  }
}

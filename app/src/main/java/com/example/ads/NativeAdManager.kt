package com.example.ads

/** Native feed ads are deliberately disabled until a real NativeAdView renderer is connected. */
data class SponsoredAdItem(
  val id: String,
  val headline: String,
  val body: String,
  val callToAction: String,
  val sponsorName: String,
  val category: String = "Sponsored"
)

class NativeAdManager {
  fun getNativeAdForPosition(itemIndex: Int): SponsoredAdItem? = null
  fun recordImpression(adId: String) = Unit
  fun recordClick(adId: String) = Unit
}

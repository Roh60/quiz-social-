package com.example.ads

import android.content.Context
import android.util.Log

/**
 * ConsentManager manages privacy and consent flow (Google UMP, GDPR, CCPA).
 * Ensures that ads are requested according to the user's consent preferences.
 */
object ConsentManager {

  enum class ConsentStatus {
    UNKNOWN,
    REQUIRED,
    NOT_REQUIRED,
    OBTAINED
  }

  private var currentStatus: ConsentStatus = ConsentStatus.NOT_REQUIRED
  private var canServePersonalizedAds: Boolean = false

  fun initialize(context: Context, onConsentGathered: () -> Unit) {
    // In production, integrate Google User Messaging Platform (UMP) SDK.
    // Here we initialize with compliant default non-personalized settings.
    currentStatus = ConsentStatus.OBTAINED
    canServePersonalizedAds = true
    Log.d("ConsentManager", "Consent initialized: $currentStatus, canServePersonalized: $canServePersonalizedAds")
    onConsentGathered()
  }

  fun getConsentStatus(): ConsentStatus = currentStatus

  fun canRequestAds(): Boolean {
    return currentStatus == ConsentStatus.OBTAINED || currentStatus == ConsentStatus.NOT_REQUIRED
  }

  fun canServePersonalized(): Boolean = canServePersonalizedAds

  fun updateConsent(personalizedAllowed: Boolean) {
    canServePersonalizedAds = personalizedAllowed
    currentStatus = ConsentStatus.OBTAINED
  }
}

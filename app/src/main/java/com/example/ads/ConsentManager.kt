package com.example.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Consent is refreshed on each app launch; never assume consent was obtained. */
object ConsentManager {
  @Volatile private var consentInformation: ConsentInformation? = null
  @Volatile private var adsAllowed = false

  fun initialize(activity: Activity, onComplete: (Boolean) -> Unit) {
    val info = UserMessagingPlatform.getConsentInformation(activity)
    consentInformation = info
    val params = ConsentRequestParameters.Builder().build()
    info.requestConsentInfoUpdate(
      activity,
      params,
      {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
          if (formError != null) Log.w("ConsentManager", "Consent form: ${formError.message}")
          adsAllowed = info.canRequestAds()
          onComplete(adsAllowed)
        }
      },
      { error ->
        Log.w("ConsentManager", "Consent info update failed: ${error.message}")
        // UMP can still allow ads based on a valid prior session choice.
        adsAllowed = info.canRequestAds()
        onComplete(adsAllowed)
      }
    )
  }

  fun canRequestAds(): Boolean = adsAllowed && (consentInformation?.canRequestAds() == true)

  fun isPrivacyOptionsRequired(): Boolean = consentInformation?.privacyOptionsRequirementStatus ==
    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

  fun showPrivacyOptions(activity: Activity, onComplete: () -> Unit = {}) {
    if (!isPrivacyOptionsRequired()) { onComplete(); return }
    UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
      if (error != null) Log.w("ConsentManager", "Privacy options: ${error.message}")
      adsAllowed = consentInformation?.canRequestAds() == true
      onComplete()
    }
  }
}

package com.example.ads

import android.content.Context
import android.util.Log

/**
 * MediationManager manages ad networks integrated into the Google AdMob Mediation pipeline.
 * Keeps track of adapter registration and partner network availability status.
 */
class MediationManager {

  private val partnerStatuses = mutableMapOf<AdConfiguration.MediationPartner, Boolean>()

  fun initialize(context: Context) {
    for (partner in AdConfiguration.MediationPartner.entries) {
      val isAvailable = checkAdapterAvailability(partner.adapterName)
      partnerStatuses[partner] = isAvailable
      Log.d("MediationManager", "Partner: ${partner.networkName}, Adapter: ${partner.adapterName}, Active: $isAvailable")
    }
  }

  private fun checkAdapterAvailability(adapterClassName: String): Boolean {
    return try {
      Class.forName(adapterClassName)
      true
    } catch (_: ClassNotFoundException) {
      // Adapter not compiled into current build; will be loaded dynamically when adapter dependency is added
      false
    }
  }

  fun getPartnerStatus(partner: AdConfiguration.MediationPartner): Boolean {
    return partnerStatuses[partner] ?: false
  }

  fun getActivePartners(): List<AdConfiguration.MediationPartner> {
    return partnerStatuses.filter { it.value }.keys.toList()
  }

  fun getAllPartners(): List<Pair<AdConfiguration.MediationPartner, Boolean>> {
    return AdConfiguration.MediationPartner.entries.map { it to (partnerStatuses[it] ?: false) }
  }
}

package com.example.ads

import android.util.Log

/** Reports whether the two selected mediation adapters are present in this APK. */
class MediationManager {
  private val partnerStatuses = mutableMapOf<AdConfiguration.MediationPartner, Boolean>()

  fun initialize() {
    AdConfiguration.MediationPartner.entries.forEach { partner ->
      val available = try { Class.forName(partner.adapterName); true }
      catch (_: ClassNotFoundException) { false }
      partnerStatuses[partner] = available
      Log.i("MediationManager", "${partner.networkName} adapter present=$available")
    }
  }

  fun getPartnerStatus(partner: AdConfiguration.MediationPartner): Boolean = partnerStatuses[partner] ?: false
  fun getActivePartners(): List<AdConfiguration.MediationPartner> = partnerStatuses.filterValues { it }.keys.toList()
  fun getAllPartners(): List<Pair<AdConfiguration.MediationPartner, Boolean>> =
    AdConfiguration.MediationPartner.entries.map { it to (partnerStatuses[it] ?: false) }
}

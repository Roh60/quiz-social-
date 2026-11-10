package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

/**
 * Represents a user withdrawal request via UPI ID.
 */
data class WithdrawalRequest(
  @DocumentId
  val requestId: String = "",
  val userId: String = "",
  val userEmail: String = "",
  val userName: String = "",
  val upiId: String = "",
  val requestedCoins: Long = 0L,
  val amountInPaise: Long = 0L,
  val amountInRupees: Double = 0.0,
  val status: String = "PENDING", // PENDING, UNDER_REVIEW, APPROVED, REJECTED, PROCESSING, PAID, FAILED, REFUNDED
  val adminNotes: String = "",
  val paymentReferenceId: String = "", // Bank UTR or Transaction reference
  val rejectionReason: String = "",
  @ServerTimestamp
  val requestedAt: Timestamp? = null,
  val reviewedAt: Timestamp? = null,
  val reviewedBy: String = "",
  val paidAt: Timestamp? = null
) {
  val formattedRupees: String
    get() = String.format(Locale.getDefault(), "₹%.2f", amountInRupees)

  val isActionableByAdmin: Boolean
    get() = status == "PENDING" || status == "UNDER_REVIEW" || status == "APPROVED"

  val maskedUpiId: String
    get() {
      val trimmed = upiId.trim()
      if (!trimmed.contains("@")) return "***"
      val parts = trimmed.split("@")
      val handle = parts[0]
      val bank = parts[1]
      return if (handle.length > 3) {
        "${handle.take(2)}***${handle.takeLast(1)}@$bank"
      } else {
        "***@$bank"
      }
    }
}

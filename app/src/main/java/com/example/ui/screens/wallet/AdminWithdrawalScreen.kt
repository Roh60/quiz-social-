package com.example.ui.screens.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ads.UnifiedAdsManager
import com.example.data.model.EarningConfig
import com.example.data.model.WithdrawalRequest
import com.example.data.remote.WalletRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWithdrawalScreen(
  walletRepository: WalletRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val isAdmin = remember(currentUserId) { walletRepository.isCurrentUserAdmin() }
  val config by walletRepository.observeEarningConfig().collectAsState(initial = EarningConfig())

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Requests, 1: Controls
  var statusFilter by remember { mutableStateOf("PENDING") } // ALL, PENDING, APPROVED, PAID, REJECTED
  val requests by walletRepository.observeAllWithdrawalsForAdmin(statusFilter).collectAsState(initial = emptyList())

  var selectedRequestForAction by remember { mutableStateOf<WithdrawalRequest?>(null) }
  var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE", "PAY", "REJECT"
  var paymentRefInput by remember { mutableStateOf("") }
  var rejectionReasonInput by remember { mutableStateOf("") }
  var isProcessing by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Monetization & Payouts", fontWeight = FontWeight.Bold)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("admin_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    if (!isAdmin) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text("Access Restricted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("Only designated platform administrators may access this portal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
      }
      return@Scaffold
    }

    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Payout Requests") },
          modifier = Modifier.testTag("admin_payout_tab")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Ad & Rate Controls") },
          modifier = Modifier.testTag("admin_controls_tab")
        )
      }

      if (feedbackMessage != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(feedbackMessage ?: "", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(10.dp))
        }
      }

      if (selectedTab == 0) {
        // Payout Requests Sub-View
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            Spacer(modifier = Modifier.height(4.dp))
            // Status Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("PENDING", "APPROVED", "PAID", "REJECTED", "ALL").forEach { status ->
                FilterChip(
                  selected = statusFilter == status,
                  onClick = { statusFilter = status },
                  label = { Text(status) }
                )
              }
            }
          }

          if (requests.isEmpty()) {
            item {
              Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("No withdrawal requests in $statusFilter status.", color = MaterialTheme.colorScheme.outline)
              }
            }
          } else {
            items(requests, key = { it.requestId }) { req ->
              AdminWithdrawalItem(
                request = req,
                onApprove = {
                  selectedRequestForAction = req
                  actionType = "APPROVE"
                },
                onMarkPaid = {
                  selectedRequestForAction = req
                  actionType = "PAY"
                  paymentRefInput = ""
                },
                onReject = {
                  selectedRequestForAction = req
                  actionType = "REJECT"
                  rejectionReasonInput = ""
                }
              )
            }
          }

          item {
            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      } else {
        // Platform Ad Controls & Configuration Tab
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            Text("Advertising Networks Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          item {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // AdMob Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Google AdMob Ads", fontWeight = FontWeight.Bold)
                    Text("Controls Rewarded, Interstitial, and Banner Ad serving", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                  }
                  Switch(
                    checked = config.isAdMobEnabled,
                    onCheckedChange = { checked ->
                      coroutineScope.launch {
                        val updated = config.copy(isAdMobEnabled = checked)
                        walletRepository.updateEarningConfig(updated)
                        UnifiedAdsManager.syncAdminConfig(checked, updated.isInMobiEnabled, updated.isInMobiCashConversionAllowed)
                      }
                    }
                  )
                }

                // InMobi Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("InMobi Ads Network", fontWeight = FontWeight.Bold)
                    Text("Controls InMobi video & interstitial delivery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                  }
                  Switch(
                    checked = config.isInMobiEnabled,
                    onCheckedChange = { checked ->
                      coroutineScope.launch {
                        val updated = config.copy(isInMobiEnabled = checked)
                        walletRepository.updateEarningConfig(updated)
                        UnifiedAdsManager.syncAdminConfig(updated.isAdMobEnabled, checked, updated.isInMobiCashConversionAllowed)
                      }
                    }
                  )
                }

                // InMobi Cash Conversion (Policy switch)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("InMobi Cash Conversion Allowed", fontWeight = FontWeight.Bold)
                    Text("Keep DISABLED unless written publisher agreement permits real cash conversion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                  }
                  Switch(
                    checked = config.isInMobiCashConversionAllowed,
                    onCheckedChange = { checked ->
                      coroutineScope.launch {
                        val updated = config.copy(isInMobiCashConversionAllowed = checked)
                        walletRepository.updateEarningConfig(updated)
                        UnifiedAdsManager.syncAdminConfig(updated.isAdMobEnabled, updated.isInMobiEnabled, checked)
                      }
                    }
                  )
                }

                // Pause All Withdrawals
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Pause All Withdrawal Requests", fontWeight = FontWeight.Bold)
                    Text("Temporarily halt new withdrawal submissions during maintenance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                  }
                  Switch(
                    checked = config.isWithdrawalPaused,
                    onCheckedChange = { checked ->
                      coroutineScope.launch {
                        val updated = config.copy(isWithdrawalPaused = checked)
                        walletRepository.updateEarningConfig(updated)
                      }
                    }
                  )
                }
              }
            }
          }

          item {
            Text("Financial Ledger Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          item {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Conversion: 1000 Coins = ₹5.00 INR (200 Coins/₹1)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("• Minimum Withdrawal Threshold: ${config.minWithdrawalCoins} Coins (₹${(config.calculateInrRupees(config.minWithdrawalCoins)).toInt()})", style = MaterialTheme.typography.bodySmall)
                Text("• Payout Medium: Direct UPI via Admin UTR reference", style = MaterialTheme.typography.bodySmall)
                Text("• AdMob Compliance: Strictly non-cash bonus points only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      }
    }

    // Action Confirmation Dialogs
    if (selectedRequestForAction != null && actionType != null) {
      val req = selectedRequestForAction!!
      AlertDialog(
        onDismissRequest = {
          selectedRequestForAction = null
          actionType = null
        },
        title = {
          Text(
            text = when (actionType) {
              "APPROVE" -> "Approve Withdrawal"
              "PAY" -> "Confirm Bank UPI Payout"
              "REJECT" -> "Reject & Refund Coins"
              else -> "Review Request"
            }
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("User: ${req.userName} (${req.userEmail})")
            Text("Coins: ${req.requestedCoins} • Amount: ${req.formattedRupees}", fontWeight = FontWeight.Bold)
            Text("UPI Address: ${req.upiId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (actionType == "PAY") {
              OutlinedTextField(
                value = paymentRefInput,
                onValueChange = { paymentRefInput = it },
                label = { Text("Bank UTR / Transaction Reference *") },
                placeholder = { Text("e.g. 431289104821") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_utr_input")
              )
            }

            if (actionType == "REJECT") {
              OutlinedTextField(
                value = rejectionReasonInput,
                onValueChange = { rejectionReasonInput = it },
                label = { Text("Rejection Reason *") },
                placeholder = { Text("e.g. Invalid UPI ID / Name mismatch") },
                modifier = Modifier.fillMaxWidth().testTag("admin_rejection_input")
              )
              Text("Note: Rejecting will automatically refund ${req.requestedCoins} coins back to user's wallet.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              isProcessing = true
              coroutineScope.launch {
                val targetStatus = when (actionType) {
                  "APPROVE" -> "APPROVED"
                  "PAY" -> "PAID"
                  "REJECT" -> "REJECTED"
                  else -> "APPROVED"
                }

                val result = walletRepository.processWithdrawalReview(
                  requestId = req.requestId,
                  targetStatus = targetStatus,
                  paymentReference = paymentRefInput,
                  rejectionReason = rejectionReasonInput
                )
                isProcessing = false
                selectedRequestForAction = null
                actionType = null
                feedbackMessage = if (result.isSuccess) "Request successfully updated to $targetStatus" else "Failed: ${result.exceptionOrNull()?.message}"
              }
            },
            enabled = !isProcessing && (actionType != "PAY" || paymentRefInput.isNotBlank()) && (actionType != "REJECT" || rejectionReasonInput.isNotBlank())
          ) {
            Text("Confirm")
          }
        },
        dismissButton = {
          TextButton(onClick = { selectedRequestForAction = null; actionType = null }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
fun AdminWithdrawalItem(
  request: WithdrawalRequest,
  onApprove: () -> Unit,
  onMarkPaid: () -> Unit,
  onReject: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("admin_req_${request.requestId}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(request.userName.ifBlank { "User" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(request.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when (request.status) {
            "PAID" -> MaterialTheme.colorScheme.primaryContainer
            "APPROVED" -> MaterialTheme.colorScheme.secondaryContainer
            "REJECTED" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
          }
        ) {
          Text(
            text = request.status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Requested: ${request.requestedCoins} Coins", fontWeight = FontWeight.Bold)
        Text(request.formattedRupees, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
      }

      Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
        Text("UPI: ${request.maskedUpiId}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
      }

      if (request.status == "PAID" && request.paymentReferenceId.isNotBlank()) {
        Text("UTR Reference: ${request.paymentReferenceId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }

      if (request.isActionableByAdmin) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (request.status == "PENDING") {
            OutlinedButton(
              onClick = onApprove,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Approve", fontSize = 12.sp)
            }
          }

          Button(
            onClick = onMarkPaid,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1.2f)
          ) {
            Text("Mark Paid (UTR)", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = onReject,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f)
          ) {
            Text("Reject", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

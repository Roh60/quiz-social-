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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ads.AdMobBanner
import com.example.data.model.EarningConfig
import com.example.data.model.UserWallet
import com.example.data.model.WithdrawalRequest
import com.example.data.remote.WalletRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
  walletRepository: WalletRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val wallet by walletRepository.observeUserWallet(currentUserId).collectAsState(initial = UserWallet(userId = currentUserId))
  val withdrawals by walletRepository.observeUserWithdrawals(currentUserId).collectAsState(initial = emptyList())
  val config by walletRepository.observeEarningConfig().collectAsState(initial = EarningConfig())

  var coinInput by remember { mutableStateOf("") }
  var upiInput by remember { mutableStateOf("") }
  var confirmUpiInput by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var showConfirmDialog by remember { mutableStateOf(false) }

  val parsedCoins = coinInput.toLongOrNull() ?: 0L
  val inrAmount = config.calculateInrRupees(parsedCoins)
  val upiMatches = upiInput.isNotBlank() && upiInput.trim() == confirmUpiInput.trim()
  val isEligible = parsedCoins >= config.minWithdrawalCoins && parsedCoins <= wallet.cashConvertibleCoins && upiInput.contains("@") && upiMatches

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Withdrawal Request (UPI)", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("withdraw_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      AdMobBanner(modifier = Modifier.fillMaxWidth())
    }
  ) { padding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
      }

      // Available Balance Highlight
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth().testTag("withdraw_balance_banner")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Available Cash-Convertible", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
              Text("${wallet.cashConvertibleCoins} Coins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
              Text(wallet.getFormattedInr(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
          }
        }
      }

      // Success Banner
      item {
        AnimatedVisibility(visible = successMessage != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("withdraw_success_banner")
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(successMessage ?: "", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // Error Banner
      item {
        AnimatedVisibility(visible = errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().testTag("withdraw_error_banner")
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(8.dp))
              Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      // Withdrawal Request Form Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Request Payment to UPI ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Preset Quick-Select Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(2000L, 5000L, 10000L).forEach { preset ->
                FilterChip(
                  selected = parsedCoins == preset,
                  onClick = { coinInput = preset.toString() },
                  label = { Text("${preset} (₹${(preset * 5) / 1000})") }
                )
              }
            }

            // Coin Amount Input
            OutlinedTextField(
              value = coinInput,
              onValueChange = { coinInput = it.filter { char -> char.isDigit() } },
              label = { Text("Coins to Withdraw (Min ${config.minWithdrawalCoins})") },
              placeholder = { Text("e.g. 2000") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("withdraw_coins_input"),
              shape = RoundedCornerShape(12.dp)
            )

            // Dynamic INR Conversion Preview
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Payout Amount:", style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "₹${String.format("%.2f", inrAmount)} INR",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            // UPI ID Input
            OutlinedTextField(
              value = upiInput,
              onValueChange = { upiInput = it.trim() },
              label = { Text("Your UPI ID / VPA") },
              placeholder = { Text("e.g. yourname@oksbi or mobile@paytm") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("withdraw_upi_input"),
              shape = RoundedCornerShape(12.dp)
            )

            // Confirm UPI ID Input
            OutlinedTextField(
              value = confirmUpiInput,
              onValueChange = { confirmUpiInput = it.trim() },
              label = { Text("Confirm UPI ID / VPA") },
              placeholder = { Text("Re-enter the same UPI ID") },
              singleLine = true,
              isError = confirmUpiInput.isNotBlank() && confirmUpiInput != upiInput,
              supportingText = if (confirmUpiInput.isNotBlank() && confirmUpiInput != upiInput) {
                { Text("UPI IDs do not match", color = MaterialTheme.colorScheme.error) }
              } else null,
              modifier = Modifier.fillMaxWidth().testTag("withdraw_confirm_upi_input"),
              shape = RoundedCornerShape(12.dp)
            )

            // Security & Non-Fake Notice
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
              Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Safe & Secure: Never share UPI PIN or password. Transfers are verified by platform admin and sent with bank UTR proof.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Submit Button
            Button(
              onClick = { showConfirmDialog = true },
              enabled = isEligible && !isSubmitting && !config.isWithdrawalPaused,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("withdraw_submit_btn")
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting...")
              } else {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Withdrawal Request (₹${String.format("%.2f", inrAmount)})", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Section: Withdrawal History
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("My Withdrawal Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("${withdrawals.size} Requests", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
      }

      if (withdrawals.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("No withdrawal requests yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
          }
        }
      } else {
        items(withdrawals, key = { it.requestId }) { req ->
          WithdrawalItemCard(request = req)
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showConfirmDialog = false },
        title = { Text("Confirm Withdrawal Request") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Please verify your details before submitting:")
            Text("• Requested Coins: $parsedCoins", fontWeight = FontWeight.Bold)
            Text("• INR Amount: ₹${String.format("%.2f", inrAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("• Destination UPI ID: $upiInput", fontWeight = FontWeight.Bold)
            Text(
              "These coins will be reserved from your wallet and reviewed by administration. Once verified, payout is dispatched to your UPI.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showConfirmDialog = false
              isSubmitting = true
              errorMessage = null
              successMessage = null

              coroutineScope.launch {
                val result = walletRepository.submitWithdrawalRequest(
                  userId = currentUserId,
                  upiId = upiInput,
                  requestedCoins = parsedCoins
                )
                isSubmitting = false
                if (result.isSuccess) {
                  successMessage = "Request submitted successfully! Ref ID: ${result.getOrNull()?.take(8)}. Administration will review and dispatch payment."
                  coinInput = ""
                  upiInput = ""
                } else {
                  errorMessage = result.exceptionOrNull()?.message ?: "Submission failed. Please try again."
                }
              }
            }
          ) {
            Text("Confirm & Submit")
          }
        },
        dismissButton = {
          TextButton(onClick = { showConfirmDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
fun WithdrawalItemCard(request: WithdrawalRequest) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("withdrawal_card_${request.requestId}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(request.formattedRupees, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
          Text("${request.requestedCoins} Coins • UPI: ${request.maskedUpiId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
            color = when (request.status) {
              "PAID" -> MaterialTheme.colorScheme.onPrimaryContainer
              "APPROVED" -> MaterialTheme.colorScheme.onSecondaryContainer
              "REJECTED" -> MaterialTheme.colorScheme.onErrorContainer
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      if (request.status == "PAID" && request.paymentReferenceId.isNotBlank()) {
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
          Text(
            text = "Bank Reference (UTR): ${request.paymentReferenceId}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      if (request.status == "REJECTED" && request.rejectionReason.isNotBlank()) {
        Text(
          text = "Rejection Reason: ${request.rejectionReason} (Coins refunded to wallet)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.error
        )
      }
    }
  }
}

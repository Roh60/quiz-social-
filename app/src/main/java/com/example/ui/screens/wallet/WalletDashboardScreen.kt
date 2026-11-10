package com.example.ui.screens.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ads.AdMobBanner
import com.example.data.model.CoinTransaction
import com.example.data.model.UserWallet
import com.example.data.remote.WalletRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDashboardScreen(
  walletRepository: WalletRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  onNavigateToEarn: () -> Unit,
  onNavigateToWithdraw: () -> Unit,
  onNavigateToAdmin: () -> Unit,
  modifier: Modifier = Modifier
) {
  val wallet by walletRepository.observeUserWallet(currentUserId).collectAsState(initial = UserWallet(userId = currentUserId))
  val transactions by walletRepository.observeUserTransactions(currentUserId).collectAsState(initial = emptyList())
  val config by walletRepository.observeEarningConfig().collectAsState(initial = com.example.data.model.EarningConfig())
  val isAdmin = remember(currentUserId) { walletRepository.isCurrentUserAdmin() }

  var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Credits, 2: Withdrawals

  val filteredTransactions = remember(transactions, selectedFilter) {
    when (selectedFilter) {
      1 -> transactions.filter { it.type == "CREDIT" || it.type == "REWARD" }
      2 -> transactions.filter { it.type.startsWith("WITHDRAWAL") }
      else -> transactions
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AccountBalanceWallet,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Coin Wallet & Earnings", fontWeight = FontWeight.Bold)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("wallet_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (isAdmin) {
            IconButton(onClick = onNavigateToAdmin, modifier = Modifier.testTag("wallet_admin_btn")) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Portal", tint = MaterialTheme.colorScheme.primary)
            }
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

      // 1. Primary Wallet Balance Card (Gradient)
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth().testTag("wallet_main_balance_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                    MaterialTheme.colorScheme.primary
                  )
                )
              )
              .padding(20.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "CASH-CONVERTIBLE BALANCE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                letterSpacing = 1.sp
              )
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
              ) {
                Text(
                  text = "1000 Coins = ₹5.00",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "${wallet.cashConvertibleCoins}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Coins",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 6.dp)
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Estimated Cash Value: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
              )
              Text(
                text = wallet.getFormattedInr(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Action Buttons: Withdraw & Earn More
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = onNavigateToWithdraw,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f).height(46.dp).testTag("wallet_withdraw_btn")
              ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Withdraw UPI", fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = onNavigateToEarn,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.weight(1f).height(46.dp).testTag("wallet_earn_btn")
              ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Earn Coins", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 2. Separate Balances & Stats Grid
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // App-Only Bonus Points (Strict Policy Segregation)
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("App Points", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "${wallet.appOnlyRewardPoints}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "AdMob bonus (in-app only)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }

          // Lifetime Earned Coins
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lifetime Coins", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "${wallet.totalEarnedLifetimeCoins}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Today: +${wallet.todayEarnedCoins}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // Detailed Ledger Summary Cards
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().testTag("wallet_breakdown_card")
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ledger Accounting Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Column {
                Text("Available Coins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("${wallet.availableCoins}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
              Column {
                Text("Pending in Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("${wallet.pendingCoins} (${wallet.getFormattedPendingInr()})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Column {
                Text("Redeemed / Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("${wallet.redeemedCoins} Coins (₹${String.format("%.2f", wallet.totalWithdrawnInr)})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
              }
              Column {
                Text("Min Withdrawal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("${config.minWithdrawalCoins} Coins (₹${(config.minWithdrawalCoins * 5) / 1000})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 3. Pending & Approved Withdrawal Alerts (if any)
      if (wallet.pendingWithdrawalCoins > 0L || wallet.paidWithdrawalPaisa > 0L) {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text("Pending Withdrawal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text("${wallet.pendingWithdrawalCoins} coins (${wallet.getFormattedPendingInr()}) under review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
              }
              Text("In Review", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      // 4. Policy Transparency Card
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Monetization & Conversion Policy",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "• Cash-convertible coins can only be earned from verified knowledge quizzes & community participation.\n• Google AdMob rewards provide in-app bonus points only and cannot be exchanged for cash or UPI.\n• Minimum withdrawal threshold: ${config.minWithdrawalCoins} coins (₹${(config.calculateInrRupees(config.minWithdrawalCoins)).toInt()}).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      // 5. Transaction History Section Header & Filter Tabs
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Transaction Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
              text = "${filteredTransactions.size} Records",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = selectedFilter == 0,
              onClick = { selectedFilter = 0 },
              label = { Text("All") },
              colors = FilterChipDefaults.filterChipColors()
            )
            FilterChip(
              selected = selectedFilter == 1,
              onClick = { selectedFilter = 1 },
              label = { Text("Earned") },
              colors = FilterChipDefaults.filterChipColors()
            )
            FilterChip(
              selected = selectedFilter == 2,
              onClick = { selectedFilter = 2 },
              label = { Text("Withdrawals") },
              colors = FilterChipDefaults.filterChipColors()
            )
          }
        }
      }

      // 6. Transactions List
      if (filteredTransactions.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No transactions yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
              Text("Earn coins by solving quizzes or participating in polls!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
          }
        }
      } else {
        items(filteredTransactions, key = { it.transactionId }) { tx ->
          TransactionItemRow(tx = tx)
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun TransactionItemRow(tx: CoinTransaction) {
  val isCredit = tx.type == "CREDIT" || tx.type == "REWARD"
  val isPayout = tx.type == "WITHDRAWAL_PAYOUT"
  val isRefund = tx.type == "WITHDRAWAL_REFUND"

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("tx_item_${tx.transactionId}")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = when {
          isRefund -> MaterialTheme.colorScheme.errorContainer
          isPayout -> MaterialTheme.colorScheme.primaryContainer
          isCredit -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
          else -> MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = when {
              isCredit -> Icons.Default.ArrowUpward
              isRefund -> Icons.Default.ArrowUpward
              else -> Icons.Default.ArrowDownward
            },
            contentDescription = null,
            tint = when {
              isRefund -> MaterialTheme.colorScheme.error
              isCredit -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.description.ifBlank { tx.source },
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (tx.isCashConvertible) "Cash-Convertible" else "App-Only Point",
            style = MaterialTheme.typography.labelSmall,
            color = if (tx.isCashConvertible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = " • ${tx.status}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${if (isCredit || isRefund) "+" else "-"}${tx.coinAmount} Coins",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = if (isCredit || isRefund) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (tx.paisaEquivalent > 0L) {
          Text(
            text = tx.formattedInr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    }
  }
}

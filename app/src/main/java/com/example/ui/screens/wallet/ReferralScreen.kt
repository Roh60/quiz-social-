package com.example.ui.screens.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ads.AdMobBanner
import com.example.data.model.UserProfile
import com.example.data.remote.QuizPollRepository
import com.example.data.remote.WalletRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
  walletRepository: WalletRepository,
  quizPollRepository: QuizPollRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by quizPollRepository.observeUserProfile(currentUserId).collectAsState(initial = null)

  var referralInput by remember { mutableStateOf("") }
  var isClaiming by remember { mutableStateOf(false) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val myReferralCode = userProfile?.referralCode?.ifBlank {
    currentUserId.take(6).uppercase()
  } ?: currentUserId.take(6).uppercase()

  val shareLink = "https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app?ref=$myReferralCode"

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Referral Program (+500 Coins)", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("referral_back_btn")) {
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
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Success Alert
      AnimatedVisibility(visible = successMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth().testTag("referral_success_banner")
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = successMessage ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Error Alert
      AnimatedVisibility(visible = errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth().testTag("referral_error_banner")
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
      }

      // Hero Card: Invite & Earn 500 Coins
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().testTag("referral_hero_card")
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Invite Friends, Earn ₹2.50", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
              Text("500 Coins = ₹2.50 INR per verified friend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
          }

          Text(
            text = "Share your unique referral code. When a new friend signs up and enters your code, you receive 500 cash-convertible coins and your friend receives 50 welcome coins!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

      // Referral Code Share Box
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("referral_share_box")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Your Personal Referral Code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = myReferralCode,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("referral_code_display")
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Copy Button
                IconButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("QuizPoll Referral Code", myReferralCode))
                    Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(36.dp).testTag("referral_copy_btn")
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.primary)
                }

                // Share Button
                IconButton(
                  onClick = {
                    val sendIntent = Intent().apply {
                      action = Intent.ACTION_SEND
                      putExtra(
                        Intent.EXTRA_TEXT,
                        "Join me on QuizPoll Community! Play interactive quizzes, vote in polls, and earn real cash. Use my referral code $myReferralCode for a 50 Coins welcome bonus! Link: $shareLink"
                      )
                      type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Referral Code"))
                  },
                  modifier = Modifier.size(36.dp).testTag("referral_share_btn")
                ) {
                  Icon(Icons.Default.Share, contentDescription = "Share Code", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }

          // Referral Statistics
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${userProfile?.referralCount ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              Text("Friends Invited", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              val coinsEarned = userProfile?.referralPointsEarned ?: 0L
              Text("+${coinsEarned}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
              Text("Coins Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              val inr = ((userProfile?.referralPointsEarned ?: 0L) * 5.0) / 1000.0
              Text("₹${String.format("%.2f", inr)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              Text("INR Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
          }
        }
      }

      // Claim Friend's Referral Code Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("referral_claim_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Have a Friend's Referral Code?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text("Enter their referral code below to claim +50 Welcome Coins in your cash-convertible wallet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          OutlinedTextField(
            value = referralInput,
            onValueChange = { referralInput = it.uppercase().trim() },
            label = { Text("Friend's Referral Code") },
            placeholder = { Text("e.g. SCHOLAR") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("referral_input_code"),
            shape = RoundedCornerShape(12.dp)
          )

          Button(
            onClick = {
              if (referralInput.isBlank()) {
                errorMessage = "Please enter a valid referral code."
                return@Button
              }

              isClaiming = true
              errorMessage = null
              successMessage = null

              coroutineScope.launch {
                val userName = userProfile?.displayName?.ifBlank { "Quiz Scholar" } ?: "Quiz Scholar"
                val result = walletRepository.claimReferralReward(
                  referrerCode = referralInput,
                  refereeUserId = currentUserId,
                  refereeName = userName
                )
                isClaiming = false
                result.fold(
                  onSuccess = {
                    successMessage = "Success! +50 Welcome Coins added to your wallet. Your friend also earned +500 Coins!"
                    referralInput = ""
                  },
                  onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "Failed to claim referral code."
                  }
                )
              }
            },
            enabled = !isClaiming && referralInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("referral_submit_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isClaiming) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Verifying Code...")
            } else {
              Text("Claim 50 Welcome Coins", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Anti-Fraud & Rules Notice
      Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Referral Rules & Anti-Abuse Policy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
          Text("• Self-referrals, clone accounts, and duplicate device attempts are automatically blocked by the ledger.\n• 1000 Coins = ₹5 INR conversion applied to all cash-convertible referral earnings.\n• One welcome claim permitted per registered member.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

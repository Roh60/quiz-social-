package com.example.ui.screens.wallet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ads.AdConstants
import com.example.data.ads.AdMobBanner
import com.example.data.ads.UnifiedAdsManager
import com.example.data.model.EarningConfig
import com.example.data.remote.WalletRepository
import kotlinx.coroutines.launch
import java.util.UUID

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnRewardsScreen(
  walletRepository: WalletRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  onNavigateToQuizzes: () -> Unit,
  onNavigateToPolls: () -> Unit,
  onNavigateToReferral: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val config by walletRepository.observeEarningConfig().collectAsState(initial = EarningConfig())

  val todayCheckin by walletRepository.observeTodayCheckin(currentUserId).collectAsState(initial = null)
  val todayChallenge by walletRepository.observeTodayDailyChallenge(currentUserId).collectAsState(initial = null)

  var isLoadingAd by remember { mutableStateOf(false) }
  var isCheckingIn by remember { mutableStateOf(false) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Earn Rewards & Coins", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("earn_back_btn")) {
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
          modifier = Modifier.fillMaxWidth().testTag("earn_success_banner")
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
          modifier = Modifier.fillMaxWidth().testTag("earn_error_banner")
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

      // ---------------- 1. DAILY CHECK-IN CARD (20 COINS) ----------------
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("daily_checkin_card")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Daily Check-in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("+20 Cash-Convertible Coins", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
            }

            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
              Text(
                text = "Streak: ${todayCheckin?.streakDays ?: 1} Days",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Text(
            text = "Check in once per calendar day to claim 20 coins and build your consecutive daily streak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Button(
            onClick = {
              if (todayCheckin != null) return@Button
              isCheckingIn = true
              errorMessage = null
              successMessage = null
              coroutineScope.launch {
                val result = walletRepository.claimDailyCheckin(currentUserId)
                isCheckingIn = false
                result.fold(
                  onSuccess = { record ->
                    successMessage = "Daily Check-in Complete! +${record.rewardCoins} Coins added to your wallet (Streak: ${record.streakDays} days)!"
                  },
                  onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "Failed to claim daily check-in."
                  }
                )
              }
            },
            enabled = !isCheckingIn && todayCheckin == null,
            modifier = Modifier.fillMaxWidth().testTag("claim_checkin_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isCheckingIn) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Claiming...")
            } else if (todayCheckin != null) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Claimed for Today (+20 Coins)")
            } else {
              Text("Claim 20 Coins Today", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // ---------------- 2. DAILY CHALLENGE CARD (5 COINS/QUIZ, MAX 25) ----------------
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("daily_challenge_card")
      ) {
        val completedCount = todayChallenge?.completedCount ?: 0
        val coinsEarned = todayChallenge?.totalCoinsEarned ?: 0L

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Daily Quiz Challenge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("+5 Coins per Quiz (Max 25/day)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
              }
            }

            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
              Text(
                text = "$completedCount / 5 Done",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          LinearProgressIndicator(
            progress = { (completedCount / 5f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.tertiary
          )

          Text(
            text = "Complete 5 daily quizzes to earn up to 25 cash-convertible coins today. Earned: +$coinsEarned coins.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedButton(
            onClick = onNavigateToQuizzes,
            modifier = Modifier.fillMaxWidth().testTag("play_challenge_quiz_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(if (completedCount >= 5) "Challenge Completed! Browse Quizzes" else "Solve Challenge Quiz", fontWeight = FontWeight.Bold)
          }
        }
      }

      // ---------------- 3. REFERRAL INVITATION BANNER (500 COINS) ----------------
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().testTag("earn_referral_banner")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Invite Friends & Earn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
              Text("+500 Coins", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
          }

          Text(
            text = "Invite friends to join QuizPoll Community. Earn 500 cash-convertible coins (₹2.50) for every verified new user!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )

          Button(
            onClick = onNavigateToReferral,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().testTag("open_referral_program_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Open Referral Dashboard", fontWeight = FontWeight.Bold)
          }
        }
      }

      // ---------------- 4. COMMUNITY ACTIVITIES (QUIZZES, POLLS, POSTS) ----------------
      Text(
        text = "Community Activities",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      // Correct Answer Bonus
      EarningOpportunityCard(
        title = "Correct Answer Bonus",
        rewardText = "+2 Coins per Correct Question",
        description = "Answer questions accurately across community quizzes. Each verified correct answer yields 2 coins.",
        icon = Icons.Default.CheckCircle,
        actionLabel = "Play Quizzes",
        onAction = onNavigateToQuizzes,
        tag = "earn_answers_card"
      )

      // Community Post Reward
      EarningOpportunityCard(
        title = "Publish Community Posts",
        rewardText = "+5 Coins per Original Post",
        description = "Share thoughtful quiz questions, explanations, or educational discussion topics with fellow scholars.",
        icon = Icons.Default.PostAdd,
        actionLabel = "Create Content",
        onAction = onNavigateBack, // Navigates back to home/create
        tag = "earn_post_card"
      )

      // Poll Participation Card
      EarningOpportunityCard(
        title = "Vote in Community Polls",
        rewardText = "+${config.pollVoteCoins} Coins per Vote",
        description = "Cast your vote in public questions and join insightful discussions to receive participation coins.",
        icon = Icons.Default.Poll,
        actionLabel = "Explore Polls",
        onAction = onNavigateToPolls,
        tag = "earn_poll_card"
      )

      Spacer(modifier = Modifier.height(4.dp))

      // ---------------- 5. SPONSORED REWARDED VIDEO ADS (30 COINS) ----------------
      Text(
        text = "Sponsored Video Rewards (30 Coins)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      // AdMob Rewarded Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("earn_admob_card")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Google AdMob Rewarded Ad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("+${AdConstants.REWARDED_AD_COINS} Bonus Points", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
              }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
              Text("In-App Only", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }

          Text(
            text = "Voluntarily watch a complete sponsored video to receive 30 non-transferable bonus points. Closing before completion does not grant rewards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Button(
            onClick = {
              val activity = context.findActivity()
              if (activity == null) {
                errorMessage = "Unable to show ad: Activity context unavailable."
                return@Button
              }

              isLoadingAd = true
              errorMessage = null
              successMessage = null

              UnifiedAdsManager.showAdMobRewarded(
                activity = activity,
                onSuccess = { _, idempotencyKey ->
                  isLoadingAd = false
                  coroutineScope.launch {
                    walletRepository.claimRewardedAdCoins(currentUserId, "ADMOB", idempotencyKey)
                    successMessage = "Reward Claimed! +${AdConstants.REWARDED_AD_COINS} points credited to your ledger."
                  }
                },
                onFailure = { reason ->
                  isLoadingAd = false
                  errorMessage = reason
                }
              )
            },
            enabled = !isLoadingAd && config.isAdMobEnabled,
            modifier = Modifier.fillMaxWidth().testTag("watch_admob_reward_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isLoadingAd) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Loading Sponsored Video...")
            } else {
              Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Watch Ad (+30 Points)", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // InMobi Rewarded Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("earn_inmobi_card")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("InMobi Video Challenge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                  text = if (config.isInMobiCashConversionAllowed) "+30 Cash-Convertible Coins" else "+30 Bonus Points (In-App Only)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.tertiary
                )
              }
            }
          }

          Text(
            text = "Watch an InMobi network video to earn rewards. Cash conversion is subject to publisher agreements and admin approval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedButton(
            onClick = {
              val activity = context.findActivity()
              if (activity == null) {
                errorMessage = "Unable to show ad: Activity context unavailable."
                return@OutlinedButton
              }

              isLoadingAd = true
              errorMessage = null
              successMessage = null

              UnifiedAdsManager.showInMobiRewarded(
                activity = activity,
                onSuccess = { coins, _, idempotencyKey ->
                  isLoadingAd = false
                  coroutineScope.launch {
                    walletRepository.claimRewardedAdCoins(currentUserId, "INMOBI", idempotencyKey)
                    successMessage = "InMobi Reward Claimed! +$coins coins credited to your ledger."
                  }
                },
                onFailure = { reason ->
                  isLoadingAd = false
                  errorMessage = reason
                }
              )
            },
            enabled = !isLoadingAd && config.isInMobiEnabled,
            modifier = Modifier.fillMaxWidth().testTag("watch_inmobi_reward_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Watch InMobi Video (+30 Coins)", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun EarningOpportunityCard(
  title: String,
  rewardText: String,
  description: String,
  icon: ImageVector,
  actionLabel: String,
  onAction: () -> Unit,
  tag: String
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag(tag)
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(rewardText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
      }

      Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

      OutlinedButton(
        onClick = onAction,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(actionLabel, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

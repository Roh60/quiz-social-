package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.UserProfile
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch

@Composable
fun ReferralCard(
  profile: UserProfile,
  repository: QuizPollRepository,
  modifier: Modifier = Modifier,
  onProfileUpdated: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var inputCode by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }

  val myCode = profile.referralCode.ifBlank {
    repository.generateReferralCode(profile.username, profile.userId)
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("referral_card")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      // Header: Gift Icon & Title
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(44.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.CardGiftcard,
              contentDescription = "Referral & Rewards",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Invite Friends & Earn Rewards",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Get +100 pts per friend, friend gets +50 pts!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // User's Unique Referral Code Box
      Text(
        text = "Your Unique Referral Code",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = myCode,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("user_referral_code_text")
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("QuizPoll Referral Code", myCode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(36.dp).testTag("copy_referral_code_btn")
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
              onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "Join me on QuizPoll Community!")
                  putExtra(
                    Intent.EXTRA_TEXT,
                    "Join QuizPoll Community with my referral code $myCode. Search for QuizPoll Community on Google Play."
                  )
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
              },
              modifier = Modifier.size(36.dp).testTag("share_referral_code_btn")
            ) {
              Icon(Icons.Default.Share, contentDescription = "Share Code", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Stats Pills (Friends Invited, Points Earned)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Friends Invited", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${profile.referralCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Referral Points", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+${profile.referralPointsEarned} pts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Enter Friend's Referral Code Section
      if (profile.referredBy.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF10B981).copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth().testTag("referral_claimed_badge")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Referral Bonus Claimed (${profile.referredBy}) • +50 Points Awarded",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF047857)
            )
          }
        }
      } else {
        Text(
          text = "Have a Friend's Referral Code?",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = inputCode,
            onValueChange = { inputCode = it.uppercase() },
            placeholder = { Text("e.g. QP-ALEX1234", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("referral_input_field"),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = {
              if (inputCode.isNotBlank()) {
                isSubmitting = true
                feedbackMessage = null
                scope.launch {
                  val result = repository.processReferralCode(inputCode.trim())
                  isSubmitting = false
                  result.fold(
                    onSuccess = { msg ->
                      feedbackMessage = msg
                      isError = false
                      inputCode = ""
                      onProfileUpdated?.invoke()
                    },
                    onFailure = { err ->
                      feedbackMessage = err.localizedMessage ?: "Failed to apply referral code."
                      isError = true
                    }
                  )
                }
              }
            },
            enabled = inputCode.isNotBlank() && !isSubmitting,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("claim_referral_btn")
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
              Text("Claim")
            }
          }
        }

        // Feedback Banner
        AnimatedVisibility(visible = feedbackMessage != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFF10B981).copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("referral_feedback_message")
          ) {
            Text(
              text = feedbackMessage ?: "",
              color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF047857),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }
  }
}

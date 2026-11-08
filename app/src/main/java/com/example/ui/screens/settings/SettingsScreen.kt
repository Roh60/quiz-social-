package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthManager
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsRepository
import com.example.data.local.ThemeMode
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settingsRepo: AppSettingsRepository,
  quizPollRepo: QuizPollRepository,
  onNavigateBack: () -> Unit,
  onSignedOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

  var showTermsDialog by remember { mutableStateOf(false) }
  var showHelpDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showDeletionInfoDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var isDeleting by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Theme Mode
      SectionTitle("Appearance")
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text("App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = settings.themeMode == mode,
                onClick = { scope.launch { settingsRepo.updateThemeMode(mode) } },
                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
              )
            }
          }
        }
      }

      // Language & Preferences
      SectionTitle("Language & Content")
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = "Language", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Preferred Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("English", "Spanish", "French", "German", "Hindi").forEach { lang ->
              FilterChip(
                selected = settings.preferredLanguage == lang,
                onClick = { scope.launch { settingsRepo.updatePreferredLanguage(lang) } },
                label = { Text(lang) }
              )
            }
          }
        }
      }

      // Offline Cache & Sync
      SectionTitle("Offline Storage")
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Sync, contentDescription = "Cache", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Room Cache & Offline Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
              Text("Save feed posts & draft quizzes locally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(
            checked = settings.offlineCacheEnabled,
            onCheckedChange = { scope.launch { settingsRepo.updateOfflineCacheEnabled(it) } }
          )
        }
      }

      // Safety & Guidelines
      SectionTitle("Support & Guidelines")
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column {
          SettingNavRow(
            icon = Icons.Default.HelpOutline,
            title = "Help & FAQs",
            onClick = { showHelpDialog = true }
          )
          HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
          SettingNavRow(
            icon = Icons.Default.Policy,
            title = "Terms & Community Guidelines",
            onClick = { showTermsDialog = true }
          )
          HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
          SettingNavRow(
            icon = Icons.Default.PrivacyTip,
            title = "Privacy Policy & Data Safety",
            onClick = { showPrivacyDialog = true }
          )
          HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
          SettingNavRow(
            icon = Icons.Default.Info,
            title = "Account Deletion Instructions",
            onClick = { showDeletionInfoDialog = true }
          )
        }
      }

      // Account & Session
      SectionTitle("Account")
      OutlinedButton(
        onClick = {
          AuthManager.signOut(
            context = context,
            credentialManager = credentialManager,
            onSignOutComplete = onSignedOut,
            scope = scope
          )
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out of QuizPoll")
      }

      Button(
        onClick = { showDeleteDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_account_button")
      ) {
        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Account")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete Account & Purge Data")
      }
    }
  }

  // Terms and Guidelines Dialog
  if (showTermsDialog) {
    AlertDialog(
      onDismissRequest = { showTermsDialog = false },
      title = { Text("Terms & Guidelines", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text("Community Rules:", fontWeight = FontWeight.Bold)
          Text("1. Quizzes and polls must be educational, safe, and respectful.\n2. No hate speech, harassment, or commercial spam.\n3. Media uploads are strictly forbidden: text and structured educational metadata only.\n4. Users retain ownership of their contributed quizzes and discussion posts.")
          Spacer(modifier = Modifier.height(10.dp))
          Text("Privacy Policy:", fontWeight = FontWeight.Bold)
          Text("We do not sell personal data. Authentication is handled directly via Google Sign-In with Credential Manager.")
        }
      },
      confirmButton = {
        Button(onClick = { showTermsDialog = false }) { Text("Close") }
      }
    )
  }

  // Help Dialog
  if (showHelpDialog) {
    AlertDialog(
      onDismissRequest = { showHelpDialog = false },
      title = { Text("Help & FAQs", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text("How do I play a quiz?", fontWeight = FontWeight.Bold)
          Text("Tap 'Play Quiz' on any quiz card in the feed. Select your answers and submit before the countdown timer expires.")
          Spacer(modifier = Modifier.height(8.dp))
          Text("Can I change my poll vote?", fontWeight = FontWeight.Bold)
          Text("If the poll author enabled 'Allow Vote Changing', you can tap another choice at any time to update your vote.")
        }
      },
      confirmButton = {
        Button(onClick = { showHelpDialog = false }) { Text("Got it") }
      }
    )
  }

  // Privacy Policy Dialog
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy Policy & Data Safety", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text("QuizPoll Community Privacy Summary:", fontWeight = FontWeight.Bold)
          Text("• We collect your authentication profile (email, name, username) solely for account functionality.")
          Text("• Strict Zero-Media Policy: No images or videos are uploaded to cloud storage; all data consists of structured educational text.")
          Text("• Ads are planned through Google AdMob and InMobi mediation. Advertising partners may process device and advertising data.")
          Text("• In-app account deletion is available. Public web deletion and privacy URLs must be published before public release.")
          Spacer(modifier = Modifier.height(10.dp))
          Text("Public privacy policy URL will be added after Firebase Hosting deployment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        Button(onClick = {
          (context as? android.app.Activity)?.let { com.example.ads.AdsManager.showPrivacyOptions(it) }
          showPrivacyDialog = false
        }) { Text("Privacy Choices") }
      },
      dismissButton = {
        OutlinedButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
      }
    )
  }

  // Deletion Instructions Dialog
  if (showDeletionInfoDialog) {
    AlertDialog(
      onDismissRequest = { showDeletionInfoDialog = false },
      title = { Text("Account Deletion Instructions", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text("How to Delete Your Account:", fontWeight = FontWeight.Bold)
          Text("1. In-App Deletion: Tap 'Delete Account & Purge Data' below in this screen. All your profile data, quizzes, polls, comments, bookmarks, and auth records will be permanently erased immediately.")
          Spacer(modifier = Modifier.height(8.dp))
          Text("2. Web Request (Without App): Email rohitpandit55970@gmail.com from your registered email address. A public HTTPS deletion page will be published before production release.")
          Spacer(modifier = Modifier.height(10.dp))
          Text("Public deletion URL is pending Firebase Hosting deployment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        Button(onClick = {
          val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:rohitpandit55970@gmail.com?subject=QuizPoll%20Community%20Account%20Deletion"))
          try { context.startActivity(intent) } catch (_: Exception) {}
        }) { Text("Email Deletion Request") }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeletionInfoDialog = false }) { Text("Close") }
      }
    )
  }

  // Delete Account Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Account Permanently?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
      text = {
        Text("This action is irreversible. Your profile and user records will be deleted from Cloud Firestore and local device cache will be wiped.")
      },
      confirmButton = {
        Button(
          onClick = {
            isDeleting = true
            scope.launch {
              try {
                quizPollRepo.deleteUserAccountWorkflow()
                settingsRepo.clearAllPreferences()
                showDeleteDialog = false
                onSignedOut()
              } catch (e: Exception) {
                android.widget.Toast.makeText(context, e.localizedMessage ?: "Account deletion failed. Please try again.", android.widget.Toast.LENGTH_LONG).show()
              } finally {
                isDeleting = false
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_account_btn")
        ) {
          Text("Permanently Delete")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold
  )
}

@Composable
private fun SettingNavRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.width(12.dp))
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
  }
}

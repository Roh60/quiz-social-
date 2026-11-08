package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthManager
import com.example.data.local.AppSettings
import com.example.data.local.AppSettingsRepository
import com.example.data.local.QuizPollDatabase
import com.example.data.local.ThemeMode
import com.example.data.remote.QuizPollMessagingService
import com.example.data.remote.QuizPollRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.theme.QuizPollTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    QuizPollMessagingService.ensureNotificationChannel(this)
    com.example.ads.AdsManager.initialize(this)

    setContent {
      val context = LocalContext.current
      val settingsRepo = remember { AppSettingsRepository(context.applicationContext) }
      val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

      val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      QuizPollTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          AppRoot(settingsRepo = settingsRepo)
        }
      }
    }
  }
}

@Composable
fun AppRoot(settingsRepo: AppSettingsRepository) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  // Request Notification permission on Android 13+ (API 33+)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val permissionLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
      val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
      if (status != PackageManager.PERMISSION_GRANTED) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Auth state listener
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  // Attempt silent auto-sign-in on startup if not already signed in
  LaunchedEffect(Unit) {
    if (currentUser == null) {
      AuthManager.attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
        onUnauthenticated = {},
        scope = scope
      )
    }
  }

  // Mandatory Composable Auth Gate
  if (currentUser == null) {
    AuthScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser }
    )
  } else {
    // Authenticated user session
    val database = remember { QuizPollDatabase.getDatabase(context.applicationContext) }
    val repository = remember {
      QuizPollRepository(
        context = context.applicationContext,
        postDao = database.postDao(),
        pollDao = database.pollDao(),
        quizDao = database.quizDao(),
        quizHistoryDao = database.quizHistoryDao(),
        bookmarkDao = database.bookmarkDao(),
        quizLeaderboardDao = database.quizLeaderboardDao()
      )
    }

    AppNavigation(
      repository = repository,
      settingsRepo = settingsRepo,
      draftDao = database.draftDao(),
      currentUserId = currentUser!!.uid,
      onSignedOut = {
        currentUser = null
      }
    )
  }
}

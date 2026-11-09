package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.content.ContextWrapper

object AuthManager {

  private const val FALLBACK_WEB_CLIENT_ID = "268383589803-jdbpqei9t3c2tl4e987s3hevc9671fhg.apps.googleusercontent.com"

  private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

  private fun getWebClientId(context: Context): String {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId != 0) {
      val resVal = context.getString(resId)
      if (resVal.isNotBlank()) return resVal
    }
    return FALLBACK_WEB_CLIENT_ID
  }

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
  ) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
      return
    }
    val clientId = getWebClientId(context)

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
      try {
        val activity = context.findActivity()
        val result = if (activity != null) {
          credentialManager.getCredential(activity, request)
        } else {
          credentialManager.getCredential(context, request)
        }
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
  ) {
    val clientId = getWebClientId(context)
    val activity = context.findActivity()
    if (activity == null) {
      onAuthError("Unable to launch Google Sign-In: Activity context not found")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onAuthError("Unexpected credential type")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Sign in failed")
      }
    }
  }

  fun signUpWithEmail(
    email: String,
    password: String,
    displayName: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
  ) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      onAuthError("Please enter a valid email address.")
      return
    }
    if (password.length < 6) {
      onAuthError("Password must be at least 6 characters long.")
      return
    }

    scope.launch {
      try {
        val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val user = result.user
        if (user != null && displayName.isNotBlank()) {
          try {
            val updates = com.google.firebase.auth.userProfileChangeRequest {
              this.displayName = displayName.trim()
            }
            user.updateProfile(updates).await()
          } catch (e: Exception) {
            Log.w("Auth", "Failed to set display name on auth profile", e)
          }
        }
        try {
          user?.sendEmailVerification()?.await()
        } catch (e: Exception) {
          Log.w("Auth", "Could not send verification email immediately", e)
        }
        onAuthSuccess()
      } catch (e: Exception) {
        Log.e("Auth", "Email registration failed", e)
        val msg = when {
          e.message?.contains("email address is already in use", ignoreCase = true) == true ->
            "This email is already registered. Please sign in instead."
          e.message?.contains("badly formatted", ignoreCase = true) == true ->
            "Please enter a valid email address format."
          else -> e.localizedMessage ?: "Registration failed. Please try again."
        }
        onAuthError(msg)
      }
    }
  }

  fun signInWithEmail(
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
  ) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      onAuthError("Please enter a valid email address.")
      return
    }
    if (password.isBlank()) {
      onAuthError("Please enter your password.")
      return
    }

    scope.launch {
      try {
        Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
        onAuthSuccess()
      } catch (e: Exception) {
        Log.e("Auth", "Email sign-in failed", e)
        val msg = when {
          e.message?.contains("no user record", ignoreCase = true) == true ||
          e.message?.contains("user-not-found", ignoreCase = true) == true ->
            "No account found with this email. Please register first."
          e.message?.contains("wrong-password", ignoreCase = true) == true ||
          e.message?.contains("invalid-credential", ignoreCase = true) == true ||
          e.message?.contains("password is invalid", ignoreCase = true) == true ->
            "Invalid email or password. Please try again or use Forgot Password."
          e.message?.contains("user-disabled", ignoreCase = true) == true ->
            "This account has been disabled. Please contact support."
          e.message?.contains("too-many-requests", ignoreCase = true) == true ->
            "Too many failed login attempts. Please wait a few moments and try again."
          else -> e.localizedMessage ?: "Sign in failed. Please check your credentials."
        }
        onAuthError(msg)
      }
    }
  }

  fun sendVerificationEmail(
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
  ) {
    val user = Firebase.auth.currentUser
    if (user == null) {
      onError("No active user session found.")
      return
    }
    scope.launch {
      try {
        user.sendEmailVerification().await()
        onSuccess()
      } catch (e: Exception) {
        Log.e("Auth", "Failed to send verification email", e)
        onError(e.localizedMessage ?: "Failed to send verification email.")
      }
    }
  }

  fun isCurrentUserEmailVerified(): Boolean {
    return Firebase.auth.currentUser?.isEmailVerified ?: false
  }

  fun sendPasswordReset(
    email: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
  ) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      onError("Please enter a valid email address.")
      return
    }

    scope.launch {
      try {
        Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
        onSuccess()
      } catch (e: Exception) {
        Log.e("Auth", "Password reset failed", e)
        onError(e.localizedMessage ?: "Failed to send reset email. Please try again.")
      }
    }
  }

  fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
  ) {
    Firebase.auth.signOut()
    scope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.e("Auth", "Failed to clear credential state", e)
      } finally {
        onSignOutComplete()
      }
    }
  }
}

package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.AuthManager

enum class AuthMode {
  LOGIN,
  REGISTER,
  FORGOT_PASSWORD
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
  var displayName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var showHelpDialog by remember { mutableStateOf(false) }

  val gradient = Brush.verticalGradient(
    colors = listOf(
      MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
      MaterialTheme.colorScheme.surface
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(gradient)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // App Icon / Logo
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(80.dp),
        shadowElevation = 3.dp
      ) {
        Box(contentAlignment = Alignment.Center) {
          Image(
            painter = painterResource(id = R.drawable.app_launcher_icon_1793520010392),
            contentDescription = "QuizPoll Logo",
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "QuizPoll Community",
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
      )

      Text(
        text = when (authMode) {
          AuthMode.LOGIN -> "Welcome back! Sign in to join discussions and quiz challenges."
          AuthMode.REGISTER -> "Create your community profile to craft quizzes, vote, and earn badges."
          AuthMode.FORGOT_PASSWORD -> "Enter your email to receive a password reset link."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
      )

      // Auth Mode Switcher Tabs (Login vs Register)
      if (authMode != AuthMode.FORGOT_PASSWORD) {
        PrimaryTabRow(
          selectedTabIndex = if (authMode == AuthMode.LOGIN) 0 else 1,
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
          Tab(
            selected = authMode == AuthMode.LOGIN,
            onClick = {
              authMode = AuthMode.LOGIN
              errorMessage = null
              successMessage = null
            },
            text = { Text("Sign In", fontWeight = if (authMode == AuthMode.LOGIN) FontWeight.Bold else FontWeight.Medium) },
            modifier = Modifier.testTag("auth_mode_login_tab")
          )
          Tab(
            selected = authMode == AuthMode.REGISTER,
            onClick = {
              authMode = AuthMode.REGISTER
              errorMessage = null
              successMessage = null
            },
            text = { Text("Register", fontWeight = if (authMode == AuthMode.REGISTER) FontWeight.Bold else FontWeight.Medium) },
            modifier = Modifier.testTag("auth_mode_register_tab")
          )
        }
        Spacer(modifier = Modifier.height(18.dp))
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              authMode = AuthMode.LOGIN
              errorMessage = null
              successMessage = null
            },
            modifier = Modifier.testTag("auth_back_to_login_btn")
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Sign In")
          }
          Text(
            text = "Reset Password",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Success Banner
      AnimatedVisibility(visible = successMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("auth_success_message")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = successMessage ?: "",
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Error Banner
      AnimatedVisibility(visible = errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("auth_error_message")
        ) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // Form Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Display Name (Registration only)
          if (authMode == AuthMode.REGISTER) {
            OutlinedTextField(
              value = displayName,
              onValueChange = { displayName = it },
              label = { Text("Display Name") },
              placeholder = { Text("e.g. Alex Traveler") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("auth_display_name_input")
            )
          }

          // Email Field
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            placeholder = { Text("you@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
          )

          // Password Field (Login & Register)
          if (authMode != AuthMode.FORGOT_PASSWORD) {
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(
                  onClick = { passwordVisible = !passwordVisible },
                  modifier = Modifier.testTag("auth_password_visibility_toggle")
                ) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                  )
                }
              },
              singleLine = true,
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
            )
          }

          // Confirm Password (Register only)
          if (authMode == AuthMode.REGISTER) {
            OutlinedTextField(
              value = confirmPassword,
              onValueChange = { confirmPassword = it },
              label = { Text("Confirm Password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
              singleLine = true,
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input")
            )
          }

          // Forgot Password Link (Login only)
          if (authMode == AuthMode.LOGIN) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(
                onClick = {
                  authMode = AuthMode.FORGOT_PASSWORD
                  errorMessage = null
                  successMessage = null
                },
                modifier = Modifier.testTag("auth_forgot_password_btn")
              ) {
                Text(
                  text = "Forgot Password?",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          // Submit Button
          Button(
            onClick = {
              errorMessage = null
              successMessage = null

              when (authMode) {
                AuthMode.LOGIN -> {
                  isLoading = true
                  AuthManager.signInWithEmail(
                    email = email,
                    password = password,
                    onAuthSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onAuthError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    scope = coroutineScope
                  )
                }
                AuthMode.REGISTER -> {
                  if (password != confirmPassword) {
                    errorMessage = "Passwords do not match."
                    return@Button
                  }
                  isLoading = true
                  AuthManager.signUpWithEmail(
                    email = email,
                    password = password,
                    displayName = displayName.ifBlank { email.substringBefore("@") },
                    onAuthSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onAuthError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    scope = coroutineScope
                  )
                }
                AuthMode.FORGOT_PASSWORD -> {
                  isLoading = true
                  AuthManager.sendPasswordReset(
                    email = email,
                    onSuccess = {
                      isLoading = false
                      successMessage = "Reset link sent! Please check your email inbox."
                    },
                    onError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    scope = coroutineScope
                  )
                }
              }
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("auth_submit_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = when (authMode) {
                  AuthMode.LOGIN -> "Sign In"
                  AuthMode.REGISTER -> "Create Account"
                  AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Google Sign-In Option
      if (authMode != AuthMode.FORGOT_PASSWORD) {
        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          Text(
            text = "OR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp)
          )
          HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
          onClick = {
            isLoading = true
            errorMessage = null
            AuthManager.onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { err ->
                isLoading = false
                errorMessage = err
              },
              scope = coroutineScope,
              onAuthCancelled = {
                isLoading = false
              }
            )
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("google_sign_in_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
          )
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Google Sign In",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Continue with Google",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      TextButton(
        onClick = { showHelpDialog = true },
        modifier = Modifier.testTag("auth_help_btn")
      ) {
        Text("Need help with your account?", style = MaterialTheme.typography.bodySmall)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "By continuing, you agree to QuizPoll Community Guidelines and Terms of Service.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        fontSize = 11.sp
      )
    }

    if (showHelpDialog) {
      AlertDialog(
        onDismissRequest = { showHelpDialog = false },
        title = { Text("QuizPoll Account Help") },
        text = {
          Text("You can sign in using your email and password, or choose 'Continue with Google'. If you forgot your password, switch to the Reset Password tab and enter your email address to receive recovery instructions.")
        },
        confirmButton = {
          Button(onClick = { showHelpDialog = false }) {
            Text("Got it")
          }
        }
      )
    }
  }
}

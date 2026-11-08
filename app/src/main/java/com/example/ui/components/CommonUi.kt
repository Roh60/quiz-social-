package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight

@Composable
fun UserAvatar(
  name: String,
  modifier: Modifier = Modifier,
  avatarUrl: String = "",
  size: Int = 40,
  backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
  textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
  val cleanAvatar = avatarUrl.trim()
  Box(
    modifier = modifier
      .size(size.dp)
      .clip(CircleShape)
      .background(backgroundColor),
    contentAlignment = Alignment.Center
  ) {
    if (cleanAvatar.startsWith("emoji:")) {
      Text(
        text = cleanAvatar.removePrefix("emoji:"),
        fontSize = (size * 0.5).sp,
        textAlign = TextAlign.Center
      )
    } else if (cleanAvatar.isNotBlank() && (cleanAvatar.startsWith("http://") || cleanAvatar.startsWith("https://"))) {
      coil.compose.AsyncImage(
        model = cleanAvatar,
        contentDescription = name,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
      Text(
        text = initial,
        color = textColor,
        fontSize = (size * 0.45).sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun CategoryChip(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .testTag("category_chip_$text"),
    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(20.dp),
    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
fun OfflineBanner(
  isOffline: Boolean,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(visible = isOffline, modifier = modifier) {
    Surface(
      color = MaterialTheme.colorScheme.errorContainer,
      contentColor = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CloudOff,
          contentDescription = "Offline Mode",
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Offline Mode: Showing cached posts. Actions will sync when online.",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun EmptyStateView(
  title: String,
  message: String,
  icon: ImageVector = Icons.Default.Info,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
      modifier = Modifier.size(72.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )
    if (actionLabel != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onActionClick,
        modifier = Modifier.testTag("empty_state_action_btn")
      ) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun rememberShimmerBrush(): Brush {
  val isDark = MaterialTheme.colorScheme.surface.let {
    it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f < 0.5f
  }
  val shimmerColors = if (isDark) {
    listOf(
      Color(0xFF2C2C38),
      Color(0xFF454556),
      Color(0xFF2C2C38)
    )
  } else {
    listOf(
      Color(0xFFE2E4EB),
      Color(0xFFF4F5F9),
      Color(0xFFE2E4EB)
    )
  }
  val transition = rememberInfiniteTransition(label = "shimmer_transition")
  val translateAnim = transition.animateFloat(
    initialValue = -300f,
    targetValue = 1300f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_offset"
  )
  return Brush.linearGradient(
    colors = shimmerColors,
    start = Offset(translateAnim.value, translateAnim.value),
    end = Offset(translateAnim.value + 400f, translateAnim.value + 400f)
  )
}

@Composable
fun SkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberShimmerBrush()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .testTag("shimmer_skeleton_loader")
  ) {
    repeat(3) { index ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("skeleton_card_$index"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Author Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(width = 130.dp, height = 15.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(shimmerBrush)
              )
              Spacer(modifier = Modifier.height(6.dp))
              Box(
                modifier = Modifier
                  .size(width = 85.dp, height = 11.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(shimmerBrush)
              )
            }
            // Type badge skeleton
            Box(
              modifier = Modifier
                .size(width = 64.dp, height = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Title placeholder
          Box(
            modifier = Modifier
              .fillMaxWidth(0.92f)
              .height(18.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(shimmerBrush)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Subtitle / content placeholder
          Box(
            modifier = Modifier
              .fillMaxWidth(0.75f)
              .height(13.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Interactive challenge or poll options preview skeleton
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier
                      .size(width = 150.dp, height = 13.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(shimmerBrush)
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Box(
                    modifier = Modifier
                      .size(width = 100.dp, height = 10.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(shimmerBrush)
                  )
                }
                Box(
                  modifier = Modifier
                    .size(width = 80.dp, height = 32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(shimmerBrush)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Action buttons bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              // Like
              Box(
                modifier = Modifier
                  .size(width = 46.dp, height = 20.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(shimmerBrush)
              )
              // Comment
              Box(
                modifier = Modifier
                  .size(width = 46.dp, height = 20.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(shimmerBrush)
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              // Bookmark
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(shimmerBrush)
              )
              // Share
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(shimmerBrush)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReportDialog(
  targetType: String,
  targetId: String,
  onDismiss: () -> Unit,
  onSubmitReport: (reason: String, details: String) -> Unit
) {
  var selectedReason by remember { mutableStateOf("Inappropriate Content") }
  var details by remember { mutableStateOf("") }
  val reasons = listOf(
    "Inappropriate Content",
    "Spam or Misleading",
    "Harassment or Hate",
    "Incorrect Information",
    "Other"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.Warning, contentDescription = "Report", tint = MaterialTheme.colorScheme.error) },
    title = { Text("Report $targetType") },
    text = {
      Column {
        Text("Help us keep QuizPoll safe and educational.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        reasons.forEach { reason ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = reason }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = if (selectedReason == reason) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
              modifier = Modifier.size(16.dp)
            ) {}
            Spacer(modifier = Modifier.width(10.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text("Additional details (optional)") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmitReport(selectedReason, details) },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("submit_report_button")
      ) {
        Text("Submit Report")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

/**
 * Dedicated shimmer skeleton loader for interactive polls while fetching from Firestore.
 */
@Composable
fun PollSkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberShimmerBrush()
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("poll_skeleton_loader"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Category Pill & Votes Counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(width = 110.dp, height = 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush)
        )
        Box(
          modifier = Modifier
            .size(width = 80.dp, height = 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Question line 1 & 2
      Box(
        modifier = Modifier
          .fillMaxWidth(0.9f)
          .height(20.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(shimmerBrush)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth(0.6f)
          .height(14.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(shimmerBrush)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 3 Option rows
      repeat(3) { optIndex ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("poll_skeleton_option_$optIndex"),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(shimmerBrush)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Box(
                modifier = Modifier
                  .size(width = (120 + optIndex * 30).dp, height = 14.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(shimmerBrush)
              )
            }
            Box(
              modifier = Modifier
                .size(width = 40.dp, height = 14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
            )
          }
        }
        if (optIndex < 2) Spacer(modifier = Modifier.height(8.dp))
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Footer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(width = 140.dp, height = 12.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush)
        )
        Box(
          modifier = Modifier
            .size(width = 70.dp, height = 22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush)
        )
      }
    }
  }
}

/**
 * Shimmer skeleton loader for the Quiz Player screen while questions are fetched from Firestore.
 */
@Composable
fun QuizPlayerSkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberShimmerBrush()
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .testTag("quiz_player_skeleton_loader"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header: Question counter & timer circle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(width = 100.dp, height = 20.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(shimmerBrush)
      )
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(shimmerBrush)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Progress bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(shimmerBrush)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Question card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 4 multiple-choice answer cards
    repeat(4) { idx ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("quiz_player_skeleton_option_$idx"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(shimmerBrush)
          )
          Spacer(modifier = Modifier.width(14.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(0.75f)
              .height(16.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

/**
 * Shimmer skeleton loader for Profile screen during Firestore data retrieval.
 */
@Composable
fun ProfileSkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberShimmerBrush()
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .testTag("profile_skeleton_loader"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Avatar
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(shimmerBrush)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Name & Username
    Box(
      modifier = Modifier
        .size(width = 160.dp, height = 22.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(shimmerBrush)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Box(
      modifier = Modifier
        .size(width = 100.dp, height = 14.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(shimmerBrush)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Stats Bar (Quizzes, Polls, Badges)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        repeat(3) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(width = 40.dp, height = 20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier = Modifier
                .size(width = 55.dp, height = 12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Tab row skeleton
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      repeat(3) {
        Box(
          modifier = Modifier
            .size(width = 80.dp, height = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Tab content items
    repeat(2) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.8f)
              .height(18.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(shimmerBrush)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(0.5f)
              .height(12.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )
        }
      }
    }
  }
}

/**
 * Shimmer skeleton loader for comments threads in Discussion Detail screen.
 */
@Composable
fun CommentsSkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberShimmerBrush()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("comments_skeleton_loader")
  ) {
    // Post Card skeleton
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(shimmerBrush)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Box(
              modifier = Modifier
                .size(width = 120.dp, height = 15.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .size(width = 80.dp, height = 11.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Comments header
    Box(
      modifier = Modifier
        .size(width = 110.dp, height = 18.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(shimmerBrush)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 3 Comments
    repeat(3) { index ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(width = 100.dp, height = 13.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth((0.7f + index * 0.1f).coerceAtMost(0.95f))
              .height(14.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(shimmerBrush)
          )
        }
      }
    }
  }
}

/**
 * Shimmer skeleton loader for community and quiz leaderboards.
 */
@Composable
fun LeaderboardSkeletonLoader(modifier: Modifier = Modifier) {
  val shimmerBrush = rememberShimmerBrush()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("leaderboard_skeleton_loader")
  ) {
    repeat(4) { index ->
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Box(
                modifier = Modifier
                  .size(width = 110.dp, height = 14.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(shimmerBrush)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .size(width = 70.dp, height = 10.dp)
                  .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
              )
            }
          }
          Box(
            modifier = Modifier
              .size(width = 50.dp, height = 22.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(shimmerBrush)
          )
        }
      }
    }
  }
}


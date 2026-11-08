package com.example.ui.screens.notifications

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NotificationItem
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.EmptyStateView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  repository: QuizPollRepository,
  onNavigateBack: () -> Unit,
  onNavigateToPost: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
  var filterUnreadOnly by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    repository.observeNotifications().collectLatest {
      notifications = it
    }
  }

  val displayList = remember(notifications, filterUnreadOnly) {
    if (filterUnreadOnly) notifications.filter { !it.isRead } else notifications
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Activity Notifications", fontWeight = FontWeight.Bold) },
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
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = !filterUnreadOnly,
          onClick = { filterUnreadOnly = false },
          label = { Text("All (${notifications.size})") }
        )
        FilterChip(
          selected = filterUnreadOnly,
          onClick = { filterUnreadOnly = true },
          label = { Text("Unread (${notifications.count { !it.isRead }})") }
        )
      }

      if (displayList.isEmpty()) {
        EmptyStateView(
          title = "No Notifications",
          message = "When other members like your quizzes, answer your polls, or join your discussions, you'll see updates here.",
          icon = Icons.Default.Notifications
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          items(displayList, key = { it.id }) { item ->
            NotificationCard(
              item = item,
              onClick = {
                if (!item.isRead) {
                  scope.launch { repository.markNotificationRead(item.id) }
                }
                if (item.targetId.isNotBlank()) {
                  onNavigateToPost(item.targetId)
                }
              },
              onDelete = {
                scope.launch { repository.deleteNotification(item.id) }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationCard(
  item: NotificationItem,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  val icon: ImageVector = when (item.type) {
    "LIKE" -> Icons.Default.Favorite
    "COMMENT", "REPLY" -> Icons.Default.ChatBubble
    "FOLLOW" -> Icons.Default.PersonAdd
    "QUIZ_ATTEMPT" -> Icons.Default.EmojiEvents
    "POLL_VOTE" -> Icons.Default.BarChart
    else -> Icons.Default.Notifications
  }

  val tint = when (item.type) {
    "LIKE" -> Color(0xFFEF4444)
    "FOLLOW" -> MaterialTheme.colorScheme.primary
    "QUIZ_ATTEMPT" -> Color(0xFFF59E0B)
    "POLL_VOTE" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .testTag("notification_card_${item.id}")
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = item.type, tint = tint, modifier = Modifier.size(20.dp))
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.senderName.ifBlank { "Community Member" },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = item.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(18.dp))
      }
    }
  }
}

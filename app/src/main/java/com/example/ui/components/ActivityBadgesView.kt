package com.example.ui.components

import android.app.Activity
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.ads.AdsManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityStats
import com.example.data.model.BadgeCategory
import com.example.data.model.UserBadge

@Composable
fun ActivityPointsAndBadgesCard(
  stats: ActivityStats,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasWatchedReward by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf<BadgeCategory?>(null) }
  var isBadgesExpanded by remember { mutableStateOf(true) }

  val filteredBadges = remember(stats.badges, selectedCategory) {
    if (selectedCategory == null) stats.badges
    else stats.badges.filter { it.category == selectedCategory }
  }

  val unlockedCount = remember(stats.badges) { stats.badges.count { it.isUnlocked } }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("activity_points_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Level & Total Points
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Points",
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${stats.totalPoints} Points",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("user_activity_points")
            )
          }
          Text(
            text = "Total Activity Score",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Level pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.testTag("user_activity_level")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Lvl ${stats.levelNumber} • ${stats.levelName}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Level Progress Bar
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Level Progress",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${(stats.levelProgress * 100).toInt()}% (${stats.pointsToNextLevel} pts to next tier)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { stats.levelProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Breakdown Cards (Quizzes, Polls, Comments)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ActivityPill(
          title = "Quizzes",
          value = "${stats.quizzesCreated}",
          pts = "+${stats.quizzesCreated * 25} pts",
          modifier = Modifier.weight(1f)
        )
        ActivityPill(
          title = "Polls",
          value = "${stats.pollsCreated}",
          pts = "+${stats.pollsCreated * 20} pts",
          modifier = Modifier.weight(1f)
        )
        ActivityPill(
          title = "Comments",
          value = "${stats.commentsContributed}",
          pts = "+${stats.commentsContributed * 10} pts",
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Rewarded Video Sponsor Benefit
      OutlinedButton(
        onClick = {
          AdsManager.showRewardedAd(
            activity = context as? Activity,
            onUserEarnedReward = { _, _ ->
              hasWatchedReward = true
              Toast.makeText(context, "Ad completed. Reward points are not enabled yet.", Toast.LENGTH_SHORT).show()
            },
            onAdFailed = { msg ->
              Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("watch_rewarded_ad_btn"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.PlayCircle,
          contentDescription = "Watch Video",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (hasWatchedReward) "Video completed" else "Watch optional sponsor video (no points yet)",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Badges Section Header with Expand/Collapse
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isBadgesExpanded = !isBadgesExpanded }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFEAB308),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Community Badges ($unlockedCount / ${stats.badges.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Icon(
          imageVector = if (isBadgesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (isBadgesExpanded) "Collapse Badges" else "Expand Badges",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      AnimatedVisibility(visible = isBadgesExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("badges_section")
        ) {
          // Category filter chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { selectedCategory = null },
              label = { Text("All (${stats.badges.size})") }
            )
            FilterChip(
              selected = selectedCategory == BadgeCategory.CREATION,
              onClick = { selectedCategory = BadgeCategory.CREATION },
              label = { Text("Creations") }
            )
            FilterChip(
              selected = selectedCategory == BadgeCategory.DISCUSSION,
              onClick = { selectedCategory = BadgeCategory.DISCUSSION },
              label = { Text("Discussions") }
            )
            FilterChip(
              selected = selectedCategory == BadgeCategory.MILESTONE,
              onClick = { selectedCategory = BadgeCategory.MILESTONE },
              label = { Text("Milestones") }
            )
          }

          // Badge List
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredBadges.forEach { badge ->
              BadgeItemRow(badge = badge)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActivityPill(
  title: String,
  value: String,
  pts: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = pts,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp
      )
    }
  }
}

@Composable
fun BadgeItemRow(
  badge: UserBadge,
  modifier: Modifier = Modifier
) {
  val icon = getBadgeIcon(badge.iconName)
  val backgroundColor = if (badge.isUnlocked) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
  }

  val borderColor = if (badge.isUnlocked) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
  } else {
    Color.Transparent
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = backgroundColor,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, RoundedCornerShape(14.dp))
      .testTag("badge_item_${badge.id}")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Badge Icon container
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(
            if (badge.isUnlocked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = badge.name,
          tint = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Badge Info
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = badge.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          // Status Badge
          if (badge.isUnlocked) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Unlocked",
                  tint = Color(0xFF059669),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "UNLOCKED",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF059669),
                  fontSize = 10.sp
                )
              }
            }
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${badge.currentProgress}/${badge.targetRequirement}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = badge.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )

        if (!badge.isUnlocked) {
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { badge.progressPercent },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }
      }
    }
  }
}

private fun getBadgeIcon(iconName: String): ImageVector {
  return when (iconName) {
    "Lightbulb" -> Icons.Default.Lightbulb
    "Psychology" -> Icons.Default.Psychology
    "Poll" -> Icons.Default.Poll
    "Chat" -> Icons.Default.Chat
    "Star" -> Icons.Default.Star
    "MilitaryTech" -> Icons.Default.MilitaryTech
    "EmojiEvents" -> Icons.Default.EmojiEvents
    else -> Icons.Default.EmojiEvents
  }
}

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrendingMetric
import com.example.data.model.WeeklyTrendingSummary

@Composable
fun TrendingChartsSection(
  summary: WeeklyTrendingSummary,
  onNavigateToQuizPlayer: (String) -> Unit,
  onNavigateToPostDetail: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Quizzes, 1 = Polls
  val currentList = if (selectedTab == 0) summary.topQuizzes else summary.topPolls
  val maxScore = remember(currentList) {
    currentList.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("trending_charts_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = "Trending",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Top 5 of the Week",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Aggregated weekly community engagement",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.testTag("weekly_engagement_stat")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.BarChart,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${summary.totalWeeklyEngagements} pts",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Toggle Chips: Quizzes vs Polls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Top 5 Quizzes (${summary.topQuizzes.size})")
            }
          },
          modifier = Modifier.testTag("trending_toggle_quizzes")
        )
        FilterChip(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Top 5 Polls (${summary.topPolls.size})")
            }
          },
          modifier = Modifier.testTag("trending_toggle_polls")
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (currentList.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "No trending ${if (selectedTab == 0) "quizzes" else "polls"} this week yet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        // Chart entries
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          currentList.forEachIndexed { index, metric ->
            TrendingChartBarItem(
              rank = index + 1,
              metric = metric,
              maxScore = maxScore,
              onItemClick = {
                if (metric.type == "quiz" && metric.quizId.isNotBlank()) {
                  onNavigateToQuizPlayer(metric.quizId)
                } else {
                  onNavigateToPostDetail(metric.id)
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun TrendingChartBarItem(
  rank: Int,
  metric: TrendingMetric,
  maxScore: Int,
  onItemClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val targetRatio = (metric.score.toFloat() / maxScore.toFloat()).coerceIn(0.12f, 1f)
  val animatedRatio by animateFloatAsState(
    targetValue = targetRatio,
    animationSpec = tween(durationMillis = 600),
    label = "bar_width"
  )

  val rankColor = when (rank) {
    1 -> Color(0xFFEAB308) // Gold
    2 -> Color(0xFF94A3B8) // Silver
    3 -> Color(0xFFD97706) // Bronze
    else -> MaterialTheme.colorScheme.outline
  }

  val gradientBrush = when (metric.type) {
    "quiz" -> Brush.horizontalGradient(
      colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )
    else -> Brush.horizontalGradient(
      colors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
    )
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onItemClick() }
      .testTag("trending_chart_item_$rank")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Row 1: Rank Badge, Title, Category, Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Rank pill
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(rankColor.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = rankColor
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = metric.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${metric.category} • by ${metric.authorName.ifBlank { "Educator" }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Engagement score chip
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface
        ) {
          Text(
            text = "${metric.score} pts",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Row 2: Visual Animated Bar Chart
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(animatedRatio)
            .clip(RoundedCornerShape(5.dp))
            .background(gradientBrush)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Row 3: Metrics subtitle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (metric.type == "quiz") "${metric.playsOrVotes} plays • ${metric.likes} likes"
                 else "${metric.playsOrVotes} votes • ${metric.likes} likes",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )

        Text(
          text = if (metric.type == "quiz") "Play Now →" else "Vote Now →",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        )
      }
    }
  }
}

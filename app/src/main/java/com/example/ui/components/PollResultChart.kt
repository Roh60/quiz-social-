package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Poll

/**
 * Modern visual charting component for real-time community poll results.
 * Supports switching between interactive Pie Chart (Donut), Bar Graph, and D3.js representations.
 */
enum class PollChartType {
  BAR_GRAPH,
  PIE_CHART,
  D3_CHART
}

// Vibrant Material 3 harmonious colors for chart segments
val PollChartPalette = listOf(
  Color(0xFF3B82F6), // Vibrant Blue
  Color(0xFF10B981), // Emerald Green
  Color(0xFFF59E0B), // Warm Amber
  Color(0xFF8B5CF6), // Purple
  Color(0xFFEC4899), // Pink
  Color(0xFF06B6D4)  // Cyan
)

@Composable
fun PollResultChart(
  poll: Poll,
  modifier: Modifier = Modifier,
  initialChartType: PollChartType = PollChartType.BAR_GRAPH
) {
  var selectedChartType by remember { mutableStateOf(initialChartType) }

  val totalVotes = poll.totalVotes.coerceAtLeast(poll.optionVotes.sum())
  val optionPercentages = remember(poll.optionVotes, totalVotes) {
    if (totalVotes > 0) {
      poll.optionVotes.map { votes ->
        ((votes.toDouble() / totalVotes.toDouble()) * 100).toInt()
      }
    } else {
      poll.optionVotes.map { 0 }
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("poll_result_chart_${poll.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header with title and Chart Type Toggle (Bar Graph vs Pie Chart)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Live Results Visualization",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "$totalVotes total responses",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Toggle Buttons
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 1.dp
        ) {
          Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Bar Graph Toggle
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (selectedChartType == PollChartType.BAR_GRAPH) MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
                )
                .clickable { selectedChartType = PollChartType.BAR_GRAPH }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("poll_chart_toggle_bar"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.BarChart,
                  contentDescription = "Bar Graph",
                  tint = if (selectedChartType == PollChartType.BAR_GRAPH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Bar",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selectedChartType == PollChartType.BAR_GRAPH) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedChartType == PollChartType.BAR_GRAPH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Pie Chart Toggle
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (selectedChartType == PollChartType.PIE_CHART) MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
                )
                .clickable { selectedChartType = PollChartType.PIE_CHART }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("poll_chart_toggle_pie"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.PieChart,
                  contentDescription = "Pie Chart",
                  tint = if (selectedChartType == PollChartType.PIE_CHART) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Pie",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selectedChartType == PollChartType.PIE_CHART) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedChartType == PollChartType.PIE_CHART) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // D3.js / Recharts Toggle
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (selectedChartType == PollChartType.D3_CHART) MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
                )
                .clickable { selectedChartType = PollChartType.D3_CHART }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("poll_chart_toggle_d3"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.AutoGraph,
                  contentDescription = "D3.js",
                  tint = if (selectedChartType == PollChartType.D3_CHART) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "D3",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selectedChartType == PollChartType.D3_CHART) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedChartType == PollChartType.D3_CHART) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Chart Body View
      when (selectedChartType) {
        PollChartType.BAR_GRAPH -> {
          PollBarGraphView(
            poll = poll,
            percentages = optionPercentages,
            totalVotes = totalVotes
          )
        }
        PollChartType.PIE_CHART -> {
          PollPieChartView(
            poll = poll,
            percentages = optionPercentages,
            totalVotes = totalVotes
          )
        }
        PollChartType.D3_CHART -> {
          D3PollResultsCard(
            poll = poll,
            userVotedIndices = emptyList(),
            modifier = Modifier.fillMaxWidth().testTag("poll_d3_chart_view_${poll.id}")
          )
        }
      }
    }
  }
}

/**
 * Animated Horizontal Bar Graph representing poll options with percentage labels.
 */
@Composable
fun PollBarGraphView(
  poll: Poll,
  percentages: List<Int>,
  totalVotes: Long,
  modifier: Modifier = Modifier
) {
  val maxVotes = poll.optionVotes.maxOrNull() ?: 1L

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("poll_bar_graph_view"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    poll.options.forEachIndexed { index, optionTitle ->
      val votes = poll.optionVotes.getOrElse(index) { 0L }
      val percentage = percentages.getOrElse(index) { 0 }
      val color = PollChartPalette[index % PollChartPalette.size]

      val targetProgress = if (totalVotes > 0) votes.toFloat() / totalVotes.toFloat() else 0f
      val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "bar_progress_$index"
      )

      val isLeader = votes == maxVotes && votes > 0

      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = optionTitle,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isLeader) FontWeight.Bold else FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = "$percentage% ($votes)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isLeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar container
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(fraction = animatedProgress.coerceIn(0.02f, 1f))
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(color)
              .testTag("poll_bar_item_$index")
          )
        }
      }
    }
  }
}

/**
 * Animated Donut / Pie Chart drawn on Canvas with color legend for poll options.
 */
@Composable
fun PollPieChartView(
  poll: Poll,
  percentages: List<Int>,
  totalVotes: Long,
  modifier: Modifier = Modifier
) {
  val animationProgress = remember { Animatable(0f) }

  LaunchedEffect(poll.optionVotes) {
    animationProgress.snapTo(0f)
    animationProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .testTag("poll_pie_chart_view"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceAround
  ) {
    // Canvas Donut Chart
    Box(
      modifier = Modifier
        .size(130.dp)
        .testTag("poll_pie_canvas_box"),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 26.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        val arcSize = Size(radius * 2, radius * 2)
        val topLeft = Offset(center.x - radius, center.y - radius)

        var startAngle = -90f

        if (totalVotes == 0L) {
          // Draw empty neutral ring
          drawArc(
            color = Color.LightGray.copy(alpha = 0.35f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
          )
        } else {
          poll.optionVotes.forEachIndexed { index, votes ->
            val fraction = (votes.toFloat() / totalVotes.toFloat())
            val sweepAngle = fraction * 360f * animationProgress.value
            val color = PollChartPalette[index % PollChartPalette.size]

            if (sweepAngle > 0.5f) {
              drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
              )
            }
            startAngle += sweepAngle
          }
        }
      }

      // Center text inside Donut
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "$totalVotes",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Votes",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Legend Column
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      poll.options.forEachIndexed { index, optionTitle ->
        val votes = poll.optionVotes.getOrElse(index) { 0L }
        val percentage = percentages.getOrElse(index) { 0 }
        val color = PollChartPalette[index % PollChartPalette.size]

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(color)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = optionTitle,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
          )
        }
      }
    }
  }
}

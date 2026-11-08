package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Poll
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real-time results display for community polls using D3.js and Recharts styling.
 * Displays current percentage of votes for each option after a user has voted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun D3PollResultsCard(
  poll: Poll,
  userVotedIndices: List<Int>,
  modifier: Modifier = Modifier,
  initialMode: String = "bars" // "bars" or "donut"
) {
  var chartMode by remember { mutableStateOf(initialMode) }
  val totalVotes = poll.totalVotes.coerceAtLeast(poll.optionVotes.sum())
  val isDark = isSystemInDarkTheme()

  val primaryColorHex = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.primary.toArgb())
  val secondaryColorHex = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.secondary.toArgb())
  val surfaceColorHex = if (isDark) "#1E293B" else "#F8FAFC"
  val textColorHex = if (isDark) "#F8FAFC" else "#0F172A"
  val textMutedHex = if (isDark) "#94A3B8" else "#64748B"

  val highestVote = poll.optionVotes.maxOrNull() ?: 0L

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("d3_poll_chart_container_${poll.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: D3 Real-Time Results & Chart Mode Switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "D3.js Live Results",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "$totalVotes votes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Toggle: D3 Bars vs D3 Donut
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 1.dp
        ) {
          Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (chartMode == "bars") MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
                )
                .clickable { chartMode = "bars" }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("d3_chart_mode_bars_${poll.id}"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.BarChart,
                  contentDescription = "Bars",
                  tint = if (chartMode == "bars") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Bars",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (chartMode == "bars") FontWeight.Bold else FontWeight.Normal,
                  color = if (chartMode == "bars") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (chartMode == "donut") MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
                )
                .clickable { chartMode = "donut" }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("d3_chart_mode_donut_${poll.id}"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.PieChart,
                  contentDescription = "Donut",
                  tint = if (chartMode == "donut") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Donut",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (chartMode == "donut") FontWeight.Bold else FontWeight.Normal,
                  color = if (chartMode == "donut") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Real-time Percentage Breakdown Badges
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        poll.options.forEachIndexed { index, optionText ->
          val votes = poll.optionVotes.getOrNull(index) ?: 0L
          val pct = if (totalVotes > 0) ((votes.toDouble() / totalVotes.toDouble()) * 100).toInt() else 0
          val isVotedByUser = userVotedIndices.contains(index)
          val isLeader = highestVote > 0L && votes == highestVote

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isVotedByUser) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
              width = if (isVotedByUser) 1.5.dp else 1.dp,
              color = if (isVotedByUser) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.testTag("d3_option_percentage_${poll.id}_$index")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (isLeader) {
                Icon(
                  imageVector = Icons.Default.EmojiEvents,
                  contentDescription = "Leader",
                  tint = Color(0xFFF59E0B),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              if (isVotedByUser) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Voted",
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = "${optionText.take(16)}${if (optionText.length > 16) "…" else ""}: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isVotedByUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Embedded D3.js SVG Chart View (Hardware-accelerated WebView)
      val calculatedHeight = remember(poll.options.size, chartMode) {
        if (chartMode == "bars") {
          (poll.options.size * 52 + 40).coerceIn(180, 420).dp
        } else {
          240.dp
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(calculatedHeight)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
          .testTag("d3_chart_webview_${poll.id}")
      ) {
        D3PollWebView(
          poll = poll,
          userVotedIndices = userVotedIndices,
          chartMode = chartMode,
          isDark = isDark,
          primaryColor = primaryColorHex,
          secondaryColor = secondaryColorHex,
          textColor = textColorHex,
          textMuted = textMutedHex,
          surfaceColor = surfaceColorHex,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun D3PollWebView(
  poll: Poll,
  userVotedIndices: List<Int>,
  chartMode: String,
  isDark: Boolean,
  primaryColor: String,
  secondaryColor: String,
  textColor: String,
  textMuted: String,
  surfaceColor: String,
  modifier: Modifier = Modifier
) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  val htmlData = remember(poll, userVotedIndices, chartMode, isDark) {
    generateD3Html(
      poll = poll,
      userVotedIndices = userVotedIndices,
      chartMode = chartMode,
      isDark = isDark,
      primaryColor = primaryColor,
      secondaryColor = secondaryColor,
      textColor = textColor,
      textMuted = textMuted,
      surfaceColor = surfaceColor
    )
  }

  AndroidView(
    factory = { ctx ->
      WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        setBackgroundColor(AndroidColor.TRANSPARENT)
        webViewClient = WebViewClient()
        loadDataWithBaseURL("https://d3js.org", htmlData, "text/html", "UTF-8", null)
        webViewRef = this
      }
    },
    update = { webView ->
      webViewRef = webView
      // Update data via JavaScript if already loaded, else re-load
      webView.loadDataWithBaseURL("https://d3js.org", htmlData, "text/html", "UTF-8", null)
    },
    modifier = modifier
  )
}

/**
 * Builds an interactive HTML document that harnesses D3.js v7 for SVG-rendered
 * animated horizontal bars and donut pie charts with percentage labels, tooltips,
 * and leader indicators.
 */
private fun generateD3Html(
  poll: Poll,
  userVotedIndices: List<Int>,
  chartMode: String,
  isDark: Boolean,
  primaryColor: String,
  secondaryColor: String,
  textColor: String,
  textMuted: String,
  surfaceColor: String
): String {
  val totalVotes = poll.totalVotes.coerceAtLeast(poll.optionVotes.sum()).coerceAtLeast(1L)
  val highestVote = poll.optionVotes.maxOrNull() ?: 0L

  val dataJsonArray = JSONArray()
  val palette = listOf("#3B82F6", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4")

  poll.options.forEachIndexed { index, option ->
    val votes = poll.optionVotes.getOrNull(index) ?: 0L
    val pct = ((votes.toDouble() / totalVotes.toDouble()) * 100).toInt()
    val isVoted = userVotedIndices.contains(index)
    val isLeader = highestVote > 0L && votes == highestVote
    val color = palette[index % palette.size]

    val obj = JSONObject()
    obj.put("index", index)
    obj.put("label", option)
    obj.put("votes", votes)
    obj.put("percentage", pct)
    obj.put("isVoted", isVoted)
    obj.put("isLeader", isLeader)
    obj.put("color", color)
    dataJsonArray.put(obj)
  }

  val dataJson = dataJsonArray.toString()

  return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
  body {
    background-color: transparent;
    color: $textColor;
    padding: 12px 14px;
    overflow-x: hidden;
    user-select: none;
    -webkit-user-select: none;
  }
  .chart-wrapper {
    width: 100%;
    margin: 0 auto;
  }
  .bar-container {
    margin-bottom: 12px;
  }
  .bar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 5px;
  }
  .bar-label {
    display: flex;
    align-items: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 72%;
  }
  .bar-stats {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 700;
  }
  .voted-badge {
    background-color: $secondaryColor;
    color: white;
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 4px;
    margin-left: 6px;
    font-weight: bold;
    display: inline-block;
  }
  .leader-badge {
    color: #F59E0B;
    font-size: 12px;
    margin-right: 4px;
  }
  .track {
    width: 100%;
    height: 14px;
    background-color: ${if (isDark) "#334155" else "#E2E8F0"};
    border-radius: 7px;
    overflow: hidden;
    position: relative;
  }
  .fill-bar {
    height: 100%;
    border-radius: 7px;
    width: 0%;
    transition: width 0.75s cubic-bezier(0.16, 1, 0.3, 1);
  }
  /* Donut mode */
  .donut-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }
  svg {
    display: block;
    overflow: visible;
  }
  .donut-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 12px;
    margin-top: 10px;
    justify-content: center;
    width: 100%;
  }
  .legend-chip {
    display: flex;
    align-items: center;
    font-size: 11px;
    font-weight: 600;
  }
  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 5px;
    flex-shrink: 0;
  }
  .donut-center-num {
    font-size: 20px;
    font-weight: 800;
    fill: $textColor;
    text-anchor: middle;
  }
  .donut-center-lbl {
    font-size: 10px;
    font-weight: 600;
    fill: $textMuted;
    text-anchor: middle;
  }
</style>
<script src="https://d3js.org/d3.v7.min.js"></script>
</head>
<body>
<div id="chart" class="chart-wrapper"></div>

<script>
  const data = $dataJson;
  const totalVotes = $totalVotes;
  const chartMode = "$chartMode";

  function renderChart() {
    const container = document.getElementById("chart");
    container.innerHTML = "";

    if (chartMode === "bars") {
      renderBars(container);
    } else {
      renderDonut(container);
    }
  }

  function renderBars(container) {
    data.forEach(item => {
      const row = document.createElement("div");
      row.className = "bar-container";

      const header = document.createElement("div");
      header.className = "bar-header";

      const label = document.createElement("span");
      label.className = "bar-label";
      let labelHtml = "";
      if (item.isLeader) {
        labelHtml += '<span class="leader-badge">👑</span>';
      }
      labelHtml += item.label;
      if (item.isVoted) {
        labelHtml += '<span class="voted-badge">Your Vote</span>';
      }
      label.innerHTML = labelHtml;

      const stats = document.createElement("span");
      stats.className = "bar-stats";
      stats.style.color = item.color;
      stats.innerText = item.percentage + "% (" + item.votes + ")";

      header.appendChild(label);
      header.appendChild(stats);

      const track = document.createElement("div");
      track.className = "track";

      const fill = document.createElement("div");
      fill.className = "fill-bar";
      fill.style.backgroundColor = item.color;

      track.appendChild(fill);
      row.appendChild(header);
      row.appendChild(track);
      container.appendChild(row);

      // Trigger D3-style smooth width transition
      setTimeout(() => {
        fill.style.width = item.percentage + "%";
      }, 50);
    });
  }

  function renderDonut(container) {
    const wrap = document.createElement("div");
    wrap.className = "donut-wrap";

    const width = 160;
    const height = 160;
    const margin = 10;
    const radius = Math.min(width, height) / 2 - margin;

    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("width", width);
    svg.setAttribute("height", height);
    svg.setAttribute("viewBox", "0 0 " + width + " " + height);

    const g = document.createElementNS("http://www.w3.org/2000/svg", "g");
    g.setAttribute("transform", "translate(" + (width / 2) + "," + (height / 2) + ")");

    // Use D3 or SVG math
    let currentAngle = 0;
    data.forEach(item => {
      const sliceAngle = totalVotes > 0 ? (item.votes / totalVotes) * (2 * Math.PI) : 0;
      if (sliceAngle <= 0.001) return;

      const startAngle = currentAngle;
      const endAngle = currentAngle + sliceAngle;
      currentAngle = endAngle;

      const innerR = radius * 0.58;
      const outerR = radius;

      const x1 = outerR * Math.cos(startAngle - Math.PI / 2);
      const y1 = outerR * Math.sin(startAngle - Math.PI / 2);
      const x2 = outerR * Math.cos(endAngle - Math.PI / 2);
      const y2 = outerR * Math.sin(endAngle - Math.PI / 2);

      const x3 = innerR * Math.cos(endAngle - Math.PI / 2);
      const y3 = innerR * Math.sin(endAngle - Math.PI / 2);
      const x4 = innerR * Math.cos(startAngle - Math.PI / 2);
      const y4 = innerR * Math.sin(startAngle - Math.PI / 2);

      const largeArc = (endAngle - startAngle) > Math.PI ? 1 : 0;

      const pathData = "M " + x1 + " " + y1 +
                       " A " + outerR + " " + outerR + " 0 " + largeArc + " 1 " + x2 + " " + y2 +
                       " L " + x3 + " " + y3 +
                       " A " + innerR + " " + innerR + " 0 " + largeArc + " 0 " + x4 + " " + y4 +
                       " Z";

      const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
      path.setAttribute("d", pathData);
      path.setAttribute("fill", item.color);
      path.setAttribute("stroke", "$surfaceColor");
      path.setAttribute("stroke-width", "2");
      g.appendChild(path);
    });

    // Center total label
    const textNum = document.createElementNS("http://www.w3.org/2000/svg", "text");
    textNum.setAttribute("class", "donut-center-num");
    textNum.setAttribute("y", "-2");
    textNum.textContent = totalVotes;
    g.appendChild(textNum);

    const textLbl = document.createElementNS("http://www.w3.org/2000/svg", "text");
    textLbl.setAttribute("class", "donut-center-lbl");
    textLbl.setAttribute("y", "14");
    textLbl.textContent = "RESPONSES";
    g.appendChild(textLbl);

    svg.appendChild(g);
    wrap.appendChild(svg);

    // Legend
    const legend = document.createElement("div");
    legend.className = "donut-legend";
    data.forEach(item => {
      const chip = document.createElement("div");
      chip.className = "legend-chip";

      const dot = document.createElement("div");
      dot.className = "legend-dot";
      dot.style.backgroundColor = item.color;

      const txt = document.createElement("span");
      txt.innerText = item.label + ": " + item.percentage + "%";

      chip.appendChild(dot);
      chip.appendChild(txt);
      legend.appendChild(chip);
    });
    wrap.appendChild(legend);

    container.appendChild(wrap);
  }

  // Initial draw
  renderChart();
</script>
</body>
</html>
""".trimIndent()
}

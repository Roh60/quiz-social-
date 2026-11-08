package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Poll
import com.example.data.remote.FirestoreDataRepository
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.launch

/**
 * InteractivePoll component connected directly to FirestoreDataRepository.
 * Observes real-time poll updates and user votes via Firestore snapshot listeners,
 * and commits votes atomically using Firestore transactions.
 */
@Composable
fun InteractivePoll(
  pollId: String,
  repository: FirestoreDataRepository,
  modifier: Modifier = Modifier,
  onShareClick: (() -> Unit)? = null,
  onVoteSuccess: (() -> Unit)? = null
) {
  val pollState by repository.observePoll(pollId).collectAsStateWithLifecycle(initialValue = null)
  val userVoteState by repository.observeUserVote(pollId).collectAsStateWithLifecycle(initialValue = null)

  val scope = rememberCoroutineScope()
  var isSubmittingTransaction by remember { mutableStateOf(false) }
  var transactionErrorMessage by remember { mutableStateOf<String?>(null) }

  val currentPoll = pollState
  if (currentPoll == null) {
    PollSkeletonLoader(
      modifier = modifier
        .fillMaxWidth()
        .testTag("interactive_poll_loading_$pollId")
    )
  } else {
    InteractivePoll(
      poll = currentPoll,
      initialUserVotedIndices = userVoteState?.selectedOptions ?: emptyList(),
      onVoteCast = { selectedIndices ->
        scope.launch {
          isSubmittingTransaction = true
          transactionErrorMessage = null
          try {
            repository.submitPollVote(pollId, selectedIndices)
            onVoteSuccess?.invoke()
          } catch (e: Exception) {
            transactionErrorMessage = e.localizedMessage ?: "Failed to record vote via Firestore transaction."
          } finally {
            isSubmittingTransaction = false
          }
        }
      },
      onShareClick = onShareClick,
      isSubmitting = isSubmittingTransaction,
      errorMessage = transactionErrorMessage,
      modifier = modifier
    )
  }
}

/**
 * High-fidelity, self-contained InteractivePoll component.
 * Renders a poll question with multiple-choice or single-choice options,
 * real-time voting progress indicators, immediate optimistic updates,
 * and optional graphical result charts.
 */
@Composable
fun InteractivePoll(
  poll: Poll,
  initialUserVotedIndices: List<Int> = emptyList(),
  onVoteCast: ((selectedIndices: List<Int>) -> Unit)? = null,
  onShareClick: (() -> Unit)? = null,
  isSubmitting: Boolean = false,
  errorMessage: String? = null,
  modifier: Modifier = Modifier
) {
  // Local state for instant optimistic UI responsiveness
  var userVotedIndices by remember(poll.id, initialUserVotedIndices) {
    mutableStateOf(initialUserVotedIndices)
  }

  val localOptionVotes = remember(poll.id, poll.optionVotes) {
    mutableStateListOf<Long>().apply {
      addAll(poll.optionVotes.ifEmpty { poll.options.map { 0L } })
    }
  }

  var localTotalVotes by remember(poll.id, poll.totalVotes) {
    mutableLongStateOf(poll.totalVotes)
  }

  var showChart by remember { mutableStateOf(false) }

  val hasVoted by remember {
    derivedStateOf { userVotedIndices.isNotEmpty() }
  }

  // Find the top-voted option index (if total votes > 0)
  val highestVoteCount = localOptionVotes.maxOrNull() ?: 0L
  val topOptionIndex = if (localTotalVotes > 0 && highestVoteCount > 0L) {
    localOptionVotes.indexOf(highestVoteCount)
  } else {
    -1
  }

  fun castVote(optIndex: Int) {
    if (isSubmitting) return

    val isAlreadySelected = userVotedIndices.contains(optIndex)

    if (poll.isMultipleChoice) {
      val newSelected = if (isAlreadySelected) {
        userVotedIndices - optIndex
      } else {
        userVotedIndices + optIndex
      }

      if (isAlreadySelected) {
        if (optIndex in localOptionVotes.indices && localOptionVotes[optIndex] > 0) {
          localOptionVotes[optIndex] = localOptionVotes[optIndex] - 1
        }
        if (localTotalVotes > 0) localTotalVotes -= 1
      } else {
        while (localOptionVotes.size <= optIndex) {
          localOptionVotes.add(0L)
        }
        localOptionVotes[optIndex] = localOptionVotes[optIndex] + 1
        localTotalVotes += 1
      }

      userVotedIndices = newSelected
      onVoteCast?.invoke(newSelected)
    } else {
      // Single choice poll
      if (isAlreadySelected && !poll.allowVoteChange) {
        return
      }

      val previousIndex = userVotedIndices.firstOrNull()

      if (previousIndex != null && previousIndex != optIndex) {
        // Vote change
        if (previousIndex in localOptionVotes.indices && localOptionVotes[previousIndex] > 0) {
          localOptionVotes[previousIndex] = localOptionVotes[previousIndex] - 1
        }
      }

      if (!isAlreadySelected) {
        while (localOptionVotes.size <= optIndex) {
          localOptionVotes.add(0L)
        }
        localOptionVotes[optIndex] = localOptionVotes[optIndex] + 1
        if (previousIndex == null) {
          localTotalVotes += 1
        }
      }

      val newSelected = listOf(optIndex)
      userVotedIndices = newSelected
      onVoteCast?.invoke(newSelected)
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("interactive_poll_card_${poll.id}"),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    if (isSubmitting) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .testTag("interactive_poll_submitting_indicator_${poll.id}"),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
      )
    }
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Category, Type (Multi/Single), and Live Total Votes
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Poll,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.secondary
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = poll.category.ifBlank { "Community Poll" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }

          if (poll.isMultipleChoice) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
            ) {
              Text(
                text = "Multi-select",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Total Votes counter badge
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.testTag("interactive_poll_total_votes_${poll.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.HowToVote,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$localTotalVotes ${if (localTotalVotes == 1L) "vote" else "votes"}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          if (onShareClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
              onClick = onShareClick,
              modifier = Modifier
                .size(36.dp)
                .testTag("share_btn_${poll.id}")
                .testTag("poll_share_btn_${poll.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Poll",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Poll Question
      Text(
        text = poll.question,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      if (poll.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = poll.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Inline Error Banner if Firestore transaction failed
      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = "Error",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Options list
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        poll.options.forEachIndexed { optIndex, optionText ->
          val isSelected = userVotedIndices.contains(optIndex)
          val voteCount = localOptionVotes.getOrElse(optIndex) { 0L }
          val effectiveTotal = maxOf(localTotalVotes, 1L)
          val percentage = if (localTotalVotes > 0) {
            ((voteCount.toDouble() / effectiveTotal.toDouble()) * 100).toInt()
          } else {
            0
          }

          val targetProgress = if (localTotalVotes > 0) {
            (voteCount.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }

          val animatedProgress by animateFloatAsState(
            targetValue = if (hasVoted) targetProgress else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "poll_opt_progress_${poll.id}_$optIndex"
          )

          val isTopChoice = hasVoted && optIndex == topOptionIndex && localTotalVotes > 1

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable(
                enabled = !isSubmitting,
                onClick = { castVote(optIndex) }
              )
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                  isSelected -> MaterialTheme.colorScheme.secondary
                  isTopChoice -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                  else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
              )
              .testTag("interactive_poll_option_${poll.id}_$optIndex"),
            color = when {
              isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
              isTopChoice -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
              else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
          ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  if (poll.isMultipleChoice) {
                    Checkbox(
                      checked = isSelected,
                      onCheckedChange = null,
                      modifier = Modifier.size(20.dp),
                      colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                    )
                  } else {
                    RadioButton(
                      selected = isSelected,
                      onClick = null,
                      modifier = Modifier.size(20.dp),
                      colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                // Live Results Percentage and Leading Badge
                if (hasVoted) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("interactive_poll_percentage_${poll.id}_$optIndex")
                  ) {
                    if (isTopChoice) {
                      Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Leading Option",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Your Vote",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                      text = "$percentage% ($voteCount)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = when {
                        isSelected -> MaterialTheme.colorScheme.secondary
                        isTopChoice -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      }
                    )
                  }
                }
              }

              // Animated Progress Bar
              if (hasVoted) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { animatedProgress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("interactive_poll_progress_${poll.id}_$optIndex"),
                  color = when {
                    isSelected -> MaterialTheme.colorScheme.secondary
                    isTopChoice -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                  },
                  trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Transaction syncing status & Chart View toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(12.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Syncing with Firestore...",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium
            )
          } else {
            Icon(
              imageVector = if (hasVoted) Icons.Default.CloudDone else Icons.Default.CloudSync,
              contentDescription = null,
              modifier = Modifier.size(13.dp),
              tint = if (hasVoted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = when {
                hasVoted && poll.allowVoteChange -> "✓ Voted! Tap another to change"
                hasVoted -> "✓ Vote recorded in Firestore"
                poll.isMultipleChoice -> "Select one or more choices"
                else -> "Tap an option to vote"
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (hasVoted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Normal
            )
          }
        }

        if (hasVoted) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (showChart) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { showChart = !showChart }
              .testTag("interactive_poll_chart_toggle_${poll.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (showChart) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (showChart) "Hide Graph" else "View Graph",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (showChart) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Expandable graphical result charts
      AnimatedVisibility(visible = showChart && hasVoted) {
        Column {
          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          Spacer(modifier = Modifier.height(8.dp))
          PollResultChart(
            poll = poll.copy(
              optionVotes = localOptionVotes.toList(),
              totalVotes = localTotalVotes
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("interactive_poll_chart_${poll.id}")
          )
        }
      }
    }
  }
}

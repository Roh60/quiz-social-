package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Poll

/**
 * PollWidget provides the interactive poll experience with immediate state-managed vote casting,
 * live real-time percentage indicators, and total vote count tracking.
 */
@Composable
fun PollWidget(
  poll: Poll,
  userVotedIndices: List<Int>,
  onVoteCast: (selectedIndices: List<Int>) -> Unit,
  modifier: Modifier = Modifier
) {
  InteractivePollComponent(
    poll = poll,
    initialUserVotedIndices = userVotedIndices,
    onVoteCast = onVoteCast,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("poll_widget_${poll.id}")
  )
}

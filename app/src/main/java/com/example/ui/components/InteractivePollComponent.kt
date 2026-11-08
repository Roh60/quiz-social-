package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.Poll

/**
 * Backward-compatible delegator for [InteractivePoll].
 * Renders an interactive poll card allowing users to vote and see real-time percentage results.
 */
@Composable
fun InteractivePollComponent(
  poll: Poll,
  initialUserVotedIndices: List<Int> = emptyList(),
  onVoteCast: ((selectedIndices: List<Int>) -> Unit)? = null,
  onShareClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  InteractivePoll(
    poll = poll,
    initialUserVotedIndices = initialUserVotedIndices,
    onVoteCast = onVoteCast,
    onShareClick = onShareClick,
    modifier = modifier
  )
}

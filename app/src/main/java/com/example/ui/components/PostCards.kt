package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Comment
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SuccessGreen

@Composable
fun PostCard(
  post: Post,
  currentUserId: String,
  isLiked: Boolean,
  isBookmarked: Boolean,
  onLikeClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onCommentClick: () -> Unit,
  onShareClick: () -> Unit,
  onPostClick: () -> Unit,
  onStartQuizClick: ((String) -> Unit)? = null,
  quizData: Quiz? = null,
  pollData: Poll? = null,
  userVotedOptions: List<Int> = emptyList(),
  onVotePoll: ((pollId: String, selected: List<Int>) -> Unit)? = null,
  quizComments: List<Comment> = emptyList(),
  onAddQuizComment: ((postId: String, commentText: String) -> Unit)? = null,
  onReportClick: () -> Unit,
  onBlockUserClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clickable { onPostClick() }
      .testTag("post_card_${post.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Author & Metadata
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        UserAvatar(name = post.authorName, size = 42)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = post.authorName.ifBlank { "Quiz Member" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "@${post.authorUsername.ifBlank { "community" }}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = " • ${post.category}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Post Type Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when (post.postType) {
            "quiz" -> MaterialTheme.colorScheme.primaryContainer
            "poll" -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.tertiaryContainer
          },
          modifier = Modifier.padding(end = 4.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = when (post.postType) {
                "quiz" -> Icons.Default.HelpOutline
                "poll" -> Icons.Default.Poll
                else -> Icons.Default.QuestionAnswer
              },
              contentDescription = post.postType,
              modifier = Modifier.size(14.dp),
              tint = when (post.postType) {
                "quiz" -> MaterialTheme.colorScheme.onPrimaryContainer
                "poll" -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onTertiaryContainer
              }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = post.postType.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = when (post.postType) {
                "quiz" -> MaterialTheme.colorScheme.onPrimaryContainer
                "poll" -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onTertiaryContainer
              }
            )
          }
        }

        // Overflow menu
        Box {
          IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
              text = { Text("Share Link") },
              onClick = { showMenu = false; onShareClick() }
            )
            DropdownMenuItem(
              text = { Text("Report Content") },
              onClick = { showMenu = false; onReportClick() }
            )
            if (post.userId != currentUserId) {
              DropdownMenuItem(
                text = { Text("Block @${post.authorUsername}") },
                onClick = { showMenu = false; onBlockUserClick() }
              )
            }
            if (post.userId == currentUserId && onDeleteClick != null) {
              DropdownMenuItem(
                text = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDeleteClick() }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Title & Content
      Text(
        text = post.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      if (post.content.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = post.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Quiz specific action button
      if (post.postType == "quiz" && post.quizId.isNotBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Interactive Quiz Challenge",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                val quizDetails = buildString {
                  if (quizData != null) {
                    append("${quizData.questionCount} Questions • ${quizData.difficulty}")
                    if (quizData.timeLimitSeconds > 0) append(" • ${quizData.timeLimitSeconds}s Limit")
                  } else {
                    append("Test your knowledge & climb the community leaderboard")
                  }
                }
                Text(
                  text = quizDetails,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(44.dp).testTag("quiz_card_share_btn_${post.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "Share Quiz",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Button(
                onClick = { onStartQuizClick?.invoke(post.quizId) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("start_quiz_btn_${post.id}")
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Quiz", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Quiz")
              }
            }
            if (quizData != null && quizData.attemptCount > 0) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "${quizData.attemptCount} players have completed this challenge",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Dedicated comments & insights section below each quiz card
        Spacer(modifier = Modifier.height(10.dp))
        QuizCardCommentsSection(
          postId = post.id,
          comments = quizComments,
          onAddComment = { text ->
            onAddQuizComment?.invoke(post.id, text)
          }
        )
      }

      // Interactive Poll Component with immediate state management and real-time results
      if (post.postType == "poll" && pollData != null) {
        Spacer(modifier = Modifier.height(12.dp))
        InteractivePollComponent(
          poll = pollData,
          initialUserVotedIndices = userVotedOptions,
          onVoteCast = { selected ->
            onVotePoll?.invoke(pollData.id, selected)
          },
          onShareClick = onShareClick,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_poll_in_feed_${post.id}")
        )

        // Dedicated comments & replies section below each poll card
        Spacer(modifier = Modifier.height(10.dp))
        QuizCardCommentsSection(
          postId = post.id,
          comments = quizComments,
          title = "Poll Discussion & Community Replies",
          emptyText = "No replies yet. Share your thoughts or discuss this poll below!",
          placeholder = "Post a comment or reply to this poll...",
          onAddComment = { text ->
            onAddQuizComment?.invoke(post.id, text)
          }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Footer: Like, Comment, Bookmark, Share
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Likes
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onLikeClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("like_btn_${post.id}")
        ) {
          val heartColor by animateColorAsState(
            if (isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "heart_color"
          )
          Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Like",
            tint = heartColor,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = post.likeCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Comments
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCommentClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("comment_btn_${post.id}")
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Comment",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = post.commentCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Bookmark
        IconButton(
          onClick = onBookmarkClick,
          modifier = Modifier.size(36.dp).testTag("bookmark_btn_${post.id}")
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        // Share Icon Button
        IconButton(
          onClick = onShareClick,
          modifier = Modifier
            .size(48.dp)
            .testTag("share_btn_${post.id}")
            .testTag("share_button_${post.id}")
            .testTag("feed_share_btn_${post.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share ${post.postType.replaceFirstChar { it.uppercase() }}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
fun PollOptionsPreview(
  poll: Poll,
  userVotedIndices: List<Int>,
  onOptionSelected: (Int) -> Unit
) {
  val hasVoted = userVotedIndices.isNotEmpty()
  val total = poll.totalVotes.coerceAtLeast(1L)
  var showD3Chart by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    poll.options.forEachIndexed { index, optionText ->
      val voteCount = poll.optionVotes.getOrNull(index) ?: 0L
      val percentage = if (poll.totalVotes > 0) (voteCount.toFloat() / total * 100).toInt() else 0
      val isSelected = userVotedIndices.contains(index)
      val targetProgress = if (poll.totalVotes > 0) (voteCount.toFloat() / total).coerceIn(0f, 1f) else 0f
      val animatedProgress by animateFloatAsState(
        targetValue = if (hasVoted) targetProgress else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "poll_option_progress_${poll.id}_$index"
      )

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable { onOptionSelected(index) }
          .border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(12.dp)
          )
          .testTag("poll_option_btn_${poll.id}_$index"),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                  modifier = Modifier.size(22.dp)
                )
              } else {
                RadioButton(
                  selected = isSelected,
                  onClick = null,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            if (hasVoted) {
              Text(
                text = "$percentage% ($voteCount)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          if (hasVoted) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { animatedProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "${poll.totalVotes} total votes • ${if (poll.isMultipleChoice) "Multiple Choice" else "Single Choice"}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (hasVoted) {
        Text(
          text = if (poll.allowVoteChange) "Tap to change vote" else "Vote submitted",
          style = MaterialTheme.typography.labelSmall,
          color = if (poll.allowVoteChange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    if (hasVoted) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (showD3Chart) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showD3Chart = !showD3Chart }
            .testTag("poll_toggle_d3_btn_${poll.id}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.AutoGraph,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (showD3Chart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (showD3Chart) "Hide D3 Results" else "View D3 Results",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (showD3Chart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      AnimatedVisibility(visible = showD3Chart) {
        Column {
          Spacer(modifier = Modifier.height(10.dp))
          D3PollResultsCard(
            poll = poll,
            userVotedIndices = userVotedIndices,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("poll_options_d3_chart_${poll.id}")
          )
        }
      }
    }
  }
}

@Composable
fun QuizCardCommentsSection(
  postId: String,
  comments: List<Comment>,
  onAddComment: (String) -> Unit,
  title: String = "Question Discussion & Insights",
  emptyText: String = "No question insights yet. Share your reasoning, tips, or discuss questions below!",
  placeholder: String = "Share question insight or discuss...",
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(true) }
  var commentText by remember { mutableStateOf("") }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth().testTag("card_comments_section_$postId")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Header row with count badge and collapse/expand
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { isExpanded = !isExpanded }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.QuestionAnswer,
            contentDescription = "Discussion",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
          ) {
            Text(
              text = "${comments.size}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Collapse comments" else "Expand comments",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }

      if (isExpanded) {
        Spacer(modifier = Modifier.height(8.dp))

        // Comments list
        if (comments.isEmpty()) {
          Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            comments.takeLast(4).forEach { comment ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
              ) {
                UserAvatar(name = comment.authorName, size = 28)
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = comment.authorName.ifBlank { "Quiz Member" },
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "@${comment.authorUsername.ifBlank { "user" }}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Comment Input Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("quiz_comment_input_$postId"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (commentText.isNotBlank()) {
                onAddComment(commentText.trim())
                commentText = ""
              }
            },
            enabled = commentText.isNotBlank(),
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
              .testTag("quiz_comment_send_btn_$postId")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send Comment",
              tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Universal CardCommentsSection for posting text-based community replies on quiz and poll cards.
 */
@Composable
fun CardCommentsSection(
  postId: String,
  comments: List<Comment>,
  onAddComment: (String) -> Unit,
  title: String = "Community Discussion & Replies",
  emptyText: String = "No replies yet. Be the first to share your perspective!",
  placeholder: String = "Write a reply or comment...",
  modifier: Modifier = Modifier
) {
  QuizCardCommentsSection(
    postId = postId,
    comments = comments,
    onAddComment = onAddComment,
    title = title,
    emptyText = emptyText,
    placeholder = placeholder,
    modifier = modifier
  )
}



package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ads.AdsManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.Comment
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Scrollable feed component using LazyColumn to display user-submitted quizzes and polls
 * with clean, basic card-based UI for each item.
 */
@Composable
fun QuizPollFeed(
  repository: QuizPollRepository,
  currentUserId: String,
  onTakeQuiz: (quizId: String) -> Unit,
  modifier: Modifier = Modifier,
  onPostClick: ((Post) -> Unit)? = null,
  onShareClick: ((Post) -> Unit)? = null
) {
  val scope = rememberCoroutineScope()
  var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  val quizDataMap = remember { mutableStateMapOf<String, Quiz>() }
  val pollDataMap = remember { mutableStateMapOf<String, Poll>() }
  val pollVotesMap = remember { mutableStateMapOf<String, List<Int>>() }
  val likedPostIds = remember { mutableStateMapOf<String, Boolean>() }
  val postCommentsMap = remember { mutableStateMapOf<String, List<Comment>>() }

  LaunchedEffect(Unit) {
    isLoading = true
    repository.observeFeed(tab = "Latest").collectLatest { fetched ->
      // Filter for user-submitted quizzes and polls
      val quizAndPollPosts = fetched.filter { it.postType == "quiz" || it.postType == "poll" }
      posts = quizAndPollPosts
      isLoading = false

      // Observe comments for community discussion replies
      quizAndPollPosts.forEach { post ->
        scope.launch {
          repository.observeComments(post.id).collectLatest { commentList ->
            postCommentsMap[post.id] = commentList
          }
        }
      }

      // Observe companion quiz details
      quizAndPollPosts.filter { it.postType == "quiz" && it.quizId.isNotBlank() }.forEach { post ->
        scope.launch {
          repository.observeQuiz(post.quizId).collectLatest { q ->
            if (q != null) quizDataMap[post.quizId] = q
          }
        }
      }

      // Observe companion poll details & user votes
      quizAndPollPosts.filter { it.postType == "poll" && it.pollId.isNotBlank() }.forEach { post ->
        scope.launch {
          repository.observePoll(post.pollId).collectLatest { p ->
            if (p != null) pollDataMap[post.pollId] = p
          }
        }
        scope.launch {
          repository.observeUserVote(post.pollId).collectLatest { vote ->
            if (vote != null) pollVotesMap[post.pollId] = vote.selectedOptions
          }
        }
      }
    }
  }

  if (isLoading && posts.isEmpty()) {
    SkeletonLoader(modifier = Modifier.testTag("quiz_poll_feed_loading"))
  } else if (posts.isEmpty()) {
    EmptyStateView(
      title = "No Quizzes or Polls Yet",
      message = "Be the first in the community to publish an interactive quiz or poll!",
      modifier = Modifier.testTag("quiz_poll_feed_empty")
    )
  } else {
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .testTag("quiz_poll_feed_list"),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
        val sponsoredAd = AdsManager.nativeAdManager.getNativeAdForPosition(index)
        if (sponsoredAd != null) {
          NativeAdFeedCard(ad = sponsoredAd)
          Spacer(modifier = Modifier.height(10.dp))
        }
        val isQuiz = post.postType == "quiz"
        val quiz = if (isQuiz) quizDataMap[post.quizId] else null
        val poll = if (!isQuiz) pollDataMap[post.pollId] else null
        val userVotes = pollVotesMap[post.pollId] ?: emptyList()
        val isLiked = likedPostIds[post.id] ?: false
        val comments = postCommentsMap[post.id] ?: emptyList()

        if (isQuiz) {
          BasicQuizCard(
            post = post,
            quiz = quiz,
            isLiked = isLiked,
            comments = comments,
            onTakeQuiz = { onTakeQuiz(post.quizId.ifBlank { post.id }) },
            onLikeToggle = {
              val newLiked = !isLiked
              likedPostIds[post.id] = newLiked
              scope.launch {
                try {
                  repository.toggleLike(post.id, isLiked)
                } catch (_: Exception) {}
              }
            },
            onAddComment = { text ->
              scope.launch {
                try {
                  repository.addComment(post.id, text)
                } catch (_: Exception) {}
              }
            },
            onShare = { onShareClick?.invoke(post) },
            onClick = { onPostClick?.invoke(post) }
          )
        } else {
          BasicPollCard(
            post = post,
            poll = poll,
            userVotedIndices = userVotes,
            isLiked = isLiked,
            comments = comments,
            onVoteCast = { selected ->
              pollVotesMap[post.pollId] = selected
              scope.launch {
                try {
                  repository.submitPollVote(post.pollId, selected)
                } catch (_: Exception) {}
              }
            },
            onLikeToggle = {
              val newLiked = !isLiked
              likedPostIds[post.id] = newLiked
              scope.launch {
                try {
                  repository.toggleLike(post.id, isLiked)
                } catch (_: Exception) {}
              }
            },
            onAddComment = { text ->
              scope.launch {
                try {
                  repository.addComment(post.id, text)
                } catch (_: Exception) {}
              }
            },
            onShare = { onShareClick?.invoke(post) },
            onClick = { onPostClick?.invoke(post) }
          )
        }
      }
      item {
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

/**
 * Basic card-based UI for user-submitted quizzes.
 */
@Composable
fun BasicQuizCard(
  post: Post,
  quiz: Quiz?,
  isLiked: Boolean,
  comments: List<Comment> = emptyList(),
  onTakeQuiz: () -> Unit,
  onLikeToggle: () -> Unit,
  onAddComment: ((String) -> Unit)? = null,
  onShare: (() -> Unit)?,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .testTag("quiz_item_card_${post.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Author & Quiz Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          UserAvatar(name = post.authorName.ifBlank { post.authorUsername }, size = 36)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = post.authorName.ifBlank { "Community Educator" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "@${post.authorUsername.ifBlank { "user" }}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = post.category.ifBlank { "Quiz" },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Quiz Title & Description
      Text(
        text = post.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("quiz_item_title_${post.id}")
      )

      if (post.content.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = post.content,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Challenge details banner with 'Take Quiz' button
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "${quiz?.questionCount ?: 5} Questions • ${quiz?.difficulty ?: "Medium"}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Test your skills & earn community points",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = onTakeQuiz,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("take_quiz_item_btn_${post.id}")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Take Quiz")
          }
        }
      }

      // Community Discussion & Replies Section
      Spacer(modifier = Modifier.height(10.dp))
      CardCommentsSection(
        postId = post.id,
        comments = comments,
        title = "Quiz Discussion & Insights",
        emptyText = "No replies yet. Share your reasoning, tips, or discuss this quiz challenge!",
        placeholder = "Post a comment or reply to this quiz...",
        onAddComment = { text -> onAddComment?.invoke(text) }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Like, Comment count, Share
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onLikeToggle() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${post.likeCount + if (isLiked) 1 else 0}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${post.commentCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (onShare != null) {
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
              onClick = onShare,
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Basic card-based UI for user-submitted community polls.
 */
@Composable
fun BasicPollCard(
  post: Post,
  poll: Poll?,
  userVotedIndices: List<Int>,
  isLiked: Boolean,
  comments: List<Comment> = emptyList(),
  onVoteCast: (List<Int>) -> Unit,
  onLikeToggle: () -> Unit,
  onAddComment: ((String) -> Unit)? = null,
  onShare: (() -> Unit)?,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  val hasVoted = userVotedIndices.isNotEmpty()
  val totalVotes = (poll?.totalVotes ?: 0L).coerceAtLeast(1L)
  var showChart by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .testTag("poll_item_card_${post.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Author & Poll Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          UserAvatar(name = post.authorName.ifBlank { post.authorUsername }, size = 36)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = post.authorName.ifBlank { "Community Member" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "@${post.authorUsername.ifBlank { "user" }}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondaryContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = post.category.ifBlank { "Poll" },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Poll Question & Description
      Text(
        text = poll?.question?.ifBlank { post.title } ?: post.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("poll_item_question_${post.id}")
      )

      if (post.content.isNotBlank() && post.content != post.title) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = post.content,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Interactive Poll Component with immediate state management and live percentage calculation
      if (poll != null && poll.options.isNotEmpty()) {
        InteractivePollComponent(
          poll = poll,
          initialUserVotedIndices = userVotedIndices,
          onVoteCast = onVoteCast,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_poll_feed_card_${post.id}")
        )
      }

      // Community Discussion & Replies Section
      Spacer(modifier = Modifier.height(10.dp))
      CardCommentsSection(
        postId = post.id,
        comments = comments,
        title = "Poll Discussion & Community Replies",
        emptyText = "No replies yet. Share your thoughts or discuss this poll below!",
        placeholder = "Post a comment or reply to this poll...",
        onAddComment = { text -> onAddComment?.invoke(text) }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Total votes, Likes, Share
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onLikeToggle() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${post.likeCount + if (isLiked) 1 else 0}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (onShare != null) {
          IconButton(
            onClick = onShare,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

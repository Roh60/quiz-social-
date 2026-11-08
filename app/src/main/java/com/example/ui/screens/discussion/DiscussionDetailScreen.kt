package com.example.ui.screens.discussion

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Comment
import com.example.data.model.CommentReply
import com.example.data.model.Post
import com.example.data.remote.QuizPollRepository
import com.example.ui.components.CommentsSkeletonLoader
import com.example.ui.components.ReportDialog
import com.example.ui.components.UserAvatar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionDetailScreen(
  postId: String,
  repository: QuizPollRepository,
  currentUserId: String,
  onNavigateBack: () -> Unit,
  onNavigateToQuizPlayer: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var post by remember { mutableStateOf<Post?>(null) }
  var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
  var commentText by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }
  var isLiked by remember { mutableStateOf(false) }

  var replyTargetComment by remember { mutableStateOf<Comment?>(null) }
  var replyText by remember { mutableStateOf("") }
  var repliesMap by remember { mutableStateOf<Map<String, List<CommentReply>>>(emptyMap()) }

  var showReportDialog by remember { mutableStateOf(false) }

  LaunchedEffect(postId) {
    post = repository.getPostById(postId)
    repository.observeIsLiked(postId).collectLatest { isLiked = it }
  }

  LaunchedEffect(postId) {
    repository.observeComments(postId).collectLatest { fetchedComments ->
      comments = fetchedComments
      // For each comment, observe replies
      fetchedComments.forEach { comment ->
        scope.launch {
          repository.observeReplies(postId, comment.id).collectLatest { reps ->
            repliesMap = repliesMap + (comment.id to reps)
          }
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(post?.category ?: "Discussion", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { showReportDialog = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
          }
        }
      )
    },
    bottomBar = {
      // Bottom comment input bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          if (replyTargetComment != null) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Replying to @${replyTargetComment?.authorUsername}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { replyTargetComment = null }
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = if (replyTargetComment != null) replyText else commentText,
              onValueChange = {
                if (replyTargetComment != null) replyText = it else commentText = it
              },
              placeholder = {
                Text(if (replyTargetComment != null) "Write a reply..." else "Add your insight to discussion...")
              },
              modifier = Modifier.weight(1f).testTag("comment_input_field"),
              maxLines = 3,
              shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = {
                if (isSending) return@IconButton
                if (replyTargetComment != null) {
                  if (replyText.isNotBlank()) {
                    val target = replyTargetComment!!
                    val textToSend = replyText
                    replyText = ""
                    replyTargetComment = null
                    scope.launch {
                      isSending = true
                      try {
                        repository.addReply(postId, target.id, textToSend)
                      } finally {
                        isSending = false
                      }
                    }
                  }
                } else {
                  if (commentText.isNotBlank()) {
                    val textToSend = commentText
                    commentText = ""
                    scope.launch {
                      isSending = true
                      try {
                        repository.addComment(postId, textToSend)
                      } finally {
                        isSending = false
                      }
                    }
                  }
                }
              },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("send_comment_btn")
            ) {
              if (isSending) {
                androidx.compose.material3.CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    if (post == null) {
      CommentsSkeletonLoader(modifier = Modifier.padding(innerPadding))
    } else {
      val currentPost = post!!
      LazyColumn(
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
      ) {
        // Main Post Card
        item {
          Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              UserAvatar(name = currentPost.authorName, size = 44)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(text = currentPost.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "@${currentPost.authorUsername} • ${currentPost.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = currentPost.title,
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            if (currentPost.content.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = currentPost.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactions row
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  scope.launch {
                    repository.toggleLike(postId, isLiked)
                  }
                }
              ) {
                Icon(
                  imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  contentDescription = "Like",
                  tint = if (isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(text = "${currentPost.likeCount} Likes", style = MaterialTheme.typography.bodySmall)

              Spacer(modifier = Modifier.width(16.dp))
              Text(text = "${comments.size} Comments", style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Community Discussion (${comments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }

        // Comments List
        items(comments, key = { it.id }) { comment ->
          CommentCardItem(
            comment = comment,
            replies = repliesMap[comment.id] ?: emptyList(),
            onReplyClick = { replyTargetComment = comment }
          )
        }

        item {
          Spacer(modifier = Modifier.height(30.dp))
        }
      }
    }
  }

  if (showReportDialog && post != null) {
    ReportDialog(
      targetType = "Discussion",
      targetId = post!!.id,
      onDismiss = { showReportDialog = false },
      onSubmitReport = { reason, details ->
        scope.launch {
          repository.reportContent("POST", post!!.id, reason, details)
          showReportDialog = false
        }
      }
    )
  }
}

@Composable
private fun CommentCardItem(
  comment: Comment,
  replies: List<CommentReply>,
  onReplyClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(name = comment.authorName, size = 32)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = comment.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "@${comment.authorUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Reply",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.clickable { onReplyClick() }
        )
      }

      // Nested replies
      if (replies.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          replies.forEach { rep ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  UserAvatar(name = rep.authorName, size = 24)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = rep.authorName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = rep.content, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
  }
}

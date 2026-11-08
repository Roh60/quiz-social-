package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.QuizCategories

/**
 * Reaction item definition for discussion posts.
 */
data class DiscussionReaction(
  val key: String,
  val emoji: String,
  val label: String,
  val defaultColor: Color
)

val DISCUSSION_REACTIONS = listOf(
  DiscussionReaction("like", "❤️", "Love", Color(0xFFEF4444)),
  DiscussionReaction("lightbulb", "💡", "Insightful", Color(0xFFF59E0B)),
  DiscussionReaction("fire", "🔥", "Fire", Color(0xFFF97316)),
  DiscussionReaction("rocket", "🚀", "Innovative", Color(0xFF8B5CF6)),
  DiscussionReaction("party", "🎉", "Celebrate", Color(0xFF10B981))
)

/**
 * Topic-based discussion feed displaying threads, reactions, and inline comments.
 */
@Composable
fun DiscussionFeed(
  discussions: List<Post>,
  currentUserId: String,
  selectedTopic: String,
  onTopicSelected: (String) -> Unit,
  onLikeClick: (Post) -> Unit,
  likedPostIds: Set<String>,
  onPostComment: (postId: String, commentText: String) -> Unit,
  modifier: Modifier = Modifier,
  topics: List<String> = listOf("All") + QuizCategories.ALL,
  commentsMap: Map<String, List<Comment>> = emptyMap(),
  bookmarkedPostIds: Set<String> = emptySet(),
  onBookmarkClick: ((Post) -> Unit)? = null,
  onShareClick: ((Post) -> Unit)? = null,
  onReportClick: ((Post) -> Unit)? = null,
  onCreateDiscussionClick: (() -> Unit)? = null,
  lazyListState: LazyListState = rememberLazyListState()
) {
  // Reaction states tracked locally for responsive, instantaneous feedback
  val userReactionMap = remember { mutableStateMapOf<String, MutableSet<String>>() }
  val reactionCountsMap = remember { mutableStateMapOf<String, MutableMap<String, Int>>() }

  // Filter posts based on selected topic
  val filteredDiscussions = remember(discussions, selectedTopic) {
    if (selectedTopic == "All" || selectedTopic.isBlank()) {
      discussions
    } else {
      discussions.filter { it.category.equals(selectedTopic, ignoreCase = true) }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("discussion_feed_container")
  ) {
    // 1. Horizontal Topic Selection Filter Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      topics.forEach { topic ->
        val isSelected = topic.equals(selectedTopic, ignoreCase = true)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
          border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onTopicSelected(topic) }
            .testTag("discussion_topic_chip_$topic")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = topic,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 2. Feed Header & Quick Discussion Launcher
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (selectedTopic == "All") "Community Discussions" else "$selectedTopic Discussions",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${filteredDiscussions.size} topic ${if (filteredDiscussions.size == 1) "thread" else "threads"}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (onCreateDiscussionClick != null) {
        Button(
          onClick = onCreateDiscussionClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("new_discussion_action_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Thread", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 3. Discussion Feed List or Empty State
    if (filteredDiscussions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(32.dp)
          .testTag("discussion_empty_state"),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No Discussions in $selectedTopic Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Be the first to share an insight, ask a question, or spark a conversation!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
          )
          if (onCreateDiscussionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onCreateDiscussionClick,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("start_first_discussion_btn")
            ) {
              Text("Start Discussion")
            }
          }
        }
      }
    } else {
      LazyColumn(
        state = lazyListState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("discussion_thread_list")
      ) {
        items(filteredDiscussions, key = { it.id.ifBlank { it.title.hashCode().toString() } }) { post ->
          val isLiked = likedPostIds.contains(post.id)
          val isBookmarked = bookmarkedPostIds.contains(post.id)
          val comments = commentsMap[post.id] ?: emptyList()

          // Local reactions sets for this post
          val userReactions = userReactionMap.getOrPut(post.id) { mutableSetOf() }
          val reactionCounts = reactionCountsMap.getOrPut(post.id) {
            mutableMapOf(
              "like" to post.likeCount.toInt(),
              "lightbulb" to (if (post.id.hashCode() % 3 == 0) 4 else 1),
              "fire" to (if (post.id.hashCode() % 2 == 0) 3 else 0),
              "rocket" to (if (post.id.hashCode() % 5 == 0) 2 else 0),
              "party" to 0
            )
          }

          DiscussionThreadCard(
            post = post,
            currentUserId = currentUserId,
            isLiked = isLiked,
            isBookmarked = isBookmarked,
            userReactions = userReactions,
            reactionCounts = reactionCounts,
            comments = comments,
            onLikeToggle = {
              onLikeClick(post)
              if (userReactions.contains("like")) {
                userReactions.remove("like")
                val c = (reactionCounts["like"] ?: 1) - 1
                reactionCounts["like"] = maxOf(0, c)
              } else {
                userReactions.add("like")
                reactionCounts["like"] = (reactionCounts["like"] ?: 0) + 1
              }
            },
            onReactionToggle = { reactionKey ->
              if (userReactions.contains(reactionKey)) {
                userReactions.remove(reactionKey)
                val c = (reactionCounts[reactionKey] ?: 1) - 1
                reactionCounts[reactionKey] = maxOf(0, c)
              } else {
                userReactions.add(reactionKey)
                reactionCounts[reactionKey] = (reactionCounts[reactionKey] ?: 0) + 1
              }
            },
            onBookmarkToggle = { onBookmarkClick?.invoke(post) },
            onShareClick = { onShareClick?.invoke(post) },
            onReportClick = { onReportClick?.invoke(post) },
            onPostComment = { text -> onPostComment(post.id, text) }
          )
        }
      }
    }
  }
}

/**
 * Individual card for a topic discussion thread, containing reactions and inline comment thread.
 */
@Composable
fun DiscussionThreadCard(
  post: Post,
  currentUserId: String,
  isLiked: Boolean,
  isBookmarked: Boolean,
  userReactions: Set<String>,
  reactionCounts: Map<String, Int>,
  comments: List<Comment>,
  onLikeToggle: () -> Unit,
  onReactionToggle: (reactionKey: String) -> Unit,
  onBookmarkToggle: () -> Unit,
  onShareClick: () -> Unit,
  onReportClick: () -> Unit,
  onPostComment: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var isCommentsExpanded by remember { mutableStateOf(false) }
  var commentInputText by remember { mutableStateOf("") }
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("discussion_thread_card_${post.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // 1. Author row & Topic Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          UserAvatar(name = post.authorName, size = 42)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = post.authorName.ifBlank { "Community Scholar" },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "@${post.authorUsername.ifBlank { "user" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = " • ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
              ) {
                Text(
                  text = post.category.ifBlank { "General" },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        // Overflow menu
        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(36.dp).testTag("discussion_menu_${post.id}")
          ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
              text = { Text("Share Thread") },
              onClick = { showMenu = false; onShareClick() }
            )
            DropdownMenuItem(
              text = { Text("Report Discussion") },
              onClick = { showMenu = false; onReportClick() }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Thread Title & Content
      if (post.title.isNotBlank()) {
        Text(
          text = post.title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("discussion_thread_title_${post.id}")
        )
        Spacer(modifier = Modifier.height(6.dp))
      }

      Text(
        text = post.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 22.sp,
        modifier = Modifier.testTag("discussion_thread_content_${post.id}")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Emoji Reactions Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        DISCUSSION_REACTIONS.forEach { reaction ->
          val isReacted = userReactions.contains(reaction.key) || (reaction.key == "like" && isLiked)
          val count = reactionCounts[reaction.key] ?: 0

          val animatedScale by animateFloatAsState(
            targetValue = if (isReacted) 1.05f else 1.0f,
            animationSpec = spring(),
            label = "reaction_scale"
          )

          val chipBg by animateColorAsState(
            targetValue = if (isReacted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            label = "reaction_bg"
          )

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = chipBg,
            border = if (isReacted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
              .scale(animatedScale)
              .clip(RoundedCornerShape(16.dp))
              .clickable {
                if (reaction.key == "like") {
                  onLikeToggle()
                } else {
                  onReactionToggle(reaction.key)
                }
              }
              .testTag("reaction_btn_${post.id}_${reaction.key}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = reaction.emoji, fontSize = 16.sp)
              if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = count.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isReacted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 4. Action Bar (Comments toggle, Bookmark, Share)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Comments trigger button
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { isCommentsExpanded = !isCommentsExpanded }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("discussion_toggle_comments_${post.id}"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isCommentsExpanded) Icons.AutoMirrored.Filled.Comment else Icons.Default.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = if (isCommentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${comments.size.coerceAtLeast(post.commentCount.toInt())} Comments",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isCommentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBookmarkToggle,
            modifier = Modifier.size(44.dp).testTag("discussion_bookmark_btn_${post.id}")
          ) {
            Icon(
              imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Bookmark",
              tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onShareClick,
            modifier = Modifier.size(44.dp).testTag("discussion_share_btn_${post.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // 5. Expandable Comments Section
      AnimatedVisibility(visible = isCommentsExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              if (comments.isEmpty()) {
                Text(
                  text = "No comments yet. Start the conversation!",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  comments.takeLast(5).forEach { comment ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.Top
                    ) {
                      UserAvatar(name = comment.authorName, size = 30)
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
                            text = comment.authorName.ifBlank { "Scholar" },
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

              // Inline Comment Input Box
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = commentInputText,
                  onValueChange = { commentInputText = it },
                  placeholder = {
                    Text(
                      text = "Reply to discussion...",
                      style = MaterialTheme.typography.bodySmall
                    )
                  },
                  shape = RoundedCornerShape(20.dp),
                  singleLine = true,
                  trailingIcon = {
                    if (commentInputText.isNotBlank()) {
                      IconButton(
                        onClick = { commentInputText = "" },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                      }
                    }
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("discussion_comment_input_${post.id}"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                  )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    if (commentInputText.isNotBlank()) {
                      onPostComment(commentInputText.trim())
                      commentInputText = ""
                    }
                  },
                  enabled = commentInputText.isNotBlank(),
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                      if (commentInputText.isNotBlank()) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("discussion_post_comment_btn_${post.id}")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Post Comment",
                    tint = if (commentInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentUiState(
  val comments: List<Comment> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null,
  val isSubmitting: Boolean = false,
  val currentTargetType: String = "",
  val currentTargetId: String = ""
)

/**
 * CommentViewModel handles creating, fetching (with real-time Firestore observation),
 * and deleting comments for Quizzes, Polls, and community discussions.
 */
class CommentViewModel(
  private val repository: QuizPollRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(CommentUiState())
  val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

  private var observeJob: Job? = null

  /**
   * Subscribes to real-time comments for the given target (Quiz, Poll, or Discussion Post).
   */
  fun loadComments(targetType: String, targetId: String) {
    if (_uiState.value.currentTargetType == targetType && _uiState.value.currentTargetId == targetId && observeJob != null) {
      return
    }

    observeJob?.cancel()
    _uiState.update { it.copy(isLoading = true, error = null, currentTargetType = targetType, currentTargetId = targetId) }

    observeJob = viewModelScope.launch {
      repository.observeTargetComments(targetType, targetId)
        .catch { err ->
          _uiState.update { it.copy(isLoading = false, error = err.localizedMessage ?: "Failed to load comments.") }
        }
        .collectLatest { list ->
          _uiState.update { it.copy(comments = list, isLoading = false, error = null) }
        }
    }
  }

  /**
   * Adds a new text-based comment or community reply to a Quiz or Poll.
   */
  fun addComment(
    targetType: String,
    targetId: String,
    content: String,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
  ) {
    if (content.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, error = null) }
      try {
        repository.addTargetComment(targetType, targetId, content.trim())
        _uiState.update { it.copy(isSubmitting = false) }
        onSuccess?.invoke()
      } catch (e: Exception) {
        val msg = e.localizedMessage ?: "Failed to post comment. Please try again."
        _uiState.update { it.copy(isSubmitting = false, error = msg) }
        onError?.invoke(msg)
      }
    }
  }

  /**
   * Deletes a comment created by the authenticated user.
   */
  fun deleteComment(
    targetType: String,
    targetId: String,
    commentId: String,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
  ) {
    viewModelScope.launch {
      try {
        repository.deleteTargetComment(targetType, targetId, commentId)
        onSuccess?.invoke()
      } catch (e: Exception) {
        val msg = e.localizedMessage ?: "Failed to delete comment."
        _uiState.update { it.copy(error = msg) }
        onError?.invoke(msg)
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }
}

package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BookmarkItem
import com.example.data.model.Poll
import com.example.data.model.Post
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sealed interface representing UI state for the User Profile.
 */
sealed interface ProfileUiState {
  data object Loading : ProfileUiState
  data class Success(val profile: UserProfile) : ProfileUiState
  data class Error(val message: String) : ProfileUiState
  data object Unauthenticated : ProfileUiState
}

/**
 * Aggregate state containing profile data and user-authored collections with null-safety.
 */
data class ProfileScreenState(
  val profileState: ProfileUiState = ProfileUiState.Loading,
  val userQuizzes: List<Quiz> = emptyList(),
  val userPolls: List<Poll> = emptyList(),
  val userPosts: List<Post> = emptyList(),
  val userAttempts: List<QuizAttempt> = emptyList(),
  val userBookmarks: List<BookmarkItem> = emptyList(),
  val userCompletedPolls: List<Poll> = emptyList(),
  val isRefreshing: Boolean = false,
  val updateError: String? = null
)

/**
 * ViewModel managing user profile lifecycle, null-safe data fallback,
 * subcollection observation, and profile updates.
 */
class ProfileViewModel(
  private val repository: QuizPollRepository
) : ViewModel() {

  private val _state = MutableStateFlow(ProfileScreenState())
  val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

  private var activeUserId: String = ""
  private var profileJob: Job? = null
  private var collectionsJob: Job? = null

  /**
   * Initializes profile observation for a user ID.
   * If [userId] is blank, falls back to the currently authenticated Firebase user.
   */
  fun loadProfile(userId: String) {
    val resolvedId = userId.ifBlank { repository.getCurrentUserId() ?: "" }
    if (resolvedId.isBlank()) {
      _state.update { it.copy(profileState = ProfileUiState.Unauthenticated) }
      return
    }

    if (activeUserId == resolvedId && profileJob != null) {
      return
    }

    activeUserId = resolvedId
    _state.update { it.copy(profileState = ProfileUiState.Loading) }

    observeProfile(resolvedId)
    observeSubcollections(resolvedId)
  }

  private fun observeProfile(userId: String) {
    profileJob?.cancel()
    profileJob = viewModelScope.launch {
      repository.observeUserProfile(userId)
        .catch { err ->
          _state.update {
            it.copy(
              profileState = ProfileUiState.Error(
                err.localizedMessage ?: "Failed to load user profile."
              )
            )
          }
        }
        .collectLatest { fetchedProfile ->
          if (fetchedProfile != null) {
            _state.update { it.copy(profileState = ProfileUiState.Success(fetchedProfile)) }
          } else {
            // Profile document missing in Firestore — attempt auto-creation with fallback
            try {
              val created = repository.getOrCreateUserProfile()
              _state.update { it.copy(profileState = ProfileUiState.Success(created)) }
            } catch (e: Exception) {
              // Provide a safe fallback UserProfile so UI doesn't crash
              val fallback = UserProfile(
                userId = userId,
                displayName = "Quiz Member",
                username = "member_${userId.take(5).lowercase()}",
                bio = "Exploring knowledge & sharing quizzes with the community!"
              )
              _state.update { it.copy(profileState = ProfileUiState.Success(fallback)) }
            }
          }
        }
    }
  }

  private fun observeSubcollections(userId: String) {
    collectionsJob?.cancel()
    collectionsJob = viewModelScope.launch {
      // Observe user quizzes
      launch {
        repository.observeUserQuizzes(userId)
          .catch { emit(emptyList()) }
          .collectLatest { quizzes ->
            _state.update { it.copy(userQuizzes = quizzes) }
          }
      }

      // Observe user polls
      launch {
        repository.observeUserPolls(userId)
          .catch { emit(emptyList()) }
          .collectLatest { polls ->
            _state.update { it.copy(userPolls = polls) }
          }
      }

      // Observe user voted polls
      launch {
        repository.observeUserVotedPolls(userId)
          .catch { emit(emptyList()) }
          .collectLatest { voted ->
            _state.update { it.copy(userCompletedPolls = voted) }
          }
      }

      // Observe user bookmarks
      launch {
        repository.observeUserBookmarks(userId)
          .catch { emit(emptyList()) }
          .collectLatest { bookmarks ->
            _state.update { it.copy(userBookmarks = bookmarks) }
          }
      }

      // Observe user quiz attempts
      launch {
        repository.observeUserAttempts(userId)
          .catch { emit(emptyList()) }
          .collectLatest { attempts ->
            _state.update { it.copy(userAttempts = attempts) }
          }
      }

      // Observe user discussions
      launch {
        repository.observeFeed(tab = "Latest")
          .catch { emit(emptyList()) }
          .collectLatest { all ->
            val posts = all.filter { it.userId == userId }
            _state.update { it.copy(userPosts = posts) }
          }
      }
    }
  }

  /**
   * Refreshes the profile state and re-fetches from repository.
   */
  fun refresh() {
    val uid = activeUserId.ifBlank { repository.getCurrentUserId() ?: "" }
    if (uid.isNotBlank()) {
      _state.update { it.copy(isRefreshing = true, updateError = null) }
      viewModelScope.launch {
        try {
          val refreshed = repository.getOrCreateUserProfile()
          _state.update {
            it.copy(
              profileState = ProfileUiState.Success(refreshed),
              isRefreshing = false
            )
          }
        } catch (e: Exception) {
          _state.update { it.copy(isRefreshing = false) }
        }
      }
    }
  }

  /**
   * Updates profile metadata safely.
   */
  fun updateProfile(
    displayName: String,
    bio: String,
    language: String,
    profilePictureUrl: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      try {
        repository.updateUserProfile(displayName, bio, language, profilePictureUrl)
        _state.update { current ->
          val currentSuccess = current.profileState as? ProfileUiState.Success
          if (currentSuccess != null) {
            val updated = currentSuccess.profile.copy(
              displayName = displayName,
              bio = bio,
              language = language,
              profilePictureUrl = profilePictureUrl
            )
            current.copy(profileState = ProfileUiState.Success(updated), updateError = null)
          } else {
            current.copy(updateError = null)
          }
        }
        onSuccess()
      } catch (e: Exception) {
        _state.update { it.copy(updateError = e.localizedMessage ?: "Failed to update profile.") }
      }
    }
  }

  fun clearUpdateError() {
    _state.update { it.copy(updateError = null) }
  }
}

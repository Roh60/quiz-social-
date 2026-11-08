package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.QuizQuestion
import com.example.data.remote.QuizPollRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizPollRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testUserProfileAndDiscussionPostCreation() {
    runBlocking {
      val uid = signInTestUser("tester@example.com")
      val repo = QuizPollRepository(firestore)

      val profile = repo.getOrCreateUserProfile("tester_one", "Tester One")
      assertEquals(uid, profile.userId)
      assertEquals("Tester One", profile.displayName)

      val postId = repo.createDiscussionPost(
        title = "Robolectric Coroutine Test",
        content = "Testing integration against local emulator",
        category = "Programming"
      )
      assertTrue(postId.isNotBlank())

      val fetchedPost = repo.getPostById(postId)
      assertNotNull(fetchedPost)
      assertEquals("Robolectric Coroutine Test", fetchedPost?.title)
    }
  }

  @Test
  fun testQuizCreationAndAttempt() {
    runBlocking {
      signInTestUser("quizmaster@example.com")
      val repo = QuizPollRepository(firestore)

      val questions = listOf(
        QuizQuestion(
          questionText = "Kotlin is developed by?",
          questionType = "SINGLE_MCQ",
          options = listOf("Google", "JetBrains", "Microsoft", "Apple"),
          correctAnswers = listOf(1),
          explanation = "JetBrains created Kotlin."
        )
      )

      val quizId = repo.createQuiz(
        title = "Kotlin History",
        description = "Quick history quiz",
        category = "Programming",
        difficulty = "Easy",
        language = "English",
        timeLimitSeconds = 30,
        isPublic = true,
        questions = questions
      )
      assertTrue(quizId.isNotBlank())

      val quiz = repo.getQuiz(quizId)
      assertNotNull(quiz)
      assertEquals("Kotlin History", quiz?.title)

      val fetchedQuestions = repo.getQuizQuestions(quizId)
      assertEquals(1, fetchedQuestions.size)

      val attempt = repo.submitQuizAttempt(
        quizId = quizId,
        quizTitle = quiz!!.title,
        score = 1,
        totalQuestions = 1,
        timeTakenSeconds = 12,
        userAnswers = mapOf(0 to listOf(1)),
        submissionId = "test_submission_0000000001"
      )
      assertEquals(1, attempt.score)
      assertEquals(100.0, attempt.accuracy, 0.01)
    }
  }

  @Test
  fun testPollCreationAndVote() {
    runBlocking {
      signInTestUser("pollvoter@example.com")
      val repo = QuizPollRepository(firestore)

      val pollId = repo.createPoll(
        question = "Which architecture do you prefer?",
        description = "Vote for modern Android architecture",
        category = "Programming",
        options = listOf("MVVM", "MVI", "Clean Arch"),
        isMultipleChoice = false,
        allowVoteChange = true,
        isAnonymous = false,
        isPublic = true
      )
      assertTrue(pollId.isNotBlank())

      repo.submitPollVote(pollId, listOf(0))
      val updatedPoll = repo.getPoll(pollId)
      assertNotNull(updatedPoll)
      assertEquals(1L, updatedPoll?.totalVotes)
    }
  }

  @Test(expected = IllegalStateException::class)
  fun testUnauthenticatedUserCannotCreatePost() {
    runBlocking {
      auth.signOut()
      val repo = QuizPollRepository(firestore)
      repo.createDiscussionPost("Unauthorized", "Content", "Science")
    }
  }
}
